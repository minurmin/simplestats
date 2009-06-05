package fi.helsinki.lib.simplestatsreporter;

public class Community extends Node{

    public Community(int community_id, String community_name,
		     String community_handle,
		     int n_items, int n_bitstreams, long n_bytes) {
	super(community_id, community_name, community_handle,
	      n_items, n_bitstreams, n_bytes);
    }

    public String getCSSClassStringForTableRow() {
	return " class=\"community\"";
    }
}
