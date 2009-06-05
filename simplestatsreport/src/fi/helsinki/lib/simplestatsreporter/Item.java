package fi.helsinki.lib.simplestatsreporter;

public class Item extends Node{
    
    public Item(int item_id, String item_name, String item_handle,
		int n_items, int n_bitstreams, long n_bytes) {
	super(item_id, item_name, item_handle, n_items, n_bitstreams, n_bytes);
    }

    public String printTableRow(int level, int maxDepth) {
	return "";
    }

}
