#!/usr/bin/env python

# This is a script that reads DSpace database and log files, and then
# writes download statistics to another database.
#
# Download statistics are monthly downloads and they are counted per
# community, per collection and per item.
#
# Known limitations:
#
#  - We assume that "objects" in DSpace form a tree (or a forest really...),
#    or to be more spesific we assume:
#     - each item has only one collection as a parent
#     - each collection has only one community as a parent
#     - each community has at most one (another) community as a parent
#
#  - Statistics are not 100% accurate, see how count_downloads_from_logs()
#    compares a current log line to previous log line.
#
#  - I have not tested how well this behaves if things are moved or deleted
#    in the DSpace database, see write_stats() below for more info.

import pgdb
import glob
import os
import sys
import re
import types
from sets import ImmutableSet
from datetime import date
from stat import ST_MTIME

from simplestats_config import config # Our configuration information.

class LogLine:
    def __init__(self, line):

        m = re.match('(....)-(..)-(..) \S* (\S*)  (\S*) @ (.*)', line)
        if m:
            year, month, day, level, klass, rest = m.groups()
            try:
                action = rest.split(':')[3]
                ip_addr = rest.split(':')[2].split('=')[1]
            except IndexError:
                raise ValueError

            self.year = int(year)
            self.month = int(month)
            self.day = int(day)
            self.level = level
            self.klass = klass
            self.line = line
            self.rest = rest
            self.action = action
            self.ip_addr = ip_addr
        else:
            raise ValueError

    def is_download_action(self):
        return self.action == 'view_bitstream'

    def get_bitstream_id(self):
        return int(self.rest.split(':')[4].split('=')[1])

    def get_time(self):
        return self.year * 100 + self.month

class LeafNode:
    def __init__(self, my_id, name='', handle=''):
        self.counter = {}
        self.parent = None
        self.my_id = my_id
        self.name = name
        self.handle = handle
        self.n_items = 0
        self.n_bitstreams = 0
        self.n_bytes = 0

    def inc_counter(self, time):
        if self.counter.has_key(time):
            self.counter[time] += 1
        else:
            self.counter[time] = 1

        if self.parent:
            self.parent.inc_counter(time)

    def get_counter(self, time):
        if self.counter.has_key(time):
            return self.counter[time]
        else:
            return 0

    def get_counter_total(self):
        return sum(self.counter.values())
    
class Node(LeafNode):
    def __init__(self, my_id, name, handle=''):
        LeafNode.__init__(self, my_id, name, handle)
        self.children = []

    def add_child(self, child):
        child.parent = self
        self.children.append(child)

    def count_n_items(self):
        n_items = 0
        for child in self.children:
            n_items += child.count_n_items()
        return n_items

    def count_n_bitstreams(self):
        n_bitstreams = 0
        for child in self.children:
            n_bitstreams += child.count_n_bitstreams()
        return n_bitstreams

    def count_n_bytes(self):
        n_bytes = 0
        for child in self.children:
            n_bytes += child.count_n_bytes()
        return n_bytes
    
class Community(Node):
    pass
    
class Collection(Node):
    pass

class Item(Node):
    def count_n_items(self):
        return 1

class Bitstream(LeafNode):
    def count_n_bitstreams(self):
        return 1

    def count_n_bytes(self):
        return self.n_bytes

    # We have this method in order to filter out (at least)
    # thumbnails downloads.
    def is_original_bitstream(self):
        try:
            return self.bundle_name == 'ORIGINAL'
        except AttributeError:
            return True

