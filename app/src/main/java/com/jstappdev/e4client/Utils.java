package com.jstappdev.e4client;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
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

}
