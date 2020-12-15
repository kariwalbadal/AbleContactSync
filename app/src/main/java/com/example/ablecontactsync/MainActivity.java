package com.example.ablecontactsync;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String DB_KEY = "Users";
    private static final String DATE_KEY = "Date";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    FirebaseFirestore db;
    TextView info;
    TextView progress;
    Button syncContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();
        info = findViewById(R.id.info_text);
        progress = findViewById(R.id.progress_text);
        syncContacts = findViewById(R.id.button_contact_sync);

        syncContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateSyncOrRequestPermissions();
                progress.setText("Progress: ");
            }
        });
    }

    /**
     * Logic to sync contacts after getting it from Firebase Cloud Firestore DB
     */
    private void initiateContactSync() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy"); //sets the date format to query contacts from the database
        Date date = new Date();
        String dateString = dateFormat.format(date);
        db.collection(DB_KEY).whereEqualTo(DATE_KEY, dateString)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                        int l = task.getResult().size();
                        if (l == 0) {
                            Log.i(TAG, "Database is empty");
                            Toast.makeText(getApplicationContext(), "Database is empty. Sync completed", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int i = 0;
                        int percent = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) { //work on each contact obtained as result of the query
                            ContactUtils.addContact(getApplicationContext(), doc.getData());
                            i++;
                            percent = i * 100 / l;
                            int finalPercent = percent;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progress.setText("Progress: " + finalPercent + "%");  //updating the progress text on UI thread
                                }
                            });
                            Log.i(TAG, "Percentage of sync completed: " + finalPercent + "%");
                        }
                        Toast.makeText(getApplicationContext(), "Contacts Synced!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Check connection and try again", Toast.LENGTH_SHORT).show(); //in case network isn't available, the contacts need not be synced(cached data would contain contacts that have been stored already)
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error while fetching contacts");
                }
            }
        });
    }

    /**
     * Initiates contact sync, requests for permissions in case permissions are not granted
     */
    private void initiateSyncOrRequestPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {
            List<String> requiredPermissions = PermissionUtils.checkAndGetRequiredPermissions(this, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);

            if (requiredPermissions.size() == 0) {
                initiateContactSync();

            } else {
                requestPermissions(
                        requiredPermissions.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE
                );
            }
        } else {
            initiateContactSync();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {

            List<String> permissionsDenied = PermissionUtils.checkRequestPermissionsResult(permissions, grantResults);

            if (permissionsDenied.size() == 0) {
                initiateContactSync();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("All permissions are required. Please grant them from settings");
                builder.setPositiveButton("OK", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                builder.create().show();
            }
        }
    }
}