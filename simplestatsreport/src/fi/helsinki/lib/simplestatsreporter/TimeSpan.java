package fi.helsinki.lib.simplestatsreporter;

import java.util.Iterator;

public class TimeSpan implements Iterable<Integer>, Iterator<Integer> {

    private int year;
    private int month;
    private int stopTime;

    public TimeSpan(int startTime, int stopTime) {
	year = startTime / 100;
	month = startTime % 100;
	this.stopTime = stopTime;
    }

    // Implements Iterable interface.
    public Iterator<Integer> iterator() {
	return this;
    }

    // The following three methods implement Iterator interface.
    public boolean hasNext() {
	return (year * 100 + month <= stopTime) ? true : false;
    }

    public Integer next() {
	int retVal = year * 100 + month;
	
	month++;
	if (month > 12) {
	    month = 1;
	    year++;
	}

	return retVal;
    }

    public void remove() {
	throw new UnsupportedOperationException();
    }

}