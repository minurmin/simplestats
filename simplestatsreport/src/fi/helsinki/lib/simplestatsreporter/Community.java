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

    public String firstTd(int level, int maxDepth,
			  int startTime, int stopTime) {

	String link = "percommunity?id=" + getId() + "&amp;start_time=" +
	    startTime + "&amp;stop_time=" + stopTime;

	return "<td" + getCSSClassStringForTableRow() + " colspan=\"" +
	    (maxDepth-level) + "\"><a href=\"" + link + "\">" +
	    Misc.fi(getName()) + "</a>" +
	    " (" +
	    getNItems() + " items, " +
	    getNBitstreams() + " bitstreams, " +
	    getNBytes() / (1024*1024) + " megabytes) " +
	    "</td>";
    }
}
