package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class FrontPage extends SimpleStatsReporter {

    public String htmlFooter() {
	return("");
    }

    public String extraHeaders() {
	return("<link rel=\"stylesheet\" type=\"text/css\" href=\"mktree/mktree.css\" />\n" +
	       "<script type=\"text/javascript\" src=\"mktree/mktree.js\"></script>\n");
    }

    private void printTree(StringWriter out, Node node, int level) {
	if (node.getParent() == null) {
	    out.write("<li class=\"open\">");
	}
	else {
	    out.write("<li>");
	}
	String checked = node.isRoot() ? " checked=\"checked\" " : "";

	out.write("<input name=\"community_id\" type=\"radio\" value=\"" +
		  node.getId() + "\" id=\"i" + node.getId() + "\" " +
		  checked + "/>");
	out.write("<label for=\"i" + node.getId() + "\">" +
		  Misc.fi(node.getName()) + "</label>\n");
	if (node.hasChildren()) {
	    out.write("<ul>");

	    ArrayList<Node> children = node.getChildren();
	    Collections.sort(children, Node.NAME_ORDER);

	    for (Node child : children) {
		printTree(out, child, level + 1);
	    }
	    out.write("</ul>");
	}
	out.write("</li>");
    }

    private void printSelect(StringWriter out, String name, Integer[] values) {
	out.write("<select id=\"" + name + "\" name=\"" + name + "\" >");
	for(int i : values) {
	    String s = String.format("%d / %d", i%100, i/100);
	    out.write("<option value=\"" + i + "\">" + s + "</option>\n");
	}
	out.write("</select>");
    }

    public String htmlContent(Statement stmt, HttpServletRequest request)
	throws SQLException {

	Hashtable<Integer, Community> communities =
	    DBReader.readCommunities(stmt);

	DBReader.communitiesToTrees(communities, stmt);

	Node root = communities.get(0);
	
 	StringWriter out = new StringWriter();
	out.write("<form action=\"all\"><fieldset><legend>Community</legend>");
	out.write("<ul class=\"mktree\">");
	printTree(out, root, 0);
	out.write("</ul></fieldset>");
	
	Integer[] times = DBReader.readTimes(stmt);

	out.write("<fieldset class=\"timespan\"><legend>Time span</legend>");
	out.write("<span>");
	out.write("<label for=\"start_time\">Start month</label>");
	printSelect(out, "start_time", times);
	out.write("</span>");

	out.write("<span>");
	out.write("<label for=\"stop_time\">End month</label>");
	printSelect(out, "stop_time", times);
	out.write("</span>");

	out.write("</fieldset>");

	out.write("<div class=\"showbutton\"><input type=\"submit\" value=\"Show statistics\" /></div>");
	out.write("</form>");

	return out.toString();
    }
    
}
