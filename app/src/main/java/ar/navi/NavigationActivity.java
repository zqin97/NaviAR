package ar.navi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;


public class NavigationActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG = "NavigationActivity";
    private static final String ROUTE_KEY = "mRoutePoints";
    private static final String USER_LOCATION_KEY = "mLastLocation";
    private static final int PERMISSIONS_REQUEST_ENABLE_CAMERA = 8001;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    // System sensor manager instance.
    private SensorManager mSensorManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;

    // System display. Need this for determining rotation.
    private Display mDisplay;

    // Current data from accelerometer & magnetometer.  The arrays hold values
    // for X, Y, and Z.
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];

    private boolean mRequestedARCoreInstall;
    private boolean mCameraPermission;
    private ArFragment fragment;

    // TextViews to display current sensor values.
    private TextView mTextNotification;
    private TextView mTextSensorAzimuth;
    //private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;

    private Runnable mRunnable;
    private Handler mHandler = new Handler();
    private LatLng mLastLocation;
    private ArrayList<LatLng> mRoutePoints;
    private Location deviceLocation;
    private Location startLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        mTextNotification = (TextView) findViewById(R.id.value_notification);
        mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        //mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);

        Bundle extras = getIntent().getExtras();
        mRoutePoints = extras.getParcelableArrayList(ROUTE_KEY);
        mLastLocation = extras.getParcelable(USER_LOCATION_KEY);

        if (mRoutePoints.size() > 0 && mLastLocation != null){
            deviceLocation = new Location("");
            deviceLocation.setLatitude(mLastLocation.latitude);
            deviceLocation.setLongitude(mLastLocation.longitude);

            startLocation = new Location("");
            startLocation.setLatitude(mRoutePoints.get(0).latitude);
            startLocation.setLongitude(mRoutePoints.get(0).longitude);
        }

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);

        // Get the display from the window manager (for rotation).
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        cameraVerify();
    }

    private void initCamera(){
        Log.d(TAG, "initCamera: initializing variables");

        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        //disable plane detection since we aren't concern about HitResult
        Session session = fragment.getArSceneView().getSession();
        Config newConfig = session.getConfig();
        newConfig.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        session.configure(newConfig);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = event.values.clone();
                break;
            default:
                return;
        }

        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null, mAccelerometerData, mMagnetometerData);
        //int mScreenRotation = mDisplay.getRotation();
        //aligning the view between device and user always uses x and z
        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Z;

        // Get the orientation of the device (x, y, z) based
        // on the rotation matrix. Output units are radians.
        float orientationValues[] = new float[3];
        float[] rotationMatrixModified = new float[9];
        boolean valid = SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, rotationMatrixModified);
        if (valid) {
            SensorManager.getOrientation(rotationMatrixModified,
                    orientationValues);

            for (int i = 0; i < 3; i++) {
                orientationValues[i] = (float) Math.toDegrees(orientationValues[i]);
            }
        }

        // Pull out the individual values from the array.
        float azimuth = orientationValues[0] >= 0 ? orientationValues[0]: orientationValues[0] + 360;
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        // Fill in the string placeholders and set the textview text.
        mTextSensorAzimuth.setText(getResources().getString(
                R.string.value_format, azimuth));
        //pitch might not be concerned in this project as we looking through the phone all time
        //mTextSensorPitch.setText(getResources().getString(R.string.value_format, pitch));
        mTextSensorRoll.setText(getResources().getString(
                R.string.value_format, roll));

        float bearing = deviceLocation.bearingTo(startLocation);
        bearing = bearing >= 0 ? bearing: bearing + 360;

        //check if device is facing the direction of the starting point
        if (azimuth > bearing - 30.0 && azimuth < bearing + 30.0) {
            //mTextNotification.setText(getResources().getString(R.string.label_align));
            float distance = deviceLocation.distanceTo(startLocation);

            mTextNotification.setText(getResources().getString(R.string.value_format, distance));
            //finding the best value to determine user was closed to the starting point
            if (distance < 3.0){
                //should remove the starting marker and show the route
            }

        } else{
            // calculate to determine the device should turn right or left
            //NOTED*****should be replace with animated arrow instead of TextView
            if (azimuth - bearing < 0){
                mTextNotification.setText(getResources().getString(R.string.label_right));
            } else{
                mTextNotification.setText(getResources().getString(R.string.label_left));
            }
        }
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        FusedLocationProviderClient mFusedLocationProviderClient;

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            final Task<Location> location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: found location!");
                        Location currentLocation = (Location) task.getResult();

                        if (currentLocation != null){
                            deviceLocation = new Location("");
                            deviceLocation.setLatitude(currentLocation.getLatitude());
                            deviceLocation.setLongitude(currentLocation.getLongitude());
                        }
                    }else{
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(NavigationActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void stopLocationUpdates(){
        mHandler.removeCallbacks(mRunnable);
    }

    private void startUserLocationsRunnable(){
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                getDeviceLocation();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void cameraVerify(){
        if (isCameraEnabled()){
            if (mCameraPermission){
                initCamera();
            }
        } else{
            getCameraPermission();
        }
    }

    public boolean isCameraEnabled() {
        final CameraManager manager = (CameraManager) getSystemService( Context.CAMERA_SERVICE );

        try{
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length <= 0 ) {
                buildAlertMessageNoCamera();
                return false;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return true;
    }

    private void buildAlertMessageNoCamera() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires Camera to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableCameraIntent = new Intent(Settings.ACTION_CAPTIONING_SETTINGS);
                        startActivityForResult(enableCameraIntent, PERMISSIONS_REQUEST_ENABLE_CAMERA);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void getCameraPermission() {
        /*
         * Request camera permission, so that we can get the camera of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraPermission = true;
            initCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_ENABLE_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mCameraPermission = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPermission = true;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUserLocationsRunnable();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
        stopLocationUpdates();
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}
