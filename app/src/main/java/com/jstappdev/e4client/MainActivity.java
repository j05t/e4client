package com.jstappdev.e4client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.util.Utils;
import com.squareup.okhttp.OkHttpClient;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements EmpaStatusDelegate {
    public static final String TAG = "e4";

    public static final String SESSION_NAME = "e4session";
    public static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 42;

    public static final String PREFS_NAME = "prefs";
    public static final String PREF_UNAME = "uname";
    public static final String PREF_PASSWORD = "pass";
    public static final String PREF_APIKEY = "apikey";
    public static final String PREF_FIRST_CONNECTED = "firstconnected";
    public static final String PREF_LAST_CONNECTED = "lastconnected";
    public static final String PREF_DATATYPES_CREATED = "types_created";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final String SCICHART_LICENSE = BuildConfig.SCICHART_LICENSE;
    private static final String[] customDataTypes = new String[]{"eda", "temp", "bvp", "ibi", "acc", "hrv"};

    public static ArrayList<DataType> dataTypes;
    public static OkHttpClient okHttpClient;
    public static FitnessOptions fitnessOptions;
    private static boolean googleFitCustomDatatypesCreated;
    private EmpaDeviceManager deviceManager;
    private AppBarConfiguration mAppBarConfiguration;
    private SharedViewModel sharedViewModel;
    private NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        sharedViewModel.setFilesDir(getFilesDir());

        setContentView(R.layout.activity_main);

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home,
                R.id.nav_connection, R.id.nav_charts, R.id.nav_session,
                R.id.nav_settings, R.id.nav_sync)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navigationView, navController);

        setUpSciChartLicense();

        loadPreferences();

        if (okHttpClient == null) {
            final CookieManager mCookieManager = new CookieManager();
            mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(mCookieManager);

            okHttpClient = new OkHttpClient();
            okHttpClient.setFollowRedirects(true);
            okHttpClient.setFollowSslRedirects(true);
            okHttpClient.setRetryOnConnectionFailure(true);
            // default timeout is 10 seconds
            //okHttpClient.setConnectTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setReadTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setWriteTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setCookieHandler(mCookieManager);
        }

        createGoogleFitClient();

        sharedViewModel.getCurrentStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });

        sharedViewModel.getTag().observe(this, new Observer<Double>() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onChanged(Double time) {
                final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Describe Event at " + Utils.getDateAsString(Math.round(time)));

                final EditText input = new EditText(getApplicationContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String description = input.getText().toString();
                        sharedViewModel.addTagDescription(time, description);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                final android.app.AlertDialog alert = builder.create();
                alert.show();

                // close dialog if user has not started to edit after one minute
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (alert.isShowing()) {
                            alert.dismiss();
                        }
                    }
                };

                alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        handler.removeCallbacks(runnable);
                    }
                });

                input.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        handler.removeCallbacks(runnable);
                        return false;
                    }
                });

                handler.postDelayed(runnable, 60 * 1000);
            }
        });
    }


    public void openFragment(final int fragmentId) {
        navController.navigate(fragmentId);
    }

    private void loadPreferences() {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        googleFitCustomDatatypesCreated = settings.getBoolean(PREF_DATATYPES_CREATED, false);

        sharedViewModel.setUsername(settings.getString(PREF_UNAME, ""));
        sharedViewModel.setPassword(settings.getString(PREF_PASSWORD, ""));
        sharedViewModel.setApiKey(settings.getString(PREF_APIKEY, ""));

        if (sharedViewModel.getUsername().length() == 0) {
            navController.navigate(R.id.nav_settings);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //noinspection ConstantConditions
        if (!sharedViewModel.getIsConnected().getValue())
            detectAndSavePreviousSession();
    }

    // recover previous session when app has been unexpectedly closed
    // todo: refactor
    private void detectAndSavePreviousSession() {
        final String basePath = getFilesDir() + "/";
        File edaFile = new File(basePath + "EDA.csv");

        if (edaFile.exists()) {
            File tempFile = new File(basePath + "TEMP.csv");
            File bvpFile = new File(basePath + "BVP.csv");
            File hrFile = new File(basePath + "HR.csv");
            File tagFile = new File(basePath + "tags.csv");
            File ibiFile = new File(basePath + "IBI.csv");
            File accFile = new File(basePath + "ACC.csv");
            File tagDescriptionFile = new File(basePath + "tags_description.csv");

            final SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            final long connected = settings.getLong(PREF_FIRST_CONNECTED, 0);
            final long disconnected = settings.getLong(PREF_LAST_CONNECTED, 0);

            final E4Session e4Session = new E4Session("e4c" + connected / 1000000, connected / 1000, disconnected / 1000 - connected / 1000, "000", "local", "E4", "0", "0");
            final File sessionFile = new File(getFilesDir(), e4Session.getZIPFilename());

            try {
                new ZipFile(sessionFile).addFiles(Arrays.asList(edaFile, tempFile, bvpFile, accFile, hrFile, ibiFile, tagFile, tagDescriptionFile));
                sharedViewModel.getCurrentStatus().postValue("Previous session detected, saved to local storage: " + sessionFile.getAbsolutePath());

                edaFile.delete();
                tempFile.delete();
                bvpFile.delete();
                accFile.delete();
                ibiFile.delete();
                hrFile.delete();
                tagFile.delete();
                tagDescriptionFile.delete();
            } catch (ZipException e) {
                sharedViewModel.getCurrentStatus().postValue("Error creating ZIP file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void setUpSciChartLicense() {
        try {
            com.scichart.charting.visuals.SciChartSurface.setRuntimeLicenseKey(SCICHART_LICENSE);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("Invalid SciChart license. Insert valid license in apikeys.properties and rebuild the project.")
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSION_ACCESS_COARSE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, yay!
                initEmpaticaDeviceManager();
            } else {
                // Permission denied, boo!
                final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                new AlertDialog.Builder(this)
                        .setTitle("Permission required")
                        .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // try again
                                if (needRationale) {
                                    // the "never ask again" flash is not set, try again with permission request
                                    initEmpaticaDeviceManager();
                                } else {
                                    // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            }
                        })
                        .setNeutralButton("Later", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                openFragment(R.id.nav_home);
                            }
                        })
                        .setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
            }
        }
    }

    public void initEmpaticaDeviceManager() {
        // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {

            if (sharedViewModel.getApiKey().isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("No API key set. Please insert your API key to be able to connect to your E4.")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                navController.navigate(R.id.nav_home);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                navController.navigate(R.id.nav_settings);
                            }
                        }).show();

                return;
            }

            // Create a new EmpaDeviceManager. MainActivity is status delegate, SharedViewModel is data delegate
            deviceManager = new EmpaDeviceManager(getApplicationContext(), sharedViewModel, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(sharedViewModel.getApiKey());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //noinspection ConstantConditions
        if (sharedViewModel.getIsConnected().getValue()) {
            final SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = settings.edit();
            editor.putLong(MainActivity.PREF_FIRST_CONNECTED, sharedViewModel.getTimeConnected());
            editor.putLong(MainActivity.PREF_LAST_CONNECTED, Utils.getCurrentTimestamp());
            editor.apply();

            sharedViewModel.flushFiles();
        }

        if (deviceManager != null) try {
            deviceManager.stopScanning();
        } catch (Exception e) {
            Log.e(TAG, "unable to stop scanning");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            Log.d(TAG, "detected App finishing, cleaning up..");
            disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (deviceManager != null)
            deviceManager.cleanUp();
    }

    public void disconnect() {
        if (deviceManager != null) {
            deviceManager.disconnect();
        }
        //noinspection ConstantConditions
        if (sharedViewModel.getIsConnected().getValue()) {
            sharedViewModel.setIsConnected(false);
            sharedViewModel.saveSession();
        }
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                sharedViewModel.setDeviceName(deviceName);

            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                sharedViewModel.getCurrentStatus().postValue("Sorry, you can't connect to this device");
            }
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user chose not to enable Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // You should deal with this
            return;
        }
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && !googleFitCustomDatatypesCreated) {
                Log.d(TAG, "got google fit permissions, creating custom datatypes");
                new CreateCustomDataTypes(this).execute();
            } else {
                Log.d(TAG, "Google Fit permission request denied, resultCode: " + resultCode);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {

        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        sharedViewModel.setSessionStatus(status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            sharedViewModel.setSessionStatus(status.name() + " - Turn on your device");

            // Start scanning
            try {
                deviceManager.startScanning();
                // The device manager has established a connection
            } catch (NullPointerException e) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Device manager is unable to download label file and may reject connecting to your wristband. Enable internet connection or try again.")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                openFragment(R.id.nav_home);
                            }
                        })
                        .show();
            }

            connectionDisconnected();

        } else if (status == EmpaStatus.CONNECTED) {

            connectionEstablished();
            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {
            sharedViewModel.setDeviceName("");

            connectionDisconnected();
        }
    }

    void connectionEstablished() {
        sharedViewModel.setIsConnected(true);
    }

    // may be called before connection is established
    void connectionDisconnected() {
        //noinspection ConstantConditions
        if (sharedViewModel.getIsConnected().getValue()) {
            sharedViewModel.setIsConnected(false);
            sharedViewModel.saveSession();
        }
    }

    @Override
    public void didEstablishConnection() {
        connectionEstablished();
    }

    @Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {
        if (status == EmpaSensorStatus.ON_WRIST) {

            sharedViewModel.setOnWrist(true);
        } else {

            sharedViewModel.setOnWrist(false);
        }

    }

    public void createGoogleFitClient() {
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION}, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE);

        final FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            readUploadedSessions();

            if (!googleFitCustomDatatypesCreated) {
                new CreateCustomDataTypes(this).execute();
                googleFitCustomDatatypesCreated = true;
            }
        }
    }

    private void readUploadedSessions() {

        Log.d(TAG, "reading sessions stored in Google Fit");

        // Set a start and end time for our query
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();

        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(1L, endTime, TimeUnit.MILLISECONDS).enableServerQueries()
                .readSessionsFromAllApps().setSessionName(MainActivity.SESSION_NAME)
                .build();

        Log.d(MainActivity.TAG, readRequest.toString());

        // Invoke the Sessions API to fetch the session with the query and wait for the result
        // of the read request. Note: Fitness.SessionsApi.readSession() requires the
        // ACCESS_FINE_LOCATION permission.
        Fitness.getSessionsClient(getApplicationContext(), Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(getApplicationContext())))
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {

                        final List<Session> sessions = sessionReadResponse.getSessions();

                        for (final Session session : sessions) {
                            if (!sharedViewModel.getUploadedSessionIDs().contains(session.getIdentifier())) {
                                sharedViewModel.getUploadedSessionIDs().add(session.getIdentifier());
                            }
                        }
                        Log.d(MainActivity.TAG, "Sessions already uploaded to Google Fit: " + Arrays.toString(sharedViewModel.getUploadedSessionIDs().toArray()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(MainActivity.TAG, "Failed to read session");
                    }
                });
    }


    private static class CreateCustomDataTypes extends AsyncTask<Void, String, Void> {
        WeakReference<MainActivity> activity;

        CreateCustomDataTypes(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            Log.d("e4", "creating custom datatypes");

            GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity.get());

            Log.d(MainActivity.TAG, "CreateCustomDataTypes() signed in as " +
                    googleSignInAccount.getDisplayName());


            dataTypes = new ArrayList<>();

            // the only predefined data type we can use is com.google.heart_rate.bpm
            for (final String s : customDataTypes) {
                try {
                    DataType dataType = Tasks.await(Fitness.getConfigClient(activity.get(), googleSignInAccount)
                            .createCustomDataType(new DataTypeCreateRequest.Builder()
                                    .setName("com.jstappdev.e4client." + s)
                                    .addField(s.toUpperCase(), Field.FORMAT_FLOAT)
                                    .build()));

                    dataTypes.add(dataType);
                    publishProgress("added custom datatype " + dataType);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            final FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();

            for (DataType dataType : dataTypes) {
                Log.d(TAG, "adding datatype to fitnessoptions: " + dataType.toString());
                fitnessOptionsBuilder.addDataType(dataType, FitnessOptions.ACCESS_WRITE);
            }

            fitnessOptionsBuilder.addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE);

            fitnessOptions = fitnessOptionsBuilder.build();

            Log.d(TAG, "created fitnessOptions with scopes");
            for (Scope s : fitnessOptions.getImpliedScopes()) {
                Log.d(TAG, s.toString());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values != null && values.length > 0)
                Log.d("e4", "created custom datatype " + values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // todo: save in sharedpreferences
            googleFitCustomDatatypesCreated = true;
        }
    }

}
