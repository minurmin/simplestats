package fi.helsinki.lib.simplestatsreporter;

import java.io.*;

public class Misc {
    public static String fi(String s) {
	String t = s.trim();
	int startIndex = t.indexOf("fi=");
	if (startIndex >= 0) {
	    int stopIndex = t.indexOf('|', startIndex);
	    return t.substring(startIndex + 3, stopIndex);
	}
	else {
	    return t;
	}
    }	

    public static String monthHeaders(int startTime, int stopTime) {
 	StringWriter out = new StringWriter();

	for(int time : new TimeSpan(startTime,stopTime)) {
	    int year = time / 100;
	    int month = time % 100;
	    out.write("<th>" + month + " / " + year + "</th>");
	}
	out.write("<th class=\"total\">Total</th>");
	
	return out.toString();
    }
}