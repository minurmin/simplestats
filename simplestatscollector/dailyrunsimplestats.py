#!/usr/bin/env python

# Edit a crontab file so that this script is run once each day.

# This script collects statistics also for the current month, so the statistics
# for the last month will of course always be incomplete until the end of the
# month. If you don't like that, make a simpler script and run it only once
# a month.

import sys
import datetime

import simplestats

def main(argv=None):

    if argv is None:
        argv = sys.argv

    # NOTE: datetime.date.today() returns the local time.
    today = datetime.date.today()
    yesterday = today - datetime.timedelta(1)

    if today.day == 1:
        # First day of each month we collect statistics for the previous
        # and the current month...
        start_year, start_month = yesterday.year, yesterday.month
    else:
        # ... but normally only for the current month.
        start_year, start_month = today.year, today.month

    stop_year, stop_month = today.year, today.month

    start_time = str(start_year) + ('%2d' % start_month).replace(' ', '0')
    stop_time = str(stop_year) + ('%2d' % stop_month).replace(' ', '0')
        
    simplestats.main(['', start_time, stop_time])


if __name__ == '__main__':
    sys.exit(main())