def create_objects(cursor):
    """Checks the database to see what communities, collections, items, and
    bitstreams we have there and constructs corresponding objects (and put
    them into trees...)

    Returns a 4-tuple of dicts."""

    communities = {}
    collections = {}
    items = {}
    bitstreams = {}

    ITEM = 2
    COLLECTION = 3
    COMMUNITY = 4
    handles = {ITEM: {}, COLLECTION: {}, COMMUNITY: {}}
    cursor.execute("SELECT handle, resource_type_id, resource_id FROM handle")
    for handle, resource_type_id, resource_id in cursor.fetchall():
        handles[resource_type_id][resource_id] = handle

    # Create objects...

    h = handles[COMMUNITY]
    cursor.execute("SELECT community_id, name  from community")
    for community_id, name in cursor.fetchall():
        communities[community_id] = Community(community_id, name,
                                              h[community_id])

    # Let's create a 'virtual' root community because this makes it easier:
    # a) to handle a tree than a forest
    # b) to create statistics for whole DSpace.
    communities[0] = Community(0, 'The entire DSpace')

    h = handles[COLLECTION]
    cursor.execute("SELECT collection_id, name from collection")
    for collection_id, name in cursor.fetchall():
        collections[collection_id] = Collection(collection_id, name,
                                                h[collection_id])

    h = handles[ITEM]
    cursor.execute("SELECT item.item_id, dcvalue.text_value " +
                   "FROM item, dcvalue " +
                   "WHERE item.item_id = dcvalue.item_id " +
                   "AND dcvalue.dc_type_id = 64 " +
                   "AND item.in_archive = TRUE")
    for item_id, name in cursor.fetchall():
        items[item_id] = Item(item_id, name, h[item_id])

    cursor.execute("SELECT bitstream_id FROM bitstream")
    for bitstream_id in [row[0] for row in cursor.fetchall()]:
        bitstreams[bitstream_id] = Bitstream(bitstream_id)


    # ... and put them into hierarchy (i.e. trees).

    cursor.execute("SELECT parent_comm_id, child_comm_id " +
                   "FROM community2community")
    for parent_comm_id, child_comm_id in cursor.fetchall():
        communities[parent_comm_id].add_child(communities[child_comm_id])

    root = communities[0]
    for community in communities.values():
        if community.parent == None and community.my_id != 0:
            root.add_child(community)

    cursor.execute("SELECT community_id, collection_id " +
                   "FROM community2collection")
    for community_id, collection_id in cursor.fetchall():
        communities[community_id].add_child(collections[collection_id])

    cursor.execute("SELECT collection_id, item_id " +
                   "FROM collection2item")
    for collection_id, item_id in cursor.fetchall():
        collection = collections[collection_id]
        try:
            item = items[item_id]
        except KeyError:
            print "(collection2item) Bad item_id: %s" % item_id
            continue
        
        collection.add_child(item)

    # Note that we don't care about bundles so unlike the database
    # each of our Bitsream object has an Item object as a parent:
    cursor.execute("SELECT " +
                   "item2bundle.item_id, " +
                   "bundle2bitstream.bitstream_id, " +
                   "bitstream.size_bytes " +
                   "FROM item2bundle, bundle2bitstream, bitstream " +
                   "WHERE item2bundle.bundle_id = bundle2bitstream.bundle_id "
                   "AND bundle2bitstream.bitstream_id = bitstream.bitstream_id"
                   )
    for item_id, bitstream_id, size_bytes in cursor.fetchall():
        try:
            item = items[item_id]
        except KeyError:
            print "(item2bundle) Bad item_id: %s" % item_id
            continue
            
        bitstream = bitstreams[bitstream_id]
        bitstream.n_bytes = size_bytes
        item.add_child(bitstream)

    for community in communities.values():
        community.n_items = community.count_n_items()
        community.n_bitstreams = community.count_n_bitstreams()
        community.n_bytes = community.count_n_bytes()

    for collection in collections.values():
        collection.n_items = collection.count_n_items()
        collection.n_bitstreams = collection.count_n_bitstreams()
        collection.n_bytes = collection.count_n_bytes()

    for item in items.values():
        item.n_items = item.count_n_items() # Of course, this is always 1.
        item.n_bitstreams = item.count_n_bitstreams()
        item.n_bytes = item.count_n_bytes()

    # Finally, let's set bundle_name attribute for bitstreams.
        
    bundle_types = {}
    cursor.execute("SELECT bundle_id, name FROM bundle")
    for bundle_id, name in cursor.fetchall():
        bundle_types[bundle_id] = name

    cursor.execute("SELECT bundle_id, bitstream_id FROM bundle2bitstream")
    for bundle_id, bitstream_id in cursor.fetchall():
        try:
            bitstream = bitstreams[bitstream_id]
        except KeyError:
            print "(bundle2bitstream) Bad bitstream_id: %s" % bitstream_id
            continue
        try:
            bundle_type = bundle_types[bundle_id]
        except KeyError:
            print "(bundle2bitstream) Bad bundle_id: %s" % bundle_id
            continue
        bitstream.bundle_name = bundle_type

    return (communities, collections, items, bitstreams)
    

