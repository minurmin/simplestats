package fi.helsinki.lib.simplestatsreporter;

import java.util.*;

public class IntItemPair {
    public Integer integer;
    public Item item;

    public IntItemPair(int intgr, Item itm) {
	integer = intgr;
	item = itm;
    }

    static final Comparator<IntItemPair> REV_NUMBER_ORDER =
	new Comparator<IntItemPair>() {
	public int compare(IntItemPair pair1, IntItemPair pair2) {
	    return pair2.integer.compareTo(pair1.integer);
	}
    };

}

