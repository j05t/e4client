package com.jstappdev.e4client.util;

import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.CSVFile;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.data.E4SessionData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

// we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
public class LoadAndViewSessionData extends AsyncTask<E4Session, String, Boolean> {

    private final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
    private final E4SessionData e4SessionData = viewModel.getSessionData();

    @Override
    protected Boolean doInBackground(final E4Session... e4Sessions) {
        final E4Session e4Session = e4Sessions[0];

        publishProgress(String.format("Loading session %s data..", e4Session.getId()));

        if (Utils.isSessionDownloaded(e4Session)) {

            try {
                final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

                Log.d(MainActivity.TAG, "reading " + e4Session.getZIPFilename());

                String basePath = MainActivity.context.getCacheDir().getPath();

                Log.d(MainActivity.TAG, "extracting to directory " + basePath);

                Utils.trimCache(MainActivity.context);

                new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                basePath += File.separator;

                /*
                final File ibiFile = new File(basePath + "IBI.csv");
                final File accFile = new File(basePath + "ACC.csv");
                 */

                final File tagFile = new File(basePath + "tags.csv");

                publishProgress("Processing tag data");
                if (tagFile.exists())
                    try (BufferedReader reader = new BufferedReader(new FileReader(tagFile))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            Log.d(MainActivity.TAG, "loaded tag " + line);

                            e4SessionData.getTags().add(Double.parseDouble(line));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                // same file format for EDA, HR, BVP, TEMP
                final File edaFile = new File(basePath + "EDA.csv");
                final File tempFile = new File(basePath + "TEMP.csv");
                //   final File bvpFile = new File(basePath + "BVP.csv");
                final File hrFile = new File(basePath + "HR.csv");

                CSVFile data;

                publishProgress("Processing EDA data");
                data = new CSVFile(new FileInputStream(edaFile));
                e4SessionData.setInitialTime((long) data.getInitialTime());
                e4SessionData.setGsrTimestamps(data.getX());
                e4SessionData.setGsr(data.getY());
                edaFile.delete();

                publishProgress("Processing TEMP data");
                data = new CSVFile(new FileInputStream(tempFile));
                e4SessionData.setTempTimestamps(data.getX());
                e4SessionData.setTemp(data.getY());
                tempFile.delete();

                publishProgress("Processing HR data");
                data = new CSVFile(new FileInputStream(hrFile));
                e4SessionData.setHrTimestamps(data.getX());
                e4SessionData.setHr(data.getY());
                hrFile.delete();

                /*
                data = new CSVFile(new FileInputStream(bvpFile));
                e4SessionData.setBvpTimestamps(data.getX());
                e4SessionData.setBvp(data.getY());
                bvpFile.delete();
                */

                e4SessionData.setInitialTime(e4Session.getStartTime());

                publishProgress(String.format("Loaded data for session %s", e4Session.getId()));

            } catch (FileNotFoundException e) {
                publishProgress("File not found: " + e.getMessage());
                return false;
            } catch (ZipException e) {
                publishProgress("Corrupted ZIP file.");
                e.printStackTrace();
                return false;
            }
        } else {
            publishProgress("Session data not downloaded!");
            return false;
        }

        return true;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length > 0) viewModel.getCurrentStatus().setValue(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success)
            MainActivity.context.openCharts();
    }
}