def read_ip_exclude_file(ip_exclude_file):

    f = open(ip_exclude_file, 'r')

    ips = filter(lambda line: re.match('^[\d\.]+$', line),
                 [x.strip('. \n') for x in f.readlines()])

    f.close()

    # Let's divide ip addresses to four sets. This makes things faster when
    # we want test if a log line belongs to an excluded ip address (or address
    # range).

    ips_to_exclude = [[], [], [], []] # Three first will contain address ranges
    #                                   and the last one normal ip addresses.
    for ip in ips:
        ips_to_exclude[ip.count('.')].append(ip)

    return [ImmutableSet(ips_to_exclude[x]) for x in range(4)]

def count_downloads_from_logs(ip_exclude_file, log_dir, bitstreams,
                              trust_mtime=False,
                              start_time=None, stop_time=None):

    ips_to_exclude = read_ip_exclude_file(ip_exclude_file)
    pattern = re.compile('ip_addr=(.*?):')

    for filename in glob.glob(os.path.join(log_dir, 'dspace.log*')):

        if trust_mtime:
            # If we trust modification time of the log files (and why not?),
            # then in some situations we are able to save a lot of time by
            # skipping old log files.
            mtime = date.fromtimestamp(os.stat(filename)[ST_MTIME])
            mtime = mtime.year * 100 + mtime.month
            if start_time and mtime < start_time:
                print 'Skipped %s. (Too old for us!)' % filename
                continue
        
        f = open(filename, 'r')

        prev_line = ''
        prev_item = None
        prev_bitstream_id = None
        for line in f.readlines():

            # We skip log lines that are duplicates.
            if line[24:] == prev_line[24:]: # Ignore date and time
                #                             on comparison.
                continue
            else:
                prev_line = line

            try:
                log_line = LogLine(line)
            except ValueError:
                continue

            if start_time and log_line.get_time() < start_time:
                continue # Skip this line.

            if stop_time and log_line.get_time() > stop_time:
                break # Skip the whole file.

            # Skip log lines that have an excluded ip address. (The ip address
            # might be a whole range of addresses expressed as, for example
            # 204.123.9 or even 204.123 or 204 (althought two later cases
            # won't probably happen in practice...))
            m = pattern.search(line)
            exclude_this_line = False
            if m:
                ip_parts = m.group(1).split('.')
                for i in range(4):
                    if '.'.join([str(x) for x in ip_parts[0:i+1]]) in \
                           ips_to_exclude[i]:
                        exclude_this_line = True
                        break
            if exclude_this_line:
                continue # Skip this line.

            if log_line.is_download_action():
                try:
                    bitstream_id = log_line.get_bitstream_id()
                except ValueError:
                    print "Bad bitstream_id"
                    continue

                try:
                    bitstream = bitstreams[bitstream_id]                
                except KeyError:
                    continue
                if not bitstream.is_original_bitstream():
                    continue
                item = bitstream.parent
                if item == prev_item and bitstream_id != prev_bitstream_id:
                    # An item with many bitstreams: If several of those
                    # bitstreams are downloaded at a "same time"(*), count
                    # them as only one download.
                    #
                    # (*) As you can see from the code, "same time" really
                    # means consecutive lines in the log files.
                    pass
                else:
                    bitstreams[bitstream_id].inc_counter(log_line.get_time())

                prev_bitstream_id = bitstream_id
                prev_item = item

            
        f.close()

def generate_time_tuple(start_time, stop_time):
    time_list = []
    year = start_time / 100
    month = start_time % 100

    while year*100 + month <= stop_time:
        time_list.append(year*100 + month)
        month += 1
        if month > 12:
            month = 1
            year += 1

    return tuple(time_list)

