package fi.helsinki.lib.simplestatsreporter;

import java.io.*;
import javax.servlet.http.*;

public class Misc {
    public static String fi(String s) {
	if (s == null) {
	    // Probably we have bad data...
	    return "";
	}
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

    public static int getNumber(int defaultNum, HttpServletRequest request, String param) {
        String num = request.getParameter(param);
        if ((num!=null) && (num.length()>0)) {
          try {
            return Integer.parseInt(num);
          }
          catch (NumberFormatException e) {
          }
        }
        return defaultNum;
    }

    /**
     * By default we show last 12 months (if we have statistics for
     * that many months)
     */
    public static int getStartTime(Integer[] times, HttpServletRequest request) {
        return getNumber(times[(times.length > 12) ? times.length - 12 : 0], request, "start_time");
    }

    /**
     * Defaults to last month
     */
    public static int getStopTime(Integer[] times, HttpServletRequest request) {
        return getNumber(times[times.length-1], request, "stop_time");
    }

}