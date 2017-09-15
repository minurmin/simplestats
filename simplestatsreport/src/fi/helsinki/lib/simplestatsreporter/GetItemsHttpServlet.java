package fi.helsinki.lib.simplestatsreporter;

import javax.servlet.http.HttpServlet;
import java.util.ArrayList;

public abstract class GetItemsHttpServlet extends HttpServlet {

    protected ArrayList<Item> getItems(Node node) {
	ArrayList<Item> retVal = new ArrayList<Item>();
	if (node instanceof Item) {
	    retVal.add((Item)node);
	}
	for (Node child : node.getChildren()) {
	    retVal.addAll(getItems(child));
	}
	return retVal;
    }
}

