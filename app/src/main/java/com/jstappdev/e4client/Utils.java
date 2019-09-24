package com.jstappdev.e4client;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class Utils {

    public static String getFileName(final Session session) {
        final String startTime = getDate(session.getStart_time());
        return "e4_session_" + startTime + "_" + session.getId() + ".zip";
    }
    private static String getDate(final long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("yyyy-MM-dd", cal).toString();
    }
}
