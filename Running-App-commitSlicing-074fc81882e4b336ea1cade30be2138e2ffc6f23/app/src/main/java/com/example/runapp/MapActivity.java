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
import com.google.android.gms.location.LocationResult;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    /*======= map vars =========*/
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

    // =====DATABASE========
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference();
    private String uID = "";
    private long userRuns = 0;


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
        createLocationRequest();
        createLocationCallback();

        //<editor-fold desc="buttons & widget init">
        mSearchText = findViewById(R.id.input_search);
        mGps = findViewById(R.id.ic_gps);
        btnMyRuns = findViewById(R.id.btn_run_history);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        distanceTV = findViewById(R.id.distance);
        caloriesTV = findViewById(R.id.calories);
        timeCM = findViewById(R.id.chronometer);
        timeCM.setBase(SystemClock.elapsedRealtime());
        //</editor-fold>

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() == null) {
                    createUser();
                }

                getUsersRuns(dataSnapshot);
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Object value = dataSnapshot.getValue();
                Log.d(TAG, "Value is: " + value);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
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

                if (currentUser == null) {
                    Toast.makeText(MapActivity.this, "Access for logged users only", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MapActivity.this, DataActivity.class);
                startActivity(intent);

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

                if (currentUser == null) {
                    return;
                } else {
                    dbRef.child(uID).child(String.valueOf(userRuns)).child("distance").setValue(completeDistance);
                    dbRef.child(uID).child(String.valueOf(userRuns)).child("time").setValue(completeCalories);
                    dbRef.child(uID).child("runs").setValue(userRuns + 1);
                    Log.d(TAG, "onClick: value set");

                }

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

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "onLocationResult: NO LOCATION RESULT");
                    return;
                }

                mCurrentLocation = locationResult.getLastLocation();
                Log.d(TAG, "onLocationResult: tick" + mCurrentLocation);
                locationList.add(mCurrentLocation);
                coordList.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                if (locationList.size() > 2) {

                    //CALCULATE DISTANCE
                    float distance = locationList.get(locationList.size() - 1).distanceTo
                            (locationList.get(locationList.size() - 2));
                    completeDistance += distance;
                    distanceTV.setText((int) completeDistance);
                    // CALCULATE CALORIES - 0.062 kcal for each meter
                    completeCalories = completeDistance * 0.062f;
                    caloriesTV.setText((int) completeCalories);

                    if (distance < 10) {
                        Log.d(TAG, "onLocationResult: DISTANCE TOO SHORT TO UPDATE ROUTES " + distance);
                    } else {
                        drawRoute();
                    }
                }
            }
        };
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

    private void drawRoute() {

        updatePolylineOptions();
        if (route != null) {
            route.remove();
        }
        route = mMap.addPolyline(routeOpt);
        route.setVisible(true);

    }

    private void updatePolylineOptions() {
        routeOpt = new PolylineOptions();

        for (LatLng latLng : coordList) {
            routeOpt.add(latLng);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void createUser() {
        String id = currentUser.getUid();
        Log.d(TAG, "createUser: ############################## ");
        dbRef.child(id).child("runs").setValue(0);
    }

    private void getUsersRuns(DataSnapshot dataSnapshot) {
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            if (ds.getKey().equals(currentUser.getUid())) {
                UserInfo userInfo = new UserInfo();
                userInfo.setRuns(ds.getValue(UserInfo.class).getRuns());
                userRuns = userInfo.getRuns();
                Log.d(TAG, "getUsersRuns: user ID: " + userInfo.getRuns());
            }

        }

    }

}




