package com.jstappdev.e4client;

import android.text.format.DateFormat;

import com.jstappdev.e4client.data.Session;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static String getDate(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("dd. MMM. yyyy - HH:mm", cal).toString();
    }

    public static String getDuration(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("HH:mm:ss", cal).toString();
    }

    public static boolean isSessionDownloaded(final Session session) {
        final File file = new File(MainActivity.context.getFilesDir(), session.getFilename());
        return file.exists();
    }

}
