package com.jstappdev.e4client;

import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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

    public static boolean isSessionDownloaded(final E4Session e4Session) {
        return new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename()).exists();
    }

    public static float magnitude(final int x, final int y, final int z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static class UploadE4SessionToGoogleFit extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);

        @Override
        protected Boolean doInBackground(final E4Session... e4Sessions) {
            final E4Session e4Session = e4Sessions[0];

            if (!Utils.isSessionDownloaded(e4Session)) {
                publishProgress("Session data not downloaded.");
                return false;
            }

            Log.d(MainActivity.TAG, "Session start: " + e4Session.getStartTime() + " duration: " + e4Session.getDuration()
                    + " end: " + (e4Session.getStartTime() + e4Session.getDuration()));

            // Create a Google Fit session with metadata about the activity
            final Session session = new Session.Builder()
                    .setName("sample session")
                    .setDescription("Empatica E4 Session")
                    .setIdentifier(e4Session.getId())
                    .setActivity(FitnessActivities.SLEEP)
                    .setStartTime(e4Session.getStartTime(), TimeUnit.MILLISECONDS)
                    .setEndTime(e4Session.getStartTime() + e4Session.getDuration(), TimeUnit.MILLISECONDS)
                    .build();

            // datatype for heart rate is predefined by google
            final DataSource hrDataSource =
                    new DataSource.Builder()
                            .setAppPackageName(MainActivity.context)
                            .setDataType(DataType.TYPE_HEART_RATE_BPM)
                            .setStreamName("Heart Rate")
                            .setType(DataSource.TYPE_RAW)
                            .build();

            try {
                final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

                Log.d(MainActivity.TAG, "reading " + e4Session.getZIPFilename());

                String basePath = MainActivity.context.getCacheDir().getPath();

                Log.d(MainActivity.TAG, "extracting to directory " + basePath);

                new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                basePath += File.separator;

                // same file format for EDA, HR, BVP, TEMP
                final File edaFile = new File(basePath + "EDA.csv");
                final File tempFile = new File(basePath + "TEMP.csv");
                final File bvpFile = new File(basePath + "BVP.csv");
                final File hrFile = new File(basePath + "HR.csv");

                final File tagFile = new File(basePath + "tags.csv");
                final File ibiFile = new File(basePath + "IBI.csv");
                final File accFile = new File(basePath + "ACC.csv");

                publishProgress("Processing HR data");
                uploadFile(hrFile, hrDataSource, session);

                final String packageName = MainActivity.context.getPackageName();

                for (final DataType dataType : MainActivity.dataTypes) {
                    Log.d(MainActivity.TAG, "uploading " + dataType.getName());

                    final DataSource.Builder dataSourceBuilder = new DataSource.Builder()
                            .setAppPackageName(MainActivity.context)
                            .setDataType(dataType)
                            .setType(DataSource.TYPE_RAW);

                    if (dataType.getName().equals(packageName + ".eda")) {
                        dataSourceBuilder.setStreamName("Electrodermal Activity");
                        publishProgress("Uploading EDA data");
                        uploadFile(edaFile, dataSourceBuilder.build(), session);
                    } else if (dataType.getName().equals(packageName + ".temp")) {
                        dataSourceBuilder.setStreamName("Peripheral Skin Temperature");
                        publishProgress("Uploading TEMP data");
                        uploadFile(tempFile, dataSourceBuilder.build(), session);
                    } else if (dataType.getName().equals(packageName + ".bvp")) {
                        dataSourceBuilder.setStreamName("Blood Volume Pressure");
                        publishProgress("Uploading BVP data");
                        uploadFile(bvpFile, dataSourceBuilder.build(), session);
                    } // need special treatment
                    else if (dataType.getName().equals(packageName + ".ibi")) {
                        dataSourceBuilder.setStreamName("Interbeat Interval");
                        publishProgress("Uploading IBI data");
                        uploadIbiFile(ibiFile, dataSourceBuilder.build(), session);
                    } else if (dataType.getName().equals(packageName + ".acc")) {
                        dataSourceBuilder.setStreamName("Acceleration");
                        publishProgress("Uploading ACC data");
                        uploadAccFile(accFile, dataSourceBuilder.build(), session);
                    }
                    /* tags may not be useful in Google Fit
                    else if (dataType.getName().equals(packageName + ".tags")) {
                        dataSourceBuilder.setStreamName("tags");
                        publishProgress("Uploading tag data");
                        uploadTagFile(tagFile, dataSourceBuilder.build(), session);
                    }
                     */
                }

                publishProgress(String.format("Session %s upload complete.", e4Session.getId()));

            } catch (ZipException e) {
                e.printStackTrace();
                return false;
            }

            return null;
        }


        private void uploadFile(final File file, final DataSource dataSource, final Session session) {
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine());
                final double samplingRate = 1d / Double.parseDouble(reader.readLine());
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {

                    if (index < 1000) {
                        xBuf[index] = initialTime + samplingRate * lineNumber;
                        yBuf[index] = Float.parseFloat(line);
                        index++;
                        lineNumber++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void uploadAccFile(File file, DataSource dataSource, Session session) {
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
                final double samplingRate = 1d / Double.parseDouble(reader.readLine().split(",")[0]);
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    final String[] acc = line.split(",");
                    final int x = Integer.parseInt(acc[0]);
                    final int y = Integer.parseInt(acc[1]);
                    final int z = Integer.parseInt(acc[2]);
                    final float magnitude = magnitude(x, y, z);

                    if (index < 1000) {
                        xBuf[index] = initialTime + samplingRate * lineNumber;
                        yBuf[index] = magnitude;
                        index++;
                        lineNumber++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void uploadIbiFile(File file, DataSource dataSource, Session session) {
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
                double timestamp;

                String line;

                /* IBI
Time between individuals heart beats extracted from the BVP signal.
No sample rate is needed for this file.
The first column is the time (respect to the initial time) of the detected inter-beat interval expressed in seconds (s).
The second column is the duration in seconds (s) of the detected inter-beat interval (i.e., the distance in seconds from the previous beat).
                 */
                while ((line = reader.readLine()) != null) {
                    final String[] split = line.split(",");
                    final double plusTime = Double.parseDouble(split[0]);
                    final float ibi = Float.parseFloat(split[1]);

                    timestamp = initialTime + plusTime;

                    // fixme
                    if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                        Log.e(MainActivity.TAG, "skipping IBI beyond endtime with timestamp " + timestamp);
                        continue;
                    }

                    if (index < 1000) {
                        xBuf[index] = timestamp;
                        yBuf[index] = ibi;
                        index++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void uploadDataChunk(double[] timestamps, float[] values, final int index, Session session, DataSource dataSource) {
            Log.i(MainActivity.TAG, "Inserting the session in the Sessions API: " + dataSource.getStreamName());

            final DataSet.Builder dataSetBuilder = DataSet.builder(dataSource);

            for (int i = 0; i < index; i++) {
                final float value = values[i];
                final double timestamp = timestamps[i];

                final DataPoint dataPoint = DataPoint.builder(dataSource)
                        // fixme: rounding double to closest long..
                        .setTimestamp(Math.round(timestamp), TimeUnit.MILLISECONDS)
                        .setFloatValues(value)
                        .build();

                dataSetBuilder.add(dataPoint);
            }

            final String message = String.format("%d datapoints uploaded.", index);
            Log.d(MainActivity.TAG, message);

            insertData(session, dataSetBuilder, message);
        }

        private void insertData(final Session session, final DataSet.Builder dataSetBuilder, final String message) {

            // Build a session insert request
            final SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                    .setSession(session)
                    .addDataSet(dataSetBuilder.build())
                    .build();

            // invoke the Sessions API to insert the session and await the result,
            Fitness.getSessionsClient(MainActivity.context, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.context)))
                    .insertSession(insertRequest)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // At this point, the session has been inserted and can be read.
                            publishProgress(message);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            publishProgress("There was a problem inserting the session: " +
                                    e.getLocalizedMessage());
                        }
                    });
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values.length > 0) viewModel.getSessionStatus().setValue(values[0]);
        }
    }

    // we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
    public static class LoadAndViewSessionData extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        final E4SessionData e4SessionData = viewModel.getSessionData();

        private WeakReference<View> view;

        LoadAndViewSessionData(View v) {
            view = new WeakReference<>(v);
        }

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

                    new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                    basePath += File.separator;

                    /*
                    final File ibiFile = new File(basePath + "IBI.csv");
                    final File accFile = new File(basePath + "ACC.csv");
                     */

                    final File tagFile = new File(basePath + "tags.csv");

                    publishProgress("Processing tag data");
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

                } catch (FileNotFoundException | ZipException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                publishProgress("Session data not downloaded!");
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values.length > 0) viewModel.getSessionStatus().setValue(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success && view.get() != null)
                Navigation.findNavController(view.get()).navigate(R.id.nav_charts);
        }
    }
}
