package fi.helsinki.lib.simplestatsreporter;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public abstract class SimpleStatsReporter extends HttpServlet {

    public abstract String htmlContent(Statement stmt,
				       HttpServletRequest request)
	throws SQLException;

    public String extraHeaders() {
	return "";
    }

    public String htmlFooter() {
	return("<div class=\"footer\"><a href=\".\">Download statistics front page</a></div>");
    }
    
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response)
	throws IOException, ServletException
    {
	response.setContentType("text/html;charset=utf-8");
	PrintWriter out = response.getWriter();

	out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 " +
		    "Strict//EN\" "+
		    "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

	out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
	out.println("<head>");
	out.println("<link rel=\"stylesheet\" " +
		    "href=\"stylesheet.css\" />");
	out.println(extraHeaders());
	out.println("<title>Monthly download statistics</title></head>");
	out.println("<body>");
	out.println("<h1>Monthly download statistics</h1>");
	

	Connection conn = null;
	Statement stmt = null;
	try {

	    try {
		Class.forName(Config.DATABASE_DRIVER);
	    }
	    catch (Exception e) {
		;
	    }

	    conn = DriverManager.getConnection(Config.DATABASE_URL,
					       Config.DATABASE_USER,
					       Config.DATABASE_PASSWORD);
	    stmt = conn.createStatement();

	    out.println(htmlContent(stmt, request));
	}
	catch (SQLException sqlEx) {
	    out.println("SqlException: " + sqlEx.toString());
	}

	if (stmt != null) {
	    try {
		stmt.close();
	    }
	    catch (SQLException sqlEx) {
		out.println("Close failed: " + sqlEx.toString());
	    }
	}		

	if (conn != null) {
	    try {
		conn.close();
	    }
	    catch (SQLException sqlEx) {
		out.println("Close failed: " + sqlEx.toString());
	    }
	}		

	out.println(htmlFooter());

	out.println("</body>");
	out.println("</html>");
    }
}
