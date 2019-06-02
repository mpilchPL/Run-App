package com.example.runapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.auth.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DataActivity extends AppCompatActivity {

    private static final String TAG = "DataActivity";

    private FirebaseDatabase db;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference dbRef;
    private String userID;
    Run run;

    // WIDGETS
    private TextView timeVal, distVal, calVal;

    private ListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_data);

        listView = findViewById(R.id.listHistory);
        timeVal = findViewById(R.id.tv_timeVal);
        distVal = findViewById(R.id.tv_distVal);
        calVal = findViewById(R.id.tv_calVal);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
        FirebaseUser user = mAuth.getCurrentUser();
        userID = user.getUid();


        run = new Run();
        run.setCalories("0");
        run.setDistance("0");
        run.setRunId("0");


        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
            }
        };

        init();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                init();
                String item = (String) adapterView.getItemAtPosition(i);
                Log.d(TAG, "onItemClick: " + item);
                run.setRunId(item);
            }
        });


    }

    private void init() {
        dbRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                showData(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showData(DataSnapshot dataSnapshot) {
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            if (ds.getKey().equals(userID)) {
                UserInfo uInfo = new UserInfo();
                Log.d(TAG, "showData: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA  " + ds.child(run.getRunId()).getKey());
                uInfo.setRuns(ds.getValue(UserInfo.class).getRuns());
                ArrayList<String> array = new ArrayList<>();
                for (int i = 0; i < uInfo.getRuns(); i++) {
                    array.add(String.valueOf(ds.child(String.valueOf(i)).getKey()));
                }
                run.setDistance(ds.child(run.getRunId()).getValue(Run.class).getDistance());
                run.setTime(ds.child(run.getRunId()).getValue(Run.class).getTime());
                long dist = Long.valueOf(run.getDistance());
                run.setCalories(String.valueOf(dist* 0.062));


                ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, array);
                listView.setAdapter(adapter);

                setupInfo();
            }
        }
    }

    private void setupInfo() {
        Log.d(TAG, "setupInfo: 55555555555555");
        calVal.setText(run.getCalories());
        timeVal.setText(run.getTime());
        distVal.setText(run.getDistance());
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
}
