package com.jstappdev.e4client;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
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
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;
import com.squareup.okhttp.OkHttpClient;

import java.lang.ref.WeakReference;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements EmpaStatusDelegate {
    public static final String TAG = "e4";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final String EMPATICA_API_KEY = BuildConfig.EMPATICA_API_KEY;
    private static final String SCICHART_LICENSE = BuildConfig.SCICHART_LICENSE;
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 42;

    private static boolean googleFitCustomDatatypesCreated = false;

    private EmpaDeviceManager deviceManager = null;
    private AppBarConfiguration mAppBarConfiguration;
    private SharedViewModel sharedViewModel;

    public static MainActivity context;

    public static final String PREFS_NAME = "preferences";
    public static final String PREF_UNAME = "Username";
    public static final String PREF_PASSWORD = "Password";
    public static final String PREFS_DATATYPES_CREATED = "datatypes_created";

    public static OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        sharedViewModel = ViewModelProviders.of(this).get(SharedViewModel.class);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);


        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home,
                R.id.nav_connection, R.id.nav_charts, R.id.nav_session,
                R.id.nav_settings, R.id.nav_share_csv, R.id.nav_sync)
                .setDrawerLayout(drawer)
                .build();
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
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
            okHttpClient.setConnectTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setReadTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setWriteTimeout(120, TimeUnit.SECONDS);
            okHttpClient.setCookieHandler(mCookieManager);
        }

        createGoogleFitClient();
    }

    private void loadPreferences() {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        googleFitCustomDatatypesCreated = settings.getBoolean(PREFS_DATATYPES_CREATED, false);

        sharedViewModel.setUsername(settings.getString(PREF_UNAME, ""));
        sharedViewModel.setPassword(settings.getString(PREF_PASSWORD, ""));
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
                            // without API key exit is the only way
                            finish();
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

            if (EMPATICA_API_KEY.contentEquals("INSERT API KEY HERE")) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("No API key set. Please insert your API KEY in apikeys.properties and rebuild the project.")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without API key exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            // Create a new EmpaDeviceManager. MainActivity is status delegate, SharedViewModel is data delegate
            deviceManager = new EmpaDeviceManager(getApplicationContext(), sharedViewModel, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (deviceManager != null) try {
            deviceManager.stopScanning();
        } catch (Exception e) {
            Log.e(TAG, "unable to stop scanning");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceManager != null) {
            deviceManager.cleanUp();
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
                sharedViewModel.setDeviceName("To: " + deviceName);

            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
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
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "got google fit permissions");
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
        sharedViewModel.setStatus(status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            sharedViewModel.setStatus(status.name() + " - Turn on your device");

            // Start scanning
            try {
                deviceManager.startScanning();
                // The device manager has established a connection
            } catch (NullPointerException e) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Unable to get device manager instance. Check your internet connection.")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without device manager exit is the only way
                                finish();
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

    void connectionDisconnected() {
        sharedViewModel.setIsConnected(false);
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

    public void disconnect() {
        if (deviceManager != null) {
            deviceManager.disconnect();
        }
    }


    public void createGoogleFitClient() {
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE);

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {

            new CreateCustomDataTypes(this).execute();
        }
    }

    private static class CreateCustomDataTypes extends AsyncTask<Void, Void, Void> {
        WeakReference<MainActivity> activity;

        CreateCustomDataTypes(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("e4", "createCustomDataTypes()");
            if (!googleFitCustomDatatypesCreated) {
                Log.d("e4", "creating custom datatypes");

                GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity.get());

                // todo: tags
                // the only predefined data type we can use is com.google.heart_rate.bpm
                for (String s : new String[]{"eda", "temp", "bvp", "ibi"}) {
                    final Task<DataType> response = Fitness.getConfigClient(activity.get(), googleSignInAccount)
                            .createCustomDataType(new DataTypeCreateRequest.Builder()
                                    .setName("com.jstappdev.e4client." + s)
                                    .addField(s.toUpperCase(), Field.FORMAT_FLOAT)
                                    .build());
                    try {
                        final DataType dataType = Tasks.await(response);

                        Log.d("e4", "created custom datatype " + dataType.getName());

                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final Task<DataType> response = Fitness.getConfigClient(activity.get(), googleSignInAccount)
                        .createCustomDataType(new DataTypeCreateRequest.Builder()
                                .setName("com.jstappdev.e4client.acc")
                                .addField("X", Field.FORMAT_INT32)
                                .addField("Y", Field.FORMAT_INT32)
                                .addField("Z", Field.FORMAT_INT32)
                                .build());
                try {
                    final DataType dataType = Tasks.await(response);

                    Log.d("e4", "created custom datatype " + dataType.getName());

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }


            return null;
        }
    }

}
