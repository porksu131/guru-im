package com.guru.im.demo.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    public static String formatTime(Long timestamp) {
        if (timestamp == null) return "";

        Date date = new Date(timestamp);
        Date now = new Date();
        SimpleDateFormat format;

        if (isSameDay(date, now)) {
            format = new SimpleDateFormat("HH:mm");
        } else if (isYesterday(date, now)) {
            return "昨天";
        } else if (isThisWeek(date, now)) {
            format = new SimpleDateFormat("E");
        } else {
            format = new SimpleDateFormat("MM/dd");
        }

        return format.format(date);
    }

    private static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isYesterday(Date date, Date now) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date);
        cal2.setTime(now);
        cal2.add(Calendar.DAY_OF_YEAR, -1);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isThisWeek(Date date, Date now) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date);
        cal2.setTime(now);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }
}
