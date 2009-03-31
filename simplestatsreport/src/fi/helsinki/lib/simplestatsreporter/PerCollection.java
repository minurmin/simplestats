package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class PerCollection extends SimpleStatsReporter {

    private String printItems(Collection collection,
			      int startTime, int stopTime) {

 	StringWriter out = new StringWriter();

	String collectionName = Misc.fi(collection.getName());

	out.write("<table>");

	out.write("<tr>");

	out.write("<th>");
	out.write(All.printPath(collection.getParent(), startTime, stopTime));
	out.write(" / ");
	out.write(Misc.fi(collection.getName()));
	out.write("</th>");

	out.write(Misc.monthHeaders(startTime, stopTime));
	out.write("</tr>");

	ArrayList<Node> items = collection.getChildren();
	Collections.sort(items, Node.NAME_ORDER);

	for (Node item : items) {
	    out.write("<tr>");
	    out.write("<td><a href=\"" + Config.DSPACE_URL + "/handle/" +
		      item.getHandle() + "\">" +
		      item.getName() + "</a></td>");
	    int countTotal = 0;
	    for(int time : new TimeSpan(startTime, stopTime)) {
		int count = ((Item)item).getCounter(time);

		countTotal += count;

		if (count == 0) {
		    out.write("<td class=\"zero\">" + count + "</td>\n");
		}
		else {
		    out.write("<td>" + count + "</td>\n");
		}
	    }
	    if (countTotal == 0) {
		out.write("<td class=\"total zero\">");
	    }
	    else {
		out.write("<td class=\"total\">");
	    }
	    out.write(countTotal + "</strong></td>");
	    out.write("</tr>\n\n");
	}

	out.write("</table>");

	return out.toString();
    }

    public String htmlContent(Statement stmt, HttpServletRequest request)
	throws SQLException {

	int collectionId = Integer.parseInt(request.getParameter("id"));
	int startTime =
	    Integer.parseInt(request.getParameter("start_time"));
	int stopTime =
	    Integer.parseInt(request.getParameter("stop_time"));

	Hashtable<Integer, Community> communities =
	    DBReader.readCommunities(stmt);
	Hashtable<Integer, Collection> collections =
	    DBReader.readCollections(stmt);
	Hashtable<Integer, Item> items = DBReader.readItems(stmt);

	DBReader.readItemsStatsForCollection(stmt, items, collectionId);

	DBReader.setRelations(stmt, communities, collections, items);


	return printItems(collections.get(collectionId),
			  startTime, stopTime);
    }
}


	
	