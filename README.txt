Simplestats - collect and display DSpace monthly download statistics


HOW IT WORKS
------------

The idea is to use a separate database where we collect download
statistics from the DSpace log files (and from the DSpace
database). It's not a problem if this collecting process is slow
because we don't have to run it often (for example once a day or once
a month depending how you configure things).

The other part is displaying collected statistics which is implemented
using a Java servlet. A user goes to a web page where she can select a
community and time span for which she wants to see the download
statistics. The servlet then reads from our separate database and
generates a page that displays required statistics. This process has
to be fast (enough).


INSTALLATION
------------

Before installing Simplestats, please make sure that you already have
these installed:
 - DSpace (obviously)
 - Python
 - PyGreSQL
 - Java compiler and Ant
 - PostgreSQL(*)
 - Tomcat or some other servlet container(*)

(*) You can (but don't have to) use the same PostgreSQL and servlet container
    that you are already using for running your DSpace instance.

Installation steps:

1) Creating a database.

Create a database (and maybe a database user too, if you don't already
have one that you can use here).

Edit (if needed) pg_hba.conf so that the user can access the database.

Create tables to the database using database_schema.sql (the file in
this same directory). For example, if your database is called
simplestats and your database user is dspace, the command is something
like this:

% psql -U dspace simplestats < database_schema.sql

2) Configuring and testing python script.

% cd simplestatscollector
% cp simplestats_config.py.tmpl simplestats_config.py

Edit simplestats_config.py, the 'input_db' is your DSpace database
(don't worry the script will *NOT* modify it, it only reads it) and
the 'output_db' is the database you created in the step 1. Don't
forget to configure 'log_dir'.

Now you are ready to run the script...

% ./simplestats.py

... and as you can see that without parameters it just gives some
instructions. Follow those instructions.

Running the script may take few minutes per month.

3) Configuring and compiling Java servlet.

% cd simplestatsreport/src/fi/helsinki/lib/simplestatsreporter/
% cp Config.java.tmpl Config.java

Edit Config.java to have right parameters to access the dabase you
created in the step 1. Also remember to configure DSPACE_URL.

% cd simplestatsreport
% cp build.properties.tmpl build.properties
% cp build.xml.tmpl build.xml

Edit build configuration files to match your environment and then
compile and install:

% ant install

At this point you should be able to point your web browser to
http://yourwebserver/simplestatsreport/front

If you run into trouble with the database connection, you may need to
replace simplestatsreport/web/WEB-INF/lib/postgresql.jar with your
local, more compatible version.

Finally, if everything seems to work fine, you can edit your crontab
file to run simplestatscollector/dailyrunsimplestats.py script every
day.


MISCELLANEOUS
-------------

Note that "Simplestats" is completely separate from the DSpace which means
that if you want to, you can run it on a different computer - just
make sure that the Python script can access DSpace log files and
DSpace database.

Furthermore "Simplestats" has three different parts: the Python script
(the only part that has to access DSpace), the Java servlet and the
PostgreSQL database. There is nothing that forces you run those three
parts on a same computer.
