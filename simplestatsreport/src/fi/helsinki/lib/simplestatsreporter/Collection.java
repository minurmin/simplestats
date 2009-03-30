package fi.helsinki.lib.simplestatsreporter;

import java.util.*;

public class Collection extends Node{

    public Collection(int collection_id, String collection_name,
		      String collection_handle) {
	super(collection_id, collection_name, collection_handle);
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
	    Misc.fi(getName()) + "</a></td>";
    }


}
