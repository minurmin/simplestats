package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class JSON extends HttpServlet {

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
	    try { Class.forName(Config.DATABASE_DRIVER); }
	    catch (Exception e) { ; }

	    conn = DriverManager.getConnection(Config.DATABASE_URL,
					       Config.DATABASE_USER,
					       Config.DATABASE_PASSWORD);
	    stmt = conn.createStatement();

	    try {
		out.println(getDownloadNumberForHandle(stmt, handle));
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

    private String getDownloadNumberForHandle(Statement stmt, String handle)
	throws UnknownHandleException, SQLException {

	Hashtable<String, Integer> stats = null;

	if (handle.equals("0")) {
	    /* A special case: the whole DSpace does not have a handle, but
	       in the database we represent the whole DSpace as a community (
	       with id 0). */
	    stats = DBReader.statsForCommunity(stmt, 0);
	}
	else {
	    /* A normal case: we have a handle but we don't know if that
	       belongs to a community, a collection or an item... so we
	       go through all the options until we find what it is. */

	    int itemId = DBReader.handleToItemId(stmt, handle);
       
	    if (itemId != -1) {
		stats = DBReader.statsForItem(stmt, itemId);
	    }
	    else {
		int collectionId = DBReader.handleToCollectionId(stmt, handle);
		if (collectionId != -1) {
		    stats = DBReader.statsForCollection(stmt, collectionId);
		}
		else {
		    int communityId = DBReader.handleToCommunityId(stmt,
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

	// Finally return a JSON representation of the hashtable:
	StringBuffer out = new StringBuffer();
	out.append("{");
	for (String key: stats.keySet()) {
	    out.append("\"" + key + "\":");
	    out.append(stats.get(key));
	    out.append(",");
	}
	int length = out.length();
	out.replace(length-1, length, "}");

	return out.toString();
    
    }
}