def update_download_table(cursor, table_name, id_column_name, nodes, time):
    names = id_column_name + ', time, count'
    cursor.execute("SELECT %s FROM %s" % (names, table_name))
    rows = cursor.fetchall()
    existing_triples = ImmutableSet([tuple(x) for x in rows])
    existing_pairs = ImmutableSet([(x[0], x[1]) for x in rows])

    insert_string = ("INSERT INTO %s (%s) VALUES (%%s, %s, %%s)" %
                     (table_name, names, time))

    update_string = ("UPDATE %s SET count = %%s " +
                     "WHERE %s = %%s AND time = %s") % (table_name,
                                                        id_column_name, time)

    for node in nodes:
        count = node.get_counter(time)
        if (node.my_id, time) not in existing_pairs:
            cursor.execute(insert_string % (node.my_id, count))
        elif (node.my_id, time, count) not in existing_triples:
            cursor.execute(update_string % (count, node.my_id))
        else:
            # The download count for node in the table already exists and is
            # correct -> do nothing.
            pass
        
def update_id_name_handle_etc_table(cursor, table_name, id_column_name, nodes):
    names = id_column_name + ', name, handle, n_items, n_bitstreams, n_bytes'
    cursor.execute("SELECT %s FROM %s" % (names, table_name))
    rows = cursor.fetchall()
    existing_ids = ImmutableSet([x[0] for x in rows])
    existing_tuples = ImmutableSet([tuple(x) for x in rows])

    insert_string = ("INSERT INTO %s (%s) " +
                     "VALUES (%%s, %%s, %%s, %%s, %%s, %%s)") % (table_name,
                                                                 names)

    update_string = ("UPDATE %s SET name = %%s, handle = %%s " +
                     ", n_items = %%s, n_bitstreams = %%s, n_bytes = %%s " + 
                     "WHERE %s = %%s") % (table_name, id_column_name)
                      
    for node in nodes:
        if node.my_id not in existing_ids:
            cursor.execute(insert_string,
                           (node.my_id, node.name, node.handle,
                            node.n_items, node.n_bitstreams, node.n_bytes))
        elif (node.my_id, node.name, node.handle) not in existing_tuples:
            cursor.execute(update_string,
                           (node.name, node.handle,
                            node.n_items, node.n_bitstreams, node.n_bytes,
                            node.my_id))
        else:
            # The node is already in the table and has correct values.
            pass

def update_parent_child_table(cursor, table_name,
                              parent_column_name, child_column_name, children):
    cursor.execute("SELECT %s, %s FROM %s" % (parent_column_name,
                                              child_column_name, table_name))
    rows = cursor.fetchall()
    existing_pairs = ImmutableSet([tuple(row) for row in rows])
    existing_child_ids = ImmutableSet([row[1] for row in rows])

    insert_string = ("INSERT INTO %s (%s, %s) VALUES (%%s, %%s)" %
                     (table_name, parent_column_name, child_column_name))

    update_string = ("UPDATE %s SET %s = %%s WHERE %s = %%s" %
                     (table_name, parent_column_name, child_column_name))

    for child in filter(lambda(x): x.parent != None, children):
        parent_id = child.parent.my_id
        child_id = child.my_id
        if child_id not in existing_child_ids:
            # A new child.
            cursor.execute(insert_string % (parent_id, child_id))
        elif (parent_id, child_id) not in existing_pairs:
            # The child has been moved.
            cursor.execute(update_string % (parent_id, child_id))
        else:
            # The child already has the right parent.
            pass

def update_community2community_table(cursor, community):

    for child in filter(lambda(x): isinstance(x, Community),
                        community.children):
        update_community2community_table(cursor, child)
        
    if community.parent is not None:
        parent_id = community.parent.my_id
        child_id = community.my_id

        cursor.execute("SELECT parent_comm_id FROM " +
                       "community2community WHERE child_comm_id = %s" %
                       community.my_id)
        row = cursor.fetchone()
        if not row:
            cursor.execute("INSERT INTO community2community " +
                           "(parent_comm_id, child_comm_id) VALUES " +
                           "(%s, %s)" % (parent_id, child_id))
        elif row[0] != parent_id:
            cursor.execute("UPDATE community2community SET " +
                           "parent_comm_id = %s " % parent_id +
                           "WHERE child_comm_id = %s" % child_id)
        else:
            pass

