package fi.helsinki.lib.simplestatsreporter;

import java.lang.Math.*;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class All extends SimpleStatsReporter {

    static public String printPath(Node node, int startTime, int stopTime) {
	if (node == null) {
	    return "";
	}

	Node n1 = node;
	ArrayList<Node> nodes = new ArrayList<Node>();
	do {
	    nodes.add(0, n1);
	    n1 = n1.getParent();
	} while (n1 != null);

 	StringWriter out = new StringWriter();

	for (Node n : nodes) {
	    out.write("<a href=\"all?community_id=" + n.getId() +
		      "&amp;start_time=" + startTime +
		      "&amp;stop_time=" + stopTime + "\">" +
		      Misc.fi(n.getName()) + "</a>");
	    if (n != nodes.get(nodes.size() - 1)) {
		out.write(" / ");
	    }
	}
	return out.toString();
    }

    private String printTable(Node root, int startTime, int stopTime) {
	StringWriter out = new StringWriter();
	int maxDepth = root.treeDepth();
	
	if (!root.isRoot()) {
	    out.write("<p class=\"path\">Parent hierarchy: " +
		      printPath(root.getParent(), startTime, stopTime) +
		      " / </p>");
	}

	out.write("<table>");

	out.write("<tr><th colspan=\"" + maxDepth + "\"></th>");
	out.write(Misc.monthHeaders(startTime, stopTime));
	out.write("</tr>");

	out.write(root.printTableRow(0, maxDepth, startTime, stopTime));
	out.write("</table>");

	return out.toString();
    }

    private ArrayList<Item> getItems(Node node) {
	ArrayList<Item> retVal = new ArrayList<Item>();
	if (node instanceof Item) {
	    retVal.add((Item)node);
	}
	for (Node child : node.getChildren()) {
	    retVal.addAll(getItems(child));
	}
	return retVal;
    }

    private class IntItemPair {
	public Integer integer;
	public Item item;

	public IntItemPair(int intgr, Item itm) {
	    integer = intgr;
	    item = itm;
	}
    }

    static final Comparator<IntItemPair> REV_NUMBER_ORDER =
	new Comparator<IntItemPair>() {
	public int compare(IntItemPair pair1, IntItemPair pair2) {
	    return pair2.integer.compareTo(pair1.integer);
	}
    };

    private String printTopItems(Node root, int startTime, int stopTime,
				 int nItems) {
	StringWriter out = new StringWriter();
	ArrayList<Item> items = getItems(root);
	ArrayList<IntItemPair>intItemPairs = new ArrayList<IntItemPair>();
	for (Item item : items) {
	    int count = 0;
	    for (int time : new TimeSpan(startTime, stopTime)) {
		count += item.getCounter(time);
	    }

	    intItemPairs.add(new IntItemPair(count, item));
	}
	Collections.sort(intItemPairs, REV_NUMBER_ORDER);
	int n = Math.min(intItemPairs.size(), nItems);

	out.write("<h2>Top " + n + " downloads for this community (" +
		  Misc.fi(root.getName()) + ")</h2>");
 	out.write("<table>");

	out.write("<tr>");
	out.write("<th></th>");
	out.write(Misc.monthHeaders(startTime, stopTime));
	out.write("</tr>");

	for (int i=0; i < n; i++) {
	    out.write("<tr>");
	    Item item = intItemPairs.get(i).item;
	    out.write("<td><a href=\"" + Config.DSPACE_URL + "/handle/" +
		      item.getHandle() + "\">" +
		      item.getName() + "</a></td>");
	    for (int time : new TimeSpan(startTime, stopTime)) {
		out.write("<td>" + item.getCounter(time) + "</td>");
	    }
	    out.write("<td class=\"total\">" +
		      intItemPairs.get(i).integer + "</td>");
	    out.write("</tr>\n");
	}
	out.write("</table>");
	return out.toString();
    }


    public String htmlContent(Statement stmt, HttpServletRequest request)
	throws SQLException {
	
	int startTime =
	    Integer.parseInt(request.getParameter("start_time"));
	int stopTime =
	    Integer.parseInt(request.getParameter("stop_time"));
	int communityId =
	    Integer.parseInt(request.getParameter("community_id"));
	
	Hashtable<Integer, Community> communities =
	    DBReader.readCommunities(stmt);
	Hashtable<Integer, Collection> collections =
	    DBReader.readCollections(stmt);
	Hashtable<Integer, Item> items = DBReader.readItems(stmt);
	
	DBReader.readCommunitiesStats(stmt, communities);
	DBReader.readCollectionsStats(stmt, collections);
	DBReader.readItemsStats(stmt, items);

	DBReader.setRelations(stmt, communities, collections, items);
	
	Node node = communities.get(communityId);
	
	return(printTable(node, startTime, stopTime) +
	       printTopItems(node, startTime, stopTime, 10));
    }
}
