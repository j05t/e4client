package com.jstappdev.e4client.util;

import android.content.Context;
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
import java.lang.ref.WeakReference;

// we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
public class LoadAndViewSessionData extends AsyncTask<E4Session, Void, Boolean> {

    private final SharedViewModel viewModel;
    private final WeakReference<MainActivity> contextRef;

    public LoadAndViewSessionData(Context context) {
        contextRef = new WeakReference<MainActivity>((MainActivity) context);
        viewModel = ViewModelProviders.of((MainActivity) context).get(SharedViewModel.class);
    }

    @Override
    protected Boolean doInBackground(final E4Session... e4Sessions) {
        final E4Session e4Session = e4Sessions[0];

        if (viewModel.isSessionDownloaded(e4Session)) {

            viewModel.setIsLoading(true);

            viewModel.setLoadingProgress(5);

            E4SessionData.clear();

            E4SessionData.getInstance().setDescription(String.format("%s\nSession ID: %s\nDuration: %s", e4Session.getStartDate(), e4Session.getId(), e4Session.getDurationAsString()));

            try {
                final File sessionFile = new File(contextRef.get().getFilesDir(), e4Session.getZIPFilename());

                String basePath = contextRef.get().getCacheDir().getPath();

                Utils.trimCache(contextRef.get());

                new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                basePath += File.separator;

                viewModel.setLoadingProgress(10);


                final File accFile = new File(basePath + "ACC.csv");
                if (accFile.exists())
                    try (BufferedReader reader = new BufferedReader(new FileReader(accFile))) {
                        String line = reader.readLine();

                        final double initialTime = Double.parseDouble(line.split(",")[0]);
                        final double samplingRate = 1d / Double.parseDouble(reader.readLine().split(",")[0]);

                        int lineNumber = 0;
                        float sum = 0;

                        while ((line = reader.readLine()) != null) {

                            lineNumber++;

                            String[] split = line.split(",");
                            final int x = Integer.parseInt(split[0]);
                            final int y = Integer.parseInt(split[1]);
                            final int z = Integer.parseInt(split[2]);
                            final float mag = Utils.magnitude(x, y, z);

                            // we just load the average acceleration for every 55 data points
                            sum += mag;
                            if (lineNumber % 55 == 0) {
                                E4SessionData.getInstance().getAccMagTimestamps().add(initialTime + (samplingRate * lineNumber));
                                E4SessionData.getInstance().getAccMag().add(sum / 55);
                                sum = 0;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                viewModel.setLoadingProgress(30);

                final File tagFile = new File(basePath + "tags.csv");
                if (tagFile.exists())
                    try (BufferedReader reader = new BufferedReader(new FileReader(tagFile))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            Log.d(MainActivity.TAG, "loaded tag " + line);

                            E4SessionData.getInstance().getTags().add(Double.parseDouble(line));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                viewModel.setLoadingProgress(35);

                final File ibiFile = new File(basePath + "IBI.csv");
                if (tagFile.exists())
                    try (BufferedReader reader = new BufferedReader(new FileReader(ibiFile))) {
                        String line = reader.readLine();

                        if (line != null) {

                            final double initialTime = Double.parseDouble(line.split(",")[0]);
                            double timestamp = initialTime;

                            while ((line = reader.readLine()) != null) {
                                final String[] split = line.split(",");
                                final double plusTime = Double.parseDouble(split[0]);
                                final float ibi = Float.parseFloat(split[1]);
                                timestamp = initialTime + plusTime;

                                E4SessionData.getInstance().getIbi().add(ibi);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                viewModel.setLoadingProgress(55);

                // same file format for EDA, HR, BVP, TEMP
                final File edaFile = new File(basePath + "EDA.csv");
                final File tempFile = new File(basePath + "TEMP.csv");
                final File hrFile = new File(basePath + "HR.csv");
                //   final File bvpFile = new File(basePath + "BVP.csv");

                CSVFile data;

                data = new CSVFile(new FileInputStream(edaFile));
                E4SessionData.getInstance().setInitialTime((long) data.getInitialTime());
                E4SessionData.getInstance().setGsrTimestamps(data.getX());
                E4SessionData.getInstance().setGsr(data.getY());
                edaFile.delete();
                viewModel.setLoadingProgress(80);

                data = new CSVFile(new FileInputStream(tempFile));
                E4SessionData.getInstance().setTempTimestamps(data.getX());
                E4SessionData.getInstance().setTemp(data.getY());
                tempFile.delete();
                viewModel.setLoadingProgress(90);

                data = new CSVFile(new FileInputStream(hrFile));
                E4SessionData.getInstance().setHrTimestamps(data.getX());
                E4SessionData.getInstance().setHr(data.getY());
                hrFile.delete();
                viewModel.setLoadingProgress(100);

                E4SessionData.getInstance().setInitialTime(e4Session.getStartTime());

            } catch (FileNotFoundException e) {
                viewModel.getCurrentStatus().postValue("File not found: " + e.getMessage());
                return false;
            } catch (ZipException e) {
                viewModel.getCurrentStatus().postValue("Corrupted ZIP file.");
                e.printStackTrace();
                return false;
            }
        } else {
            viewModel.getCurrentStatus().postValue("Session data not downloaded.");
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        viewModel.setIsLoading(false);

        if (success)
            contextRef.get().openCharts();
    }
}
