package fi.helsinki.lib.simplestatsreporter;

public class Item extends Node{

    public Item(int item_id, String item_name, String item_handle) {
	super(item_id, item_name, item_handle);
    }

    public String printTableRow(int level, int maxDepth) {
	return "";
    }

}
