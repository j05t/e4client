package com.jstappdev.e4client.util;

import android.content.Context;
import android.text.format.DateFormat;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.data.E4Session;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static boolean isUploading = false;

    private static double variance(final List<Float> list) {
        double sum = 0;

        for (float f : list) sum += f;

        double mean = sum / (double) list.size();

        // Compute sum of squared differences with mean
        double sqDiff = 0;

        for (float f : list) sqDiff += (f - mean) * (f - mean);

        return sqDiff / list.size();
    }

    // todo: remove anomalies in IBI data to calculate SDNN
    public static float calcHrvSDRR(final List<Float> list) {
        return (float) (1000f * Math.sqrt(variance(list)));
    }

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

    public static long getCurrentTimestamp() {
        final Calendar cal = Calendar.getInstance();
        final Date now = new Date();
        cal.setTime(now);

        return cal.getTimeInMillis() + TimeZone.getDefault().getRawOffset();
    }

    public static boolean isSessionDownloaded(final E4Session e4Session) {
        return new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename()).exists();
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    static float magnitude(final int x, final int y, final int z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }


}
