package com.example.runapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String FINE_LOC = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOC = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String[] permissions = {FINE_LOC, COARSE_LOC};
    private static final int LOC_PERM_REQUEST_CODE = 1234;


    private Boolean mLocPermissionGranted = false;

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
