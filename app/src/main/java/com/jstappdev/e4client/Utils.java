package com.jstappdev.e4client;

import android.text.format.DateFormat;
import android.util.Log;

import com.jstappdev.e4client.data.CSVFile;
import com.jstappdev.e4client.data.Session;
import com.jstappdev.e4client.data.SessionData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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


    public static SessionData loadSessionData(final Session session) {
        SessionData sessionData = SessionData.getInstance();

        if (isSessionDownloaded(session)) {

            try {
                final File sessionFile = new File(MainActivity.context.getFilesDir(), session.getFilename());

                Log.d(MainActivity.TAG, "reading " + session.getFilename());

                String basePath = MainActivity.context.getCacheDir().getPath();

                Log.d(MainActivity.TAG, "extracting to directory " + basePath);

                new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                basePath += File.separator;

                final String edaFileName = basePath + "EDA.csv";
                final String tempFileName = basePath + "TEMP.csv";
                final String ibiFileName = basePath + "IBI.csv";
                final String hrFileName = basePath + "HR.csv";


                final String accFileName = basePath + "ACC.csv";
                final String tagFileName = basePath + "tags.csv";

                final File edaFile = new File(edaFileName);
                Log.d(MainActivity.TAG, edaFileName);
                Log.d(MainActivity.TAG, String.valueOf(edaFile.exists()));


                CSVFile edaData = new CSVFile(new FileInputStream(edaFile));

                sessionData.setGsrTimestamps(edaData.getX());
                sessionData.setGsr(edaData.getY());

                sessionData.setInitialTime((long) edaData.getInitialTime());

            } catch (FileNotFoundException | ZipException e) {
                e.printStackTrace();
            }
        }

        sessionData.setInitialTime(session.getStartTime());

        return sessionData;
    }

}
