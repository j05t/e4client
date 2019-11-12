package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.format.DateFormat;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.jstappdev.e4client.data.CSVFile;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.data.E4SessionData;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static boolean isUploading = false;

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

    static boolean isSessionDownloaded(final E4Session e4Session) {
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

    private static float magnitude(final int x, final int y, final int z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static class UploadE4SessionsToGoogleFit extends AsyncTask<List<E4Session>, String, Void> {

        private final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);

        @SafeVarargs
        @Override
        protected final Void doInBackground(final List<E4Session>... e4Sessions) {
            final List<E4Session> listOfSessions = e4Sessions[0];

            for (final E4Session e4Session : listOfSessions) {

                if (!Utils.isSessionDownloaded(e4Session)) {
                    publishProgress("Session data not downloaded.");
                    return null;
                }

                // Create a Google Fit session with metadata about the activity
                final Session fitSession = new Session.Builder()
                        .setName(MainActivity.SESSION_NAME)
                        .setDescription("empatica_e4_session")
                        .setIdentifier(e4Session.getId())
                        .setStartTime(e4Session.getStartTime(), TimeUnit.MILLISECONDS)
                        .setEndTime(e4Session.getStartTime() + e4Session.getDuration(), TimeUnit.MILLISECONDS)
                        .build();


                if (!viewModel.getUploadedSessionIDs().contains(e4Session.getId())) {
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

            String basePath = MainActivity.context.getCacheDir().getPath();

            Log.d(MainActivity.TAG, "extracting to directory " + basePath);
            try {
                final String packageName = MainActivity.context.getPackageName();

                final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

                trimCache(MainActivity.context);

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
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine());
                final double samplingRate = 1d / Double.parseDouble(reader.readLine());
                String line;
                int lineNumber = 0;
                int chunksProcessed = 0;
                while ((line = reader.readLine()) != null) {
                    final double timestamp = initialTime + samplingRate * lineNumber++;

                    xBuf[index] = timestamp;
                    yBuf[index] = Float.parseFloat(line);

                    // fixme
                    if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                        Log.e(MainActivity.TAG, "skipping datapoint beyond endtime with timestamp " + timestamp);
                        continue;
                    }

                    if (index < 999) {
                        index++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private synchronized void uploadAccFile(File file, DataSource dataSource, Session session) {
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
                final double samplingRate = 1d / Double.parseDouble(reader.readLine().split(",")[0]);
                String line;
                int lineNumber = 0;
                int chunksProcessed = 0;

                while ((line = reader.readLine()) != null) {
                    final String[] acc = line.split(",");
                    final int x = Integer.parseInt(acc[0]);
                    final int y = Integer.parseInt(acc[1]);
                    final int z = Integer.parseInt(acc[2]);
                    final float magnitude = magnitude(x, y, z);
                    final double timestamp = initialTime + samplingRate * lineNumber++;

                    // fixme
                    if (timestamp > session.getEndTime(TimeUnit.MILLISECONDS)) {
                        Log.e(MainActivity.TAG, "skipping ACC beyond endtime with timestamp " + timestamp);
                        continue;
                    }

                    xBuf[index] = timestamp;
                    yBuf[index] = magnitude;

                    if (index < 999) {
                        index++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private synchronized void uploadIbiFile(File file, DataSource dataSource, Session session) {
            double[] xBuf = new double[1000];
            float[] yBuf = new float[1000];
            int index = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final double initialTime = Double.parseDouble(reader.readLine().split(",")[0]);
                double timestamp;
                int chunksProcessed = 0;

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

                    xBuf[index] = timestamp;
                    yBuf[index] = ibi;

                    if (index < 999) {
                        index++;
                    } else {
                        uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);
                        index = 0;
                    }
                }
                uploadDataChunk(xBuf, yBuf, index, session, dataSource, ++chunksProcessed);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private synchronized void uploadDataChunk(double[] timestamps, float[] values, final int index, Session session, final DataSource dataSource, final int chunksProcessed) {

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

            @SuppressLint("DefaultLocale") final String message = String.format("Session %s: uploading %s chunk %d",
                    session.getIdentifier(), dataSource.getDataType().getName(), chunksProcessed);

            insertData(session, dataSetBuilder.build(), message);
        }

        private synchronized void insertData(final Session session, final DataSet dataSet, final String message) {
            // Build a session insert request
            final SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                    .setSession(session)
                    .addDataSet(dataSet)
                    .build();

            while (isUploading) {
                SystemClock.sleep(250);
            }

            isUploading = true;

            // invoke the Sessions API to insert the session and await the result,
            Fitness.getSessionsClient(MainActivity.context, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.context)))
                    .insertSession(insertRequest)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // At this point, the session has been inserted and can be read.
                            publishProgress(message);
                            Log.i(MainActivity.TAG, message + " successful.");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            publishProgress(String.format("There was a problem inserting session %s: %s",
                                    session.getIdentifier(), e.getLocalizedMessage()));
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    isUploading = false;
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
    static class LoadAndViewSessionData extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        final E4SessionData e4SessionData = viewModel.getSessionData();

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

                    trimCache(MainActivity.context);

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

                } catch (FileNotFoundException e) {
                    publishProgress("File not found.");
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

            if (values.length > 0) viewModel.getSessionStatus().setValue(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success)
                MainActivity.context.openCharts();
        }
    }


    public static class DownloadSessions extends AsyncTask<ArrayList<E4Session>, String, String> {

        private SharedViewModel sharedViewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        private SessionsAdapter adapter;

        public DownloadSessions(SessionsAdapter sessionsAdapter) {
            this.adapter = sessionsAdapter;
        }

        @SuppressLint("DefaultLocale")
        @SafeVarargs
        @Override
        protected final String doInBackground(ArrayList<E4Session>... listsOfSessions) {
            Log.d(MainActivity.TAG, "DownloadSessions.doInBackground()");

            final String url = "https://www.empatica.com/connect/download.php?id=";
            final int totalSessions = listsOfSessions[0].size();
            int downloadedSessions = 0;

            for (final E4Session e4Session : listsOfSessions[0]) {

                final String sessionId = e4Session.getId();
                final String filename = e4Session.getZIPFilename();

                if (Utils.isSessionDownloaded(e4Session)) {
                    Log.d(MainActivity.TAG, "session exists: " + e4Session);
                    publishProgress("File " + filename + " already downloaded.");
                    continue;
                }

                Log.d(MainActivity.TAG, "Downloading session " + e4Session);

                final Request request = new Request.Builder().url(url + sessionId).build();

                publishProgress(String.format("Downloading session %d/%d..", downloadedSessions++, totalSessions));

                MainActivity.okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        publishProgress("Download failed for " + filename);

                        Log.d(MainActivity.TAG, "Failed Download for " + filename + " " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final InputStream inputStream = response.body().byteStream();
                            final FileOutputStream out = MainActivity.context.openFileOutput(filename, Context.MODE_PRIVATE);

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = inputStream.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }

                            publishProgress("Downloaded " + filename);
                        } else {
                            Log.d(MainActivity.TAG, "unsuccessful download, redirect: " + response.isRedirect());
                            Log.d(MainActivity.TAG, response.toString());
                            Log.d(MainActivity.TAG, response.headers().toString());
                            if (response.body() != null)
                                Log.d(MainActivity.TAG, response.body().toString());
                        }

                        if (response.body() != null)
                            response.body().close();
                    }
                });
            }
            return "Downloads enqueued";
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values.length > 0)
                sharedViewModel.getSessionStatus().setValue(values[0]);

            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Collections.sort(sharedViewModel.getE4Sessions());

            adapter.notifyDataSetChanged();

            sharedViewModel.getSessionStatus().setValue(s);
        }

    }
}
