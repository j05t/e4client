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
import java.util.List;
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

    public static float magnitude(int x, int y, int z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    static class UploadE4SessionToGoogleFit extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);

        @Override
        protected Boolean doInBackground(E4Session... e4Sessions) {
            final E4Session e4Session = e4Sessions[0];

            Log.d(MainActivity.TAG, "Session start: " + e4Session.getStartTime() + " duration: " + e4Session.getDuration()
                    + " end: " + (e4Session.getStartTime() + e4Session.getDuration()));

            // Create a session with metadata about the activity
            final Session session = new Session.Builder()
                    .setName("sample session")
                    .setDescription("Empatica E4 Session")
                    .setIdentifier(e4Session.getId())
                    .setActivity(FitnessActivities.SLEEP)
                    .setStartTime(e4Session.getStartTime(), TimeUnit.MILLISECONDS)
                    .setEndTime(e4Session.getStartTime() + e4Session.getDuration(), TimeUnit.MILLISECONDS)
                    .build();

            // HR datasource is defined by google already
            final DataSource hrDataSource =
                    new DataSource.Builder()
                            .setAppPackageName(MainActivity.context)
                            .setDataType(DataType.TYPE_HEART_RATE_BPM)
                            .setStreamName("Heart Rate")
                            .setType(DataSource.TYPE_RAW)
                            .build();
            uploadDataSet(viewModel.getSessionData().getHrTimestamps(), viewModel.getSessionData().getHr(), session, hrDataSource);

            // upload custom datatypes
            final String packageName = MainActivity.context.getPackageName();

            for (DataType dataType : MainActivity.dataTypes) {
                final DataSource.Builder dataSourceBuilder = new DataSource.Builder()
                        .setAppPackageName(MainActivity.context)
                        .setDataType(dataType)
                        .setType(DataSource.TYPE_RAW);

                if (dataType.getName().equals(packageName + ".eda")) {
                    dataSourceBuilder.setStreamName("Electodermal Activity");
                    uploadDataSet(viewModel.getSessionData().getGsrTimestamps(), viewModel.getSessionData().getGsr(), session, dataSourceBuilder.build());
                }
                if (dataType.getName().equals(packageName + ".temp")) {
                    dataSourceBuilder.setStreamName("Peripheral Skin Temperature");
                    uploadDataSet(viewModel.getSessionData().getTempTimestamps(), viewModel.getSessionData().getTemp(), session, dataSourceBuilder.build());
                }
            }

            return null;
        }

        private void uploadDataSet(List<Double> timestamps, List<Float> values, Session session, DataSource dataSource) {
            Log.i(MainActivity.TAG, "Inserting the session in the Sessions API: " + dataSource.getStreamName());

            DataSet.Builder dataSetBuilder = DataSet.builder(dataSource);

            for (int i = 0; i < values.size(); i++) {
                final float value = values.get(i);
                final double timestamp = timestamps.get(i);

                final DataPoint dataPoint = DataPoint.builder(dataSource)
                        // fixme: rounding double to closest long..
                        .setTimestamp(Math.round(timestamp), TimeUnit.MILLISECONDS)
                        .setFloatValues(value)
                        .build();

                dataSetBuilder.add(dataPoint);

                if (i % 1000 == 0) {
                    insertData(session, dataSetBuilder, String.format(Locale.getDefault(), "inserted %d/%d datapoints", i, values.size()));

                    dataSetBuilder = DataSet.builder(dataSource);
                }
            }

            insertData(session, dataSetBuilder, "done.");
        }

        private void insertData(final Session session, final DataSet.Builder dataSetBuilder, final String message) {

            // Build a session insert request
            final SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                    .setSession(session)
                    .addDataSet(dataSetBuilder.build())
                    .build();

            // Then, invoke the Sessions API to insert the session and await the result,
            // which is possible here because of the AsyncTask. Always include a timeout when
            // calling await() to avoid hanging that can occur from the service being shutdown
            // because of low memory or other conditions.
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

    public enum Action {
        NOP,
        VIEW_CHART,
        UPLOAD_TO_GOOGLE_FIT
    }

    // we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
    public static class LoadSessionData extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        final E4SessionData e4SessionData = viewModel.getSessionData();
        private E4Session e4Session;

        private Action action = Action.NOP;

        WeakReference<View> view;

        public LoadSessionData(View v, Action action) {
            view = new WeakReference<>(v);
            this.action = action;
        }

        @Override
        protected Boolean doInBackground(E4Session... e4Sessions) {
            e4Session = e4Sessions[0];

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

                    publishProgress("Processing temperature data");

                    data = new CSVFile(new FileInputStream(tempFile));
                    e4SessionData.setTempTimestamps(data.getX());
                    e4SessionData.setTemp(data.getY());
                    tempFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(bvpFile));
                    sessionData.setBvpTimestamps(data.getX());
                    sessionData.setBvp(data.getY());
                    bvpFile.delete();
                    */

                    publishProgress("Processing HR data");

                    data = new CSVFile(new FileInputStream(hrFile));
                    e4SessionData.setHrTimestamps(data.getX());
                    e4SessionData.setHr(data.getY());
                    hrFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(ibiFile));
                    sessionData.setIbiTimestamps(data.getX());
                    sessionData.setIbi(data.getY());
                    ibiFile.delete();
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
            if (success)
                switch (action) {
                    case VIEW_CHART:
                        if (view.get() != null)
                            Navigation.findNavController(view.get()).navigate(R.id.nav_charts);
                        break;
                    case UPLOAD_TO_GOOGLE_FIT:
                        new Utils.UploadE4SessionToGoogleFit().execute(e4Session);
                        break;
                }
        }
    }
}
