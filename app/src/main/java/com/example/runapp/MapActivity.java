package com.example.runapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.runapp.utils.Keyboard;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final String FINE_LOC = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOC = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String[] permissions = {FINE_LOC, COARSE_LOC};
    private static final int LOC_PERM_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15;
    private static final float COUNTRY_ZOOM = 5;
    private static final String MY_LOCATION = "My location";
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int MAX_LOC_INTERVAL = 10000;          // Max location update interval (miliseconds)
    private static final int MIN_LOC_INTERVAL = 8000;          // Min location update interval (miliseconds)


    private String uID = "";
    //widgets
    private EditText mSearchText;
    private Button btnMyRuns, btnStart, btnStop;
    private ImageView mGps;
    private TextView caloriesTV, distanceTV;
    private Chronometer timeCM;
    //vars
    private Boolean mLocPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProvidedClient;
    private FirebaseUser currentUser;
    private Location mCurrentLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private List<Location> locationList = new ArrayList<>();
    private List<LatLng> coordList = new ArrayList<>();

    private Polyline route;
    private PolylineOptions routeOpt = new PolylineOptions();

    private float completeDistance = 0.0f;
    private float completeCalories = 0f;


    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_map);
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            uID = currentUser.getUid();
            Log.d(TAG, "onCreate: user  id" + uID);

        }
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
            }
        };


        getLocationPermissions();

    }

    private void init() {
        //<editor-fold desc="MAP WIDGETS LISTENERS">
        /*========================= MAP WIDGETS LISTENERS =============================*/
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocate();
                }
                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });

        /*========================== BUTTONS LISTENERS ===============================*/
        //</editor-fold>

        btnMyRuns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO DODAC ACTIVITY DO MYTRACES)
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
                startLocationUpdates();
                timeCM.setBase(SystemClock.elapsedRealtime());
                timeCM.start();

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
                Log.d(TAG, "onClick: LOCATION UPDATES ENDED, LOCATIONS: " + coordList.size());
                timeCM.stop();


                //TODO
                // ZAPISAC WYNIKI DO BAZY DANYCH
                // WYMYC TRASE
                // WYCZYSCIC LISTY
            }
        });




        /*=============================================================================*/
    }



    private void getLocationPermissions() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOC) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOC) == PackageManager.PERMISSION_GRANTED) {
                mLocPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOC_PERM_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOC_PERM_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocPermissionGranted = false;
        switch (requestCode) {
            case LOC_PERM_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocPermissionGranted = false;
                            return;
                        }
                    }
                    mLocPermissionGranted = true;
                    initMap();
                }
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();

            /* ====================== CREATING LAT_LNG, POLYLINE FOR TESTS ========================*/


            LatLng origin = new LatLng(37.420914, -122.085371);
            LatLng chpt1 = new LatLng(37.420786, -122.082910);
            LatLng chpt2 = new LatLng(37.416761, -122.082948);

            routeOpt = new PolylineOptions()
                    .add(origin)
                    .add(chpt1)
                    .add(chpt2);

            route = mMap.addPolyline(routeOpt);
            route.setColor(Color.BLUE);
            route.setVisible(true);



            /*============================================================================*/
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProvidedClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocPermissionGranted) {
                final Task location = mFusedLocationProvidedClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found loc");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, MY_LOCATION);
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());

        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving a camera to: " + latLng.latitude + " " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals(MY_LOCATION)) {
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(options);
        }
        Keyboard.hide(this);

    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(MAX_LOC_INTERVAL);
        locationRequest.setFastestInterval(MIN_LOC_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException" + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found location: " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), isAplace(address) ? DEFAULT_ZOOM : COUNTRY_ZOOM, address.getAddressLine(0));
        }
    }

    private boolean isAplace(Address address) {
        if (address.getPostalCode() != null)
            return true;
        return false;
    }

    public void clearSearchBar(View view) {
        mSearchText.setText("");
        Log.d(TAG, "say: cleared search bar");
    }

    public void search(View view) {
        if (mSearchText.length() > 0) {
            geoLocate();
            Log.d(TAG, "search: search btn clicked");
            Keyboard.hide(this);
        }

    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, LOC_PERM_REQUEST_CODE);
            return;
        }
        final Task locate = mFusedLocationProvidedClient.getLastLocation();
        locate.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    mCurrentLocation = (Location) task.getResult();
                }
            }
        });

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, LOC_PERM_REQUEST_CODE);
            return;
        }
        mFusedLocationProvidedClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        mFusedLocationProvidedClient.removeLocationUpdates(locationCallback);
    }


}




