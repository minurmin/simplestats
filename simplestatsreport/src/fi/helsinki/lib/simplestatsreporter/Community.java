package fi.helsinki.lib.simplestatsreporter;

public class Community extends Node{

    public Community(int community_id, String community_name,
		     String community_handle) {
	super(community_id, community_name, community_handle);
    }

    public String getCSSClassStringForTableRow() {
	return " class=\"community\"";
    }
}
