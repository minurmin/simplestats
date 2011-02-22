package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.util.ArrayList;

public class PerCommunity extends SimpleStatsReporter {

    private String printItems(ArrayList<Node> allItems, Community community,
			      int startTime, int stopTime) {

 	StringWriter out = new StringWriter();

	String communityName = Misc.fi(community.getName());

	out.write("<table>");

	out.write("<tr>");

	out.write("<th>");
	out.write(All.printPath(community.getParent(), startTime, stopTime));
	out.write(" / ");
	out.write(Misc.fi(community.getName()));
	out.write("</th>");

	out.write(Misc.monthHeaders(startTime, stopTime));
	out.write("</tr>");

        Collections.sort(allItems, Node.NAME_ORDER);

	for (Node item : allItems) {
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

    private void findAllItemsFor(Node node, ArrayList<Node> allItems,
                                 ArrayList<Integer> collectionIDs) {
        ArrayList<Node> children = node.getChildren();
        for (Node thisNode : children) {
            if (thisNode instanceof Community) {
                findAllItemsFor(thisNode, allItems, collectionIDs);
            } else if (thisNode instanceof Collection) {
                Collection thisCollection = (Collection) thisNode;
                allItems.addAll(thisCollection.getChildren());
                collectionIDs.add(thisCollection.getId());
            }
        }
    }

    public String htmlContent(Statement stmt, HttpServletRequest request)
	throws SQLException {

	int communityId = Integer.parseInt(request.getParameter("id"));
	int startTime =
	    Integer.parseInt(request.getParameter("start_time"));
	int stopTime =
	    Integer.parseInt(request.getParameter("stop_time"));

	Hashtable<Integer, Community> communities =
	    DBReader.readCommunities(stmt);
	Hashtable<Integer, Collection> collections =
	    DBReader.readCollections(stmt);
	Hashtable<Integer, Item> items = DBReader.readItems(stmt);

	DBReader.setRelations(stmt, communities, collections, items);

        ArrayList<Integer> collectionIDs = new ArrayList<Integer>();
        ArrayList<Node> allItems = new ArrayList<Node>();
        findAllItemsFor(communities.get(communityId), allItems, collectionIDs);
        for (int thisCollectionID : collectionIDs) {
            DBReader.readItemsStatsForCollection(stmt, items, thisCollectionID,
                                                 startTime, stopTime);
        }

	return printItems(allItems, communities.get(communityId),
			  startTime, stopTime);
    }
}



