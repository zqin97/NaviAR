package ar.navi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;

import ar.navi.models.PlaceDetails;
import ar.navi.models.PolylineRoute;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnPolylineClickListener {

    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 15f;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(49.383639452689664, -17.39866406249996), new LatLng(59.53530451232491, 8.968523437500039));

    //widgets declare
    private AutoCompleteTextView searchText;
    private ImageView location;

    private GoogleMap mMap;
    private Marker mMarker;
    private Marker mSelectedMarker;
    private PolylineRoute mSelectedRoute;
    //private LatLngBounds locationBounds;
    private LatLng userLocation;
    private boolean mLocationPermission;
    private PlaceDetails mPlace;
    private GoogleApiClient mGoogleApiClient;
    private GeoApiContext mGeoApiContext;
    private PlaceAutocompleteAdapter mAutoCompleteAdapter;
    private ArrayList <PolylineRoute> mPolylineRoute = new ArrayList<>();

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        searchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        location = (ImageView) findViewById(R.id.ic_location);

        // Retrieving bundle values from main activity
        Intent intent = getIntent();
        Bundle bd = intent.getExtras();
        if(bd != null)
        {
            mLocationPermission = (boolean) bd.get("mLocationPermission");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_key))
                    .build();
        }

        getDeviceLocation(true);
    }

    private void initMapResource(){
        Log.d(TAG, "init: initializing variables");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        searchText.setOnItemClickListener(mAutocompleteClickListener);

        mAutoCompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);

        searchText.setAdapter(mAutoCompleteAdapter);

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: current location requested");
                getDeviceLocation(false);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map initialize complete", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        mMap.setOnPolylineClickListener(this);

        if (mLocationPermission){

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.setOnInfoWindowClickListener(this);

                initMapResource();
            }
        }
    }

    private void moveCamera(LatLng latLng, PlaceDetails placeInfo){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

        mMap.clear();

        mMap.setInfoWindowAdapter(new CustomWindowInterface(MapsActivity.this));

        if(placeInfo != null){
            try{
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + placeInfo.getWebsite() + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);
                mMarker = mMap.addMarker(options);
                mMarker.showInfoWindow();

            }catch (NullPointerException e){
                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage() );
            }
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }
    }

    private void getDeviceLocation(boolean state){
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        FusedLocationProviderClient mFusedLocationProviderClient;

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if (mLocationPermission){
                final Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            if (currentLocation != null){
                                userLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                                if (state){
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                                } else{
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                                }
                            }
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final String placeID;
            final AutocompletePrediction item = mAutoCompleteAdapter.getItem(i);

            if (item != null){
                placeID = item.getPlaceId();
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeID);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: query did not obtain any suitable result: " + places.getStatus().toString());
                //always release after done using
                places.release();
            }
            final Place place = places.get(0);

            try{
                mPlace = new PlaceDetails();
                mPlace.setName(place.getName().toString());
                mPlace.setAddress(place.getAddress().toString());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                mPlace.setId(place.getId());
                mPlace.setRating(place.getRating());
                mPlace.setLatLng(place.getLatLng());
                mPlace.setWebsite(place.getWebsiteUri());

                Log.d(TAG, "onResult: " + mPlace.toString());
            }catch (NullPointerException e){
                Log.e(TAG, "onResult: NullPointerException" + e.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), mPlace);

            places.release();
        }
    };

    private void calculateDirections(Marker marker){
        Log.d(TAG, "calculateDirections: retrieving directions");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );

        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);
        directions.alternatives(true);
        directions.origin(new com.google.maps.model.LatLng(
                userLocation.latitude,
                userLocation.longitude
        )).mode(TravelMode.WALKING).alternatives(true);

        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new com.google.maps.PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.d(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);
                // determine if we had already search for a route, removing old existing
                if (mPolylineRoute.size() > 0){
                    for (PolylineRoute polylineRoute : mPolylineRoute){
                        polylineRoute.getPolyLine().remove();
                    }
                    mPolylineRoute.clear();
                    mPolylineRoute = new ArrayList<>();
                }

                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(MapsActivity.this, R.color.colorDarkGrey));
                    polyline.setClickable(true);
//                    LatLng depature = new LatLng(route.bounds.southwest.lat, route.bounds.southwest.lng);
//                    LatLng destination = new LatLng(route.bounds.northeast.lat, route.bounds.northeast.lng);
//                    LatLngBounds bounds = LatLngBounds.builder().include(depature).include(destination).build();
                    mPolylineRoute.add(new PolylineRoute(polyline, route.legs[0]));

                    onPolylineClick(polyline);
                    mSelectedMarker.setVisible(false);
                }
            }
        });
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        int index = 0;
        for(PolylineRoute polylineRoute: mPolylineRoute){
            Log.d(TAG, "onPolylineClick: toString: " + polylineRoute.toString());
            index++;
            if(polyline.getId().equals(polylineRoute.getPolyLine().getId())){
                polylineRoute.getPolyLine().setColor(ContextCompat.getColor(MapsActivity.this, R.color.colorLightBlue));
                polylineRoute.getPolyLine().setZIndex(1);

                LatLng endLocation = new LatLng(polylineRoute.getLeg().endLocation.lat, polylineRoute.getLeg().endLocation.lng);
                LatLng cam = new LatLng((polylineRoute.getLeg().startLocation.lat + polylineRoute.getLeg().endLocation.lat) / 2,
                        (polylineRoute.getLeg().startLocation.lng + polylineRoute.getLeg().endLocation.lng) / 2);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Trip: #" + index)
                        .snippet(polylineRoute.getLegInfo()));

                marker.showInfoWindow();

                //List<LatLng> points = polyline.getPoints();
                mSelectedRoute = polylineRoute;
                //cam move can be improved
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cam, DEFAULT_ZOOM));
            }
            else{
                polylineRoute.getPolyLine().setColor(ContextCompat.getColor(MapsActivity.this, R.color.colorDarkGrey));
                polylineRoute.getPolyLine().setZIndex(0);
            }
        }
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (marker.getTitle().contains("Trip: #")){
            builder.setMessage("Navigate to " + marker.getTitle())
                    .setCancelable(true)
                    .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            Intent intentNavi = new Intent(MapsActivity.this, NavigationActivity.class);
                            List<LatLng> routes = mSelectedRoute.getPolyLine().getPoints();
                            ArrayList<LatLng> decodedPath= new ArrayList<>();

                            // This loops through all the LatLng coordinates of mSelectedRoute.
                            for(LatLng latLng: routes){
                                decodedPath.add(new LatLng(
                                        latLng.latitude,
                                        latLng.longitude
                                ));
                            }
                            Bundle extras = new Bundle();
                            extras.putParcelableArrayList("mRoutePoints", decodedPath);
                            extras.putParcelable("mLastLocation", userLocation);
                            intentNavi.putExtras(extras);
                            startActivity(intentNavi);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
        } else {
            builder.setMessage("Show Route to " + marker.getTitle())
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            mSelectedMarker = marker;
                            calculateDirections(mMarker);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
        }
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }
}
