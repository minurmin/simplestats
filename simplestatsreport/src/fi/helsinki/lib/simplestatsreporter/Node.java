package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.StringWriter;

public class Node {

    static final Comparator<Node> NAME_ORDER = new Comparator<Node>() {
        public int compare(Node n1, Node n2) {
            return Misc.fi(n1.getName()).compareTo(Misc.fi(n2.getName()));
        }
    };

    private int id;
    private String name;
    private String handle;
    private Node parent;
    private ArrayList<Node> children;
    private Hashtable<Integer, Integer> counter;

    public Node(int node_id, String node_name, String node_handle) {
	id = node_id;
	name = node_name;
	handle = node_handle;
	parent = null;
	children = new ArrayList<Node>();
	counter = new Hashtable<Integer, Integer>();
    }

    public int getId() {
	return id;
    }

    public String getName() {
	return name;
    }

    public String getHandle() {
	return handle;
    }

    public void setCounter(int time, int count) {
	counter.put(time, count);
    }

    public int getCounter(int time) {
	try {
	    return ((Integer)counter.get(time)).intValue();
	}
	catch (NullPointerException e) {
	    return 0;
	}
    }

    public Node getParent() {
	return parent;
    }

    public void setParent(Node par) {
	parent = par;
    }

    public boolean isRoot() {
	return (parent == null) ? true : false;
    }

    public void addChild(Node child) {
	child.setParent(this);
	children.add(child);
    }

    public boolean hasChildren() {
	return (children.size() > 0);
    }

    public ArrayList<Node> getChildren() {
	return children;
    }

    private ArrayList<Community> getCommunityChildren() {
	ArrayList<Community> retVal = new ArrayList<Community>();

	for (Node child : children) {
	    if (child instanceof Community) {
		retVal.add((Community)child);
	    }
	}
	return retVal;
    }

    private ArrayList<Collection> getCollectionChildren() {
	ArrayList<Collection> retVal = new ArrayList<Collection>();

	for (Node child : children) {
	    if (child instanceof Collection) {
		retVal.add((Collection)child);
	    }
	}
	return retVal;
    }

    public int treeDepth() {
	int maxDepth = 0;

	for (Object child : children) {
	    int depth = ((Node)child).treeDepth();
	    if (depth > maxDepth) {
		maxDepth = depth;
	    }
	}

	return maxDepth + 1;
    }

    public String getCSSClassStringForTableRow() {
	return " ";
    }

    public String firstTd(int level, int maxDepth,
			  int startTime, int stopTime) {
	return "<td" + getCSSClassStringForTableRow() + " colspan=\"" +
	    (maxDepth-level) + "\">" + Misc.fi(getName()) + "</td>";
    }

    public String printTableRow(int level, int maxDepth,
				int startTime, int stopTime) {
	StringWriter out = new StringWriter();
	out.write("<tr>");
	
	for(int i=0; i < level; i++) {
	    out.write("<td class=\"indent\">&nbsp;</td>");
	}
	out.write(firstTd(level, maxDepth, startTime, stopTime));
	int countTotal = 0;

	for(int time : new TimeSpan(startTime,stopTime)) {
	    int count = getCounter(time);

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
	out.write(countTotal + "</td>");

	out.write("</tr>\n\n");

	ArrayList<Collection> collectionChildren = getCollectionChildren();
	Collections.sort(collectionChildren, NAME_ORDER);
	for (Collection child : collectionChildren) {
	    out.write(child.printTableRow(level + 1, maxDepth,
					  startTime, stopTime));
	}

	ArrayList<Community> communityChildren = getCommunityChildren();
	Collections.sort(communityChildren, NAME_ORDER);
	for (Community child : communityChildren) {
	    out.write(child.printTableRow(level + 1, maxDepth,
					  startTime, stopTime));
	}
	
// 	for (Object child : getCollectionChildren()) {
// 	    out.write(((Node)child).printTableRow(level + 1, maxDepth,
// 						  startTime, stopTime));
// 	}
// 	for (Object child : getCommunityChildren()) {
// 	    out.write(((Node)child).printTableRow(level + 1, maxDepth,
// 						  startTime, stopTime));
// 	}
	return out.toString();
    }
    
}
