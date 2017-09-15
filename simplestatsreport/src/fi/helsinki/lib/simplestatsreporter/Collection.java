package fi.helsinki.lib.simplestatsreporter;

import java.util.*;

public class Collection extends Node{

    public Collection(int collection_id, String collection_name,
		      String collection_handle,
		      int n_items, int n_bitstreams, long n_bytes) {
	super(collection_id, collection_name, collection_handle,
	      n_items, n_bitstreams, n_bytes);
    }

    public String getCSSClassStringForTableRow() {
	return " class=\"collection\"";
    }

    public String firstTd(int level, int maxDepth,
			  int startTime, int stopTime) {

	String link = "percollection?id=" + getId() + "&amp;start_time=" +
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
