package com.jstappdev.e4client.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.E4Session;

import net.lingala.zip4j.ZipFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class UploadE4SessionsToGoogleFit extends AsyncTask<List<E4Session>, String, Void> {

    private final SharedViewModel viewModel;
    private final WeakReference<MainActivity> contextRef;

    public UploadE4SessionsToGoogleFit(Context context) {
        contextRef = new WeakReference<MainActivity>((MainActivity) context);
        viewModel = ViewModelProviders.of((MainActivity) context).get(SharedViewModel.class);
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(final List<E4Session>... e4Sessions) {
        final List<E4Session> listOfSessions = e4Sessions[0];

        for (final E4Session e4Session : listOfSessions) {

            if (!viewModel.isSessionDownloaded(e4Session)) {
                publishProgress("Session data not downloaded.");
                return null;
            }

            // Create a Google Fit session with metadata about the activity
            final Session fitSession = new Session.Builder()
                    .setName(MainActivity.SESSION_NAME)
                    .setDescription("empatica_e4_session")
                    .setIdentifier(e4Session.getId())
                    //.setActivity(FitnessActivities.SLEEP)
                    .setStartTime(e4Session.getStartTime(), TimeUnit.MILLISECONDS)
                    .setEndTime(e4Session.getStartTime() + e4Session.getDuration(), TimeUnit.MILLISECONDS)
                    .build();


            if (!viewModel.getUploadedSessionIDs().contains(e4Session.getId())) {

                // Build a session insert request
                final SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                        .setSession(fitSession)
                        .build();
                // invoke the Sessions API to insert the session and await the result,
                Fitness.getSessionsClient(contextRef.get(), Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(contextRef.get())))
                        .insertSession(insertRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(MainActivity.TAG, "created session: " + fitSession.toString());
                    }
                });

                Log.d(MainActivity.TAG, "uploading " + fitSession.toString());

                if (processFile(e4Session, fitSession)) {
                    viewModel.getUploadedSessionIDs().add(e4Session.getId());
                }
            }

            publishProgress(String.format("Session %s uploaded.", e4Session.getId()));
        }

        return null;
    }

    private synchronized boolean processFile(final E4Session e4Session, final Session fitSession) {

        Log.d(MainActivity.TAG, "reading " + e4Session.getZIPFilename());

        String basePath = contextRef.get().getCacheDir().getPath();

        Log.d(MainActivity.TAG, "extracting to directory " + basePath);
        try {
            final String packageName = contextRef.get().getPackageName();

            final File sessionFile = new File(contextRef.get().getFilesDir(), e4Session.getZIPFilename());

            Utils.trimCache(contextRef.get());

            new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

            basePath += File.separator;

            // same file format for EDA, HR, BVP, TEMP
            final File edaFile = new File(basePath + "EDA.csv");
            final File tempFile = new File(basePath + "TEMP.csv");
            //final File bvpFile = new File(basePath + "BVP.csv");
            final File hrFile = new File(basePath + "HR.csv");

            //final File tagFile = new File(basePath + "tags.csv");
            final File ibiFile = new File(basePath + "IBI.csv");
            final File accFile = new File(basePath + "ACC.csv");


            publishProgress(String.format("Session %s: uploading HR data", e4Session.getId()));
            // datatype for heart rate defined by google
            final DataSource hrDataSource =
                    new DataSource.Builder()
                            .setAppPackageName(packageName)
                            .setDataType(DataType.TYPE_HEART_RATE_BPM)
                            .setStreamName("heart_rate")
                            .setType(DataSource.TYPE_RAW)
                            .build();
            uploadFile(hrFile, hrDataSource, fitSession);


            for (final DataType dataType : MainActivity.dataTypes) {
                Log.d(MainActivity.TAG, "uploading datatype " + dataType.getName());

                final DataSource.Builder dataSourceBuilder = new DataSource.Builder()
                        .setAppPackageName(packageName)
                        .setDataType(dataType)
                        .setType(DataSource.TYPE_RAW);

                if (dataType.getName().equals(packageName + ".eda")) {
                    dataSourceBuilder.setStreamName("electrodermal_activity");
                    publishProgress(String.format("Session %s: uploading EDA data", e4Session.getId()));
                    uploadFile(edaFile, dataSourceBuilder.build(), fitSession);
                } else if (dataType.getName().equals(packageName + ".temp")) {
                    dataSourceBuilder.setStreamName("peripheral_skin_temperature");
                    publishProgress(String.format("Session %s: uploading TEMP data", e4Session.getId()));
                    uploadFile(tempFile, dataSourceBuilder.build(), fitSession);
                } /* BVP is used to extract HR and IBI, we don't upload it here
                else if (dataType.getName().equals(packageName + ".bvp")) {
                    dataSourceBuilder.setStreamName("blood_volume_pressure");
                    publishProgress(String.format("Session %s: uploading BVP data", e4Session.getId()));
                    uploadFile(bvpFile, dataSourceBuilder.build(), fitSession);
                } */ else if (dataType.getName().equals(packageName + ".ibi")) {
                    dataSourceBuilder.setStreamName("interbeat_interval");
                    publishProgress(String.format("Session %s: uploading IBI data", e4Session.getId()));
                    uploadIbiFile(ibiFile, dataSourceBuilder.build(), fitSession);
                } else if (dataType.getName().equals(packageName + ".acc")) {
                    dataSourceBuilder.setStreamName("acceleration");
                    publishProgress(String.format("Session %s: uploading ACC data", e4Session.getId()));
                    uploadAccFile(accFile, dataSourceBuilder.build(), fitSession);
                }
                /* tags may not be useful in Google Fit
                else if (dataType.getName().equals(packageName + ".tags")) {
                    dataSourceBuilder.setStreamName("tags");
                    publishProgress("Uploading tag data");
                    uploadTagFile(tagFile, dataSourceBuilder.build(), session);
                }
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private synchronized void uploadFile(final File file, final DataSource dataSource, final Session session) {

        Log.d(MainActivity.TAG, "uploading " + file.getName());

        double startTime = 0;
        int index = 0;

        final DataSet.Builder dataSetBuilder = DataSet.builder(dataSource);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final double initialTime = Double.parseDouble(reader.readLine());
            final double samplingRate = 1d / Double.parseDouble(reader.readLine());
            int lineNumber = 0;
            float sum = 0f;
            String line;

            while ((line = reader.readLine()) != null) {
                final double timestamp = initialTime + samplingRate * lineNumber++;

                if (startTime == 0) startTime = timestamp;

                // fixme
                if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                    Log.e(MainActivity.TAG, "skipping datapoint beyond endtime with timestamp " + timestamp);
                    continue;
                }

                sum += Float.parseFloat(line);
                index++;

                if (index > 999) {
                    insertMeanIntoDataSetBuilder(dataSetBuilder, dataSource, startTime, timestamp, sum, index);
                    sum = 0f;
                    index = 0;
                    startTime = 0;
                }
            }

            uploadDataSet(dataSetBuilder.build());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void uploadAccFile(File file, DataSource dataSource, Session session) {
        double startTime = 0;
        int index = 0;

        final DataSet.Builder dataSetBuilder = DataSet.builder(dataSource);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
            final double samplingRate = 1d / Double.parseDouble(reader.readLine().split(",")[0]);
            float sum = 0f;

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                final String[] acc = line.split(",");
                final int x = Integer.parseInt(acc[0]);
                final int y = Integer.parseInt(acc[1]);
                final int z = Integer.parseInt(acc[2]);
                final float magnitude = Utils.magnitude(x, y, z);
                final double timestamp = initialTime + samplingRate * lineNumber++;

                // fixme
                if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                    Log.e(MainActivity.TAG, "skipping ACC beyond endtime with timestamp " + timestamp);
                    continue;
                }

                sum += magnitude;
                index++;

                if (startTime == 0) startTime = timestamp;

                if (index > 999) {
                    insertMeanIntoDataSetBuilder(dataSetBuilder, dataSource, startTime, timestamp, sum, index);

                    sum = 0f;
                    index = 0;
                    startTime = 0;
                }
            }

            uploadDataSet(dataSetBuilder.build());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* IBI
Time between individuals heart beats extracted from the BVP signal.
No sample rate is needed for this file.
The first column is the time (respect to the initial time) of the detected inter-beat interval expressed in seconds (s).
The second column is the duration in seconds (s) of the detected inter-beat interval (i.e., the distance in seconds from the previous beat).
*/
    private synchronized void uploadIbiFile(File file, DataSource dataSource, Session session) {
        double startTime = 0;
        int index = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
            double timestamp = initialTime;
            float sum = 0f;

            final DataSet.Builder hrDataSetBuilder = DataSet.builder(dataSource);
            final List<Float> values = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                final String[] split = line.split(",");
                final double plusTime = Double.parseDouble(split[0]);
                final float ibi = Float.parseFloat(split[1]);

                timestamp = initialTime + plusTime;

                if (startTime == 0) startTime = timestamp;

                // fixme
                if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                    Log.e(MainActivity.TAG, "skipping IBI beyond endtime with timestamp " + timestamp);
                    continue;
                }

                values.add(ibi);
                sum += ibi;
                index++;

                if (index > 999) {
                    insertMeanIntoDataSetBuilder(hrDataSetBuilder, dataSource, startTime, timestamp, sum, index);

                    sum = 0f;
                    index = 0;
                    startTime = 0;
                }
            }

            uploadDataSet(hrDataSetBuilder.build());


            // heart rate variability
            // todo: this should be uploaded in session insert request if possible
            DataType hrvDataType = null;
            for (DataType dt : MainActivity.dataTypes) {
                if (dt.getName().equals(contextRef.get().getPackageName() + ".hrv"))
                    hrvDataType = dt;
            }
            if (hrvDataType != null) {
                final DataSource hrvDataSource =
                        new DataSource.Builder()
                                .setAppPackageName(contextRef.get().getPackageName())
                                .setDataType(hrvDataType)
                                .setStreamName("heart_rate_variability")
                                .setType(DataSource.TYPE_RAW)
                                .build();
                final DataSet.Builder hrvDataSetBuilder = DataSet.builder(hrvDataSource);

                final float hrv = Utils.calcHrvSDRR(values);

                // fixme: rounding double to closest long..
                hrvDataSetBuilder.add(DataPoint.builder(hrvDataSource)
                        .setTimeInterval(Math.round(initialTime), Math.round(timestamp), TimeUnit.MILLISECONDS)
                        .setFloatValues(hrv)
                        .build());
                Log.d(MainActivity.TAG, "total HRV for session from IBIS: " + hrv);

                uploadDataSet(hrvDataSetBuilder.build());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void insertMeanIntoDataSetBuilder(DataSet.Builder dataSetBuilder, DataSource dataSource, double startTime, double timestamp, float sum, int index) {
        final float mean = sum / index;
        // fixme: rounding double to closest long..
        dataSetBuilder.add(DataPoint.builder(dataSource)
                .setTimeInterval(Math.round(startTime), Math.round(timestamp), TimeUnit.MILLISECONDS)
                .setFloatValues(mean)
                .build());
        Log.d(MainActivity.TAG, dataSource.getDataType().getName() + " inserted average value: " + mean);
    }


    private synchronized void uploadDataSet(final DataSet dataSet) {

        if (dataSet.isEmpty()) return;

        while (Utils.isUploading) {
            SystemClock.sleep(250);
        }

        Utils.isUploading = true;

        Fitness.getHistoryClient(contextRef.get(),
                Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(contextRef.get())))
                .insertData(dataSet).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                publishProgress(dataSet.toString());
                Utils.isUploading = false;

                if (task.isSuccessful()) {
                    // At this point, the data has been inserted and can be read.
                    publishProgress("Data insert was successful for dataset" + dataSet.getDataType().getName());
                    Log.d(MainActivity.TAG, dataSet.getDataPoints().toString());
                } else {
                    publishProgress("There was a problem inserting the dataset " + dataSet.getDataType().getName());
                    Log.e(MainActivity.TAG, "There was a problem inserting the dataset.", task.getException());
                }
            }
        });

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length > 0) viewModel.getCurrentStatus().setValue(values[0]);
    }
}
