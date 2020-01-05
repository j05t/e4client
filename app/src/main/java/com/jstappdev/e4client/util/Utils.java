package com.jstappdev.e4client.util;

import android.content.Context;
import android.text.format.DateFormat;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.data.E4Session;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static boolean isUploading = false;


    public static List<Float> removeAnomalies(final List<Float> values) {
        List<Float> l = new ArrayList<>();

        if (values.isEmpty()) return l;

        l.add(values.get(0));

        for (int i = 1; i < values.size(); i++) {
            float d = values.get(i - 1) - values.get(i);

            if (Math.abs(d) < 0.2 * values.get(i - 1))
                l.add(values.get(i));
            else
                l.add(values.get(i - 1) + d * .1f);
        }

        return l;
    }

    private static double variance(final List<Float> values) {
        double sum = 0;

        for (float f : values) sum += f;

        final double mean = sum / (double) values.size();

        // Compute sum of squared differences with mean
        double sqDiff = 0;

        for (float f : values) sqDiff += (f - mean) * (f - mean);

        return sqDiff / values.size();
    }

    public static float calcHrvSDRR(final List<Float> values) {
        return (float) (1000f * Math.sqrt(variance(values)));
    }

    public static float calcHrvSDNN(final List<Float> values) {
        return (float) (1000f * Math.sqrt(variance(removeAnomalies(values))));
    }

    // RMSSD ("root mean square of successive differences"), the square root of
    // the mean of the squares of the successive differences between adjacent NNs.
    public static float calcHrvRMSSD(final List<Float> values) {
        double s = 0;
        for (int i = 1; i < values.size(); i++) {
            float d = values.get(i) - values.get(i - 1);
            s += d * d;
        }
        return (float) (1000f * Math.sqrt(s / values.size()));
    }

    // SDSD ("standard deviation of successive differences"), the standard deviation
    // of the successive differences between adjacent NNs.
    public static float calcHrvSDSD(final List<Float> values) {
        final List<Float> l = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            float d = values.get(i) - values.get(i - 1);
            l.add(d);
        }
        return (float) (1000f * Math.sqrt(variance(l)));
    }

    // NN50, the number of pairs of successive NNs that differ by more than 50 ms.
    public static int calcHrvNN50(final List<Float> values) {
        return calcHrvNN(values, 0.05f);
    }

    // NN20, the number of pairs of successive NNs that differ by more than 20 ms.
    public static int calcHrvNN20(final List<Float> values) {
        return calcHrvNN(values, 0.02f);
    }

    private static int calcHrvNN(final List<Float> values, final float maxDiff) {
        int c = 0;
        for (int i = 1; i < values.size(); i++) {
            float d = values.get(i) - values.get(i - 1);
            if (Math.abs(d) > maxDiff) c++;
        }
        return c;
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
