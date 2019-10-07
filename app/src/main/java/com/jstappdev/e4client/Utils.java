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
        return new File(MainActivity.context.getFilesDir(), session.getFilename()).exists();
    }


    // we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
    static void loadSessionData(final Session session) {
        SessionData sessionData = SessionData.getInstance();
        sessionData.setIsLive(false);

        if (isSessionDownloaded(session)) {

            try {
                final File sessionFile = new File(MainActivity.context.getFilesDir(), session.getFilename());

                Log.d(MainActivity.TAG, "reading " + session.getFilename());

                String basePath = MainActivity.context.getCacheDir().getPath();

                Log.d(MainActivity.TAG, "extracting to directory " + basePath);

                new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                basePath += File.separator;

                /*
                final File ibiFile = new File(basePath + "IBI.csv");
                final File accFile = new File(basePath + "ACC.csv");
                final File tagFile = new File(basePath + "tags.csv");
*/

                // same file format for EDA, HR, BVP, TEMP
                final File edaFile = new File(basePath + "EDA.csv");
                final File tempFile = new File(basePath + "TEMP.csv");
                //   final File bvpFile = new File(basePath + "BVP.csv");
                final File hrFile = new File(basePath + "HR.csv");

                CSVFile data;

                data = new CSVFile(new FileInputStream(edaFile));
                sessionData.setInitialTime((long) data.getInitialTime());
                sessionData.setGsrTimestamps(data.getX());
                sessionData.setGsr(data.getY());
                edaFile.delete();

                data = new CSVFile(new FileInputStream(tempFile));
                sessionData.setTempTimestamps(data.getX());
                sessionData.setTemp(data.getY());
                tempFile.delete();
/*
                data = new CSVFile(new FileInputStream(bvpFile));
                sessionData.setBvpTimestamps(data.getX());
                sessionData.setBvp(data.getY());
                bvpFile.delete();
*/
                data = new CSVFile(new FileInputStream(hrFile));
                sessionData.setHrTimestamps(data.getX());
                sessionData.setHr(data.getY());
                hrFile.delete();

/*
                data = new CSVFile(new FileInputStream(ibiFile));
                sessionData.setIbiTimestamps(data.getX());
                sessionData.setIbi(data.getY());
                ibiFile.delete();
*/
            } catch (FileNotFoundException | ZipException e) {
                e.printStackTrace();
            }
        }

        sessionData.setInitialTime(session.getStartTime());

    }

}