def write_stats(cursor, communities, collections, items,
                start_time, stop_time):
    """Write collected statistics to the output database. Note that we try to
    avoid removing old statistics; for example even if a community, a
    collection, or an item is not in the input database (i.e. the real DSpace
    database) anymore, we do *not* remove it from the output database.

    We also try to handle situations where an item is moved to another
    collection, a collection to another community, or a sub community to
    another community. An example: Let's say we have collections A and B,
    and an item C that belongs to A but is moved to B during the month
    number 200902. Download statistics for C are not lost in the move. But
    downloads of the item C also affects download statistics of its
    collection. If we run the script with a start_time 200902 after the move,
    downloads of C increase download statistics of A before 200902 and
    download statistics of B during and after 200902.

    If you want that the downloads of C affect only B, you can run the script
    with smaller start_time as long you still have old log files left.

    WARNING: The text above explains how this is supposed work, I have not
    WARNING: tested how well this handles removes and deletes.
    """

    print "Writing communities..."
    update_id_name_handle_etc_table(cursor, 'community', 'community_id',
                                    communities.values())

    print "Writing collections..."
    update_id_name_handle_etc_table(cursor, 'collection', 'collection_id',
                                    collections.values())

    print "Writing items..."
    update_id_name_handle_etc_table(cursor, 'item', 'item_id', items.values())
        
    # Hierarchy (relationships):

    print "Writing community2community..."
    update_community2community_table(cursor, communities[0])

    print "Writing community2collection..."
    update_parent_child_table(cursor, 'community2collection', 'community_id',
                              'collection_id', collections.values())
    
    print "Writing collection2item..."
    update_parent_child_table(cursor, 'collection2item', 'collection_id',
                              'item_id', items.values())

    # And finally the actual download numbers:
        
    print "Writing actual download statistics..."
    for time in generate_time_tuple(start_time, stop_time):

        print "Month " + str(time) + " ..."

        print "Writing downloadspercommunity..."
        update_download_table(cursor, 'downloadspercommunity', 'community_id',
                              communities.values(), time)

        print "Writing downloadspercollection..."
        update_download_table(cursor, 'downloadspercollection',
                              'collection_id', collections.values(), time)

        print "Writing downloadsperitem..."
        update_download_table(cursor, 'downloadsperitem', 'item_id',
                              items.values(), time)


def connect_to_db(db):
    return pgdb.connect(host = db['host'], database = db['database'],
                        user = db['user'], password = db['password'])

def main(argv=None):

    if argv is None:
        argv = sys.argv

    try:
        start_time = int(argv[1])
        stop_time = int(argv[2])
        # Some sanity checks for input... we don't believe that anyone has
        # logs dated before year 1900.
        if (start_time % 100 not in range(1,13) or start_time / 100 < 1900 or
            stop_time  % 100 not in range(1,13) or stop_time  / 100 < 1900 or
            start_time > stop_time):
            raise ValueError
    except:
        msg = """Usage: %s start_time stop_time

Gather monthly statistics between start_time and stop_time (including those
months). The month numbers must be prefixed by year. For example, to gather
statistics between April 2007 and December 2008:

%s 200704 200812
""" % (argv[0], argv[0])
        print >>sys.stderr, msg
        return 1

    input_db = config['input_db']
    output_db = config['output_db']
    log_dir = config['log_dir']
    ip_exclude_file = config['ip_exclude_file']
    
    print "Reading from the database..."
    db = connect_to_db(input_db)
    cursor = db.cursor()
    communities, collections, items, bitstreams = create_objects(cursor)
    db.close()

    print "Reading log files..."
    count_downloads_from_logs(ip_exclude_file, log_dir, bitstreams,
                              True, start_time, stop_time)

    print "Writing to the database..."
    db = connect_to_db(output_db)
    cursor = db.cursor()
    write_stats(cursor, communities, collections, items, start_time, stop_time)
    db.commit()
    db.close()

if __name__ == '__main__':
    sys.exit(main())

        
