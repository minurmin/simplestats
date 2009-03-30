package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class DBReader {

    public static Integer[] readTimes(Statement stmt) throws SQLException {
	ArrayList<Integer> times = new ArrayList<Integer>();
	ResultSet rs;

	rs = stmt.executeQuery("SELECT DISTINCT time " +
			       "FROM downloadspercommunity " +
			       "ORDER BY time");
	while (rs.next()) {
	    times.add(rs.getInt("time"));
	}

	Integer[] retVal = new Integer[times.size()];
	retVal = times.toArray(retVal);
	return retVal;
    }

    public static Hashtable<Integer, Community> readCommunities(Statement stmt)
	throws SQLException {

	Hashtable<Integer, Community> communities = new
	    Hashtable<Integer, Community>();
	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM community");
	
	while (rs.next()) {
	    int id = rs.getInt("community_id");
	    String name = rs.getString("name");
	    String handle = rs.getString("handle");
	    communities.put(id, new Community(id, name, handle));
	}
	return communities;
    }

    public static void communitiesToTrees(Hashtable<Integer, Community>
					  communities, Statement stmt)
	throws SQLException {

	ResultSet rs = stmt.executeQuery("SELECT * FROM community2community");

	while (rs.next()) {
	    Community parent, child;

	    parent = communities.get(rs.getInt("parent_comm_id"));
	    child = communities.get(rs.getInt("child_comm_id"));
	    
	    parent.addChild(child);
	}
    }

    public static Hashtable<Integer, Collection>
	readCollections(Statement stmt)
	throws SQLException {

	Hashtable<Integer, Collection> collections = new
	    Hashtable<Integer, Collection>();
	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM collection");
	
	while (rs.next()) {
	    int id = rs.getInt("collection_id");
	    String name = rs.getString("name");
	    String handle = rs.getString("handle");
	    collections.put(id, new Collection(id, name, handle));
	}
	return collections;
    }

    public static Hashtable<Integer, Item> readItems(Statement stmt)
	throws SQLException {

	Hashtable<Integer, Item> items = new Hashtable<Integer, Item>();
	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM item");
	
	while (rs.next()) {
	    int id = rs.getInt("item_id");
	    String name = rs.getString("name");
	    String handle = rs.getString("handle");
	    items.put(id, new Item(id, name, handle));
	}
	return items;
    }

    public static void setRelations(Statement stmt, Hashtable communities,
				    Hashtable collections, Hashtable items)
	throws SQLException {

	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM community2community");
	while (rs.next()) {
	    Community parent, child;

	    parent = (Community)communities.get(rs.getInt("parent_comm_id"));
	    child = (Community)communities.get(rs.getInt("child_comm_id"));
	    
	    parent.addChild(child);
	}

	rs = stmt.executeQuery("SELECT * FROM community2collection");
	while (rs.next()) {
	    Community community;
	    Collection collection;

	    community =	(Community)communities.get(rs.getInt("community_id"));
	    collection =
		(Collection)collections.get(rs.getInt("collection_id"));
	    
	    community.addChild(collection);
	}

	rs = stmt.executeQuery("SELECT * FROM collection2item");
	while (rs.next()) {
	    Collection collection;
	    Item item;

	    collection = 
		(Collection)collections.get(rs.getInt("collection_id"));
	    item =
		(Item)items.get(rs.getInt("item_id"));
	    
	    collection.addChild(item);
	}
    }

    public static void readCommunitiesStats(Statement stmt,
					    Hashtable communities)
	throws SQLException {

	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM downloadspercommunity");
	
	while (rs.next()) {
	    int time = rs.getInt("time");
	    int count = rs.getInt("count");
	    int community_id = rs.getInt("community_id");
	    ((Community)communities.get(community_id)).setCounter(time, count);
	}
    }

    public static void readCollectionsStats(Statement stmt,
					    Hashtable collection)
	throws SQLException {

	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM downloadspercollection");
	
	while (rs.next()) {
	    int time = rs.getInt("time");
	    int count = rs.getInt("count");
	    int collection_id = rs.getInt("collection_id");
	    ((Collection)collection.get(collection_id)).setCounter(time,
								   count);
	}
    }

    public static void readItemsStats(Statement stmt, Hashtable item)
	throws SQLException {

	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM downloadsperitem");
	
	while (rs.next()) {
	    int time = rs.getInt("time");
	    int count = rs.getInt("count");
	    int item_id = rs.getInt("item_id");
	    try {
		((Item)item.get(item_id)).setCounter(time, count);
	    }
	    catch (NullPointerException e) {
		// TODO: Add comment.
		;
	    }
	}
    }

    public static void readItemsStatsForCollection(Statement stmt,
						   Hashtable items,
						   int collection_id)
	throws SQLException {

	ResultSet rs;

	rs = stmt.executeQuery("SELECT * FROM downloadsperitem WHERE item_id IN (SELECT item_id FROM collection2item WHERE collection_id = " + collection_id + ")");

	while (rs.next()) {
	    int time = rs.getInt("time");
	    int count = rs.getInt("count");
	    int item_id = rs.getInt("item_id");
	    ((Item)items.get(item_id)).setCounter(time, count);
	}
    }

}

	
	