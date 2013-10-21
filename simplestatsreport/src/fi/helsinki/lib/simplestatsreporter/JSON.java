package fi.helsinki.lib.simplestatsreporter;

import java.lang.System;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class JSON extends GetItemsHttpServlet {

    static long cacheValidUntil = 0;
    static ConcurrentHashMap<String, String> cache = 
	new ConcurrentHashMap<String, String>();

    // As suggested on Stack Overflow:
    @SuppressWarnings("unchecked")
    private final static Map<Object,Object> asMap(JSONObject j)
    {
	return j;
    }

    @SuppressWarnings("unchecked")
    private final static List<Object> asList(JSONArray j)
    {
	return j;
    }

    private static final String validChars = "0123456789/";

    private boolean isHandleOK(String handle) {
	if (handle == null) {
	    return false;
	}
	int length = handle.length();
	if (length == 0) {
	    return false;
	}
	for (int i=0; i < length; i++) {
	    char ch = handle.charAt(i);
	    if (validChars.indexOf(ch) == -1) {
		return false;
	    }	
	}    
	return true;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException
    {
	response.setContentType("application/json"); // As defined in RFC 4627
	PrintWriter out = response.getWriter();

	String handle = request.getParameter("handle");

	// To avoid a SQL exploit.
	if (!isHandleOK(handle)) {
	    response.setContentType("text/plain");
	    response.sendError(response.SC_BAD_REQUEST,
			       "Bad handle.");
	    return;
	}

	Connection conn = null;
	Statement stmt = null;
	try {
	    String topString = request.getParameter("top");
	    String startTimeString = request.getParameter("top_start_time");
	    String stopTimeString = request.getParameter("top_stop_time");

	    String cache_key =
		String.format("%s|%s|%s|%s",
			      handle,
			      (topString != null) ? topString : "",
			      (startTimeString != null) ? startTimeString : "",
			      (stopTimeString != null) ? stopTimeString : "");
	    if (System.currentTimeMillis() <= cacheValidUntil) {
		String cache_value = cache.get(cache_key);
		if (cache_value != null) {
		    out.println(cache_value);
		    return;
		}
	    }
	    else {
		cache.clear();
		synchronized(this) {
		    // 24 hours from now on.
		    cacheValidUntil = System.currentTimeMillis() + 1000*60*60*24;
		}
	    }
	    
	    try { Class.forName(Config.DATABASE_DRIVER); }
	    catch (Exception e) { ; }

	    conn = DriverManager.getConnection(Config.DATABASE_URL,
					       Config.DATABASE_USER,
					       Config.DATABASE_PASSWORD);
	    stmt = conn.createStatement();

	    Integer[] times = DBReader.readTimes(stmt);

	    // Default values (used when paremeters are not given):
	    int top = 0; // This has to be 0 to indicate that we don't want
	                 // information about top items.
	    int startTime = times[0];
	    int stopTime = times[times.length-1];

	    if (topString != null) {
		try {
		    top = Integer.parseInt(topString);
		}
		catch (NumberFormatException e) {
		    response.setContentType("text/plain");
		    response.sendError(response.SC_BAD_REQUEST,
				       "Bad value for 'top' parameter.");
		    return;
		}
	    }

	    if (startTimeString != null) {
		try {
		    startTime = Integer.parseInt(startTimeString);
		}
		catch (NumberFormatException e) {
		    response.setContentType("text/plain");
		    response.sendError(response.SC_BAD_REQUEST,
				       "Bad value for 'top_start_time' parameter.");
		    return;
		}
	    }

	    if (stopTimeString != null) {
		try {
		    stopTime = Integer.parseInt(stopTimeString);
		}
		catch (NumberFormatException e) {
		    response.setContentType("text/plain");
		    response.sendError(response.SC_BAD_REQUEST,
				       "Bad value for 'top_stop_time' parameter.");
		    return;
		}
	    }


	    try {
		String value = getDownloadNumberForHandle(stmt, handle, top,
							  startTime, stopTime);
		cache.put(cache_key, value);
		out.println(value);
	    }
	    catch (UnknownHandleException e) {
 		response.setContentType("text/plain");
 		response.sendError(response.SC_NOT_FOUND, "Unknown handle.");
	    }
	    catch (SQLException e) {
		response.setContentType("text/plain");
		response.sendError(response.SC_INTERNAL_SERVER_ERROR ,
				   "SqlException: " + e.toString());
	    }
	}
	catch (SQLException e) {
	    response.setContentType("text/plain");
	    response.sendError(response.SC_INTERNAL_SERVER_ERROR ,
			       "SqlException: " + e.toString());
	}

	if (stmt != null) {
	    try { stmt.close(); }
	    catch (SQLException e) {
		response.setContentType("text/plain");
		response.sendError(response.SC_INTERNAL_SERVER_ERROR ,
				   "Close failed: " + e.toString());
	    }
	}
	if (conn != null) {
	    try { conn.close(); }
	    catch (SQLException e) {
		response.setContentType("text/plain");
		response.sendError(response.SC_INTERNAL_SERVER_ERROR ,
				   "Close failed: " + e.toString());
	    }
	}
    }

    private String getDownloadNumberForHandle(Statement stmt, String handle,
					      int top,
					      int startTime, int stopTime)
	throws UnknownHandleException, SQLException {

	Hashtable<String, Integer> stats = null;
	int itemId = -1;
	int collectionId = -1;
	int communityId = -1;

	if (handle.equals("0")) {
	    /* A special case: the whole DSpace does not have a handle, but
	       in the database we represent the whole DSpace as a community (
	       with id 0). */
	    stats = DBReader.statsForCommunity(stmt, 0);
	    communityId = 0;
	}
	else {
	    /* A normal case: we have a handle but we don't know if that
	       belongs to a community, a collection or an item... so we
	       go through all the options until we find what it is. */

	    itemId = DBReader.handleToItemId(stmt, handle);
       
	    if (itemId != -1) {
		stats = DBReader.statsForItem(stmt, itemId);
	    }
	    else {
		collectionId = DBReader.handleToCollectionId(stmt, handle);
		if (collectionId != -1) {
		    stats = DBReader.statsForCollection(stmt, collectionId);
		}
		else {
		    communityId = DBReader.handleToCommunityId(stmt,
								   handle);
		    if (communityId != -1) {
			stats = DBReader.statsForCommunity(stmt, communityId);
		    }
		    else {
			throw new UnknownHandleException(handle);
		    }
		}
	    }
	}

	JSONObject obj = new JSONObject();
	for (String key: stats.keySet()) {
	    asMap(obj).put(key, stats.get(key));
	}

	Node node;
	if (top > 0) {
	    Hashtable<Integer, Community> communities = DBReader.readCommunities(stmt);
	    Hashtable<Integer, Collection> collections = DBReader.readCollections(stmt);
	    Hashtable<Integer, Item> items = DBReader.readItems(stmt);

	    DBReader.readCommunitiesStats(stmt, communities, startTime, stopTime);
	    DBReader.readCollectionsStats(stmt, collections, startTime, stopTime);
	    DBReader.readItemsStats(stmt, items, startTime, stopTime);

	    DBReader.setRelations(stmt, communities, collections, items);

	    if (itemId != -1) {
		node = items.get(itemId);
	    }
	    else if (collectionId != -1) {
		node = collections.get(collectionId);
	    }
	    else {
		node = communities.get(communityId);
	    }

	    ArrayList<Item> itemList = getItems(node);
	    ArrayList<IntItemPair>intItemPairs = new ArrayList<IntItemPair>();
	    for (Item item : itemList) {
		int count = 0;
		for (int time : new TimeSpan(startTime, stopTime)) {
		    count += item.getCounter(time);
		}

		intItemPairs.add(new IntItemPair(count, item));
	    }
	    Collections.sort(intItemPairs, IntItemPair.REV_NUMBER_ORDER);
	    int nItems = Math.min(intItemPairs.size(), top);

	    JSONArray topList = new JSONArray();
	    String prefix = Config.DSPACE_URL + "/handle/";
	    for (int i=0; i < nItems; i++) {
		JSONObject item = new JSONObject();
		IntItemPair pair = intItemPairs.get(i);
		asMap(item).put("name", pair.item.getName());
		asMap(item).put("url", prefix + pair.item.getHandle());
		asMap(item).put("count", pair.integer);
		asList(topList).add(item);
	    }
	    asMap(obj).put("top", topList);
	}
	return obj.toJSONString();
    }
}
