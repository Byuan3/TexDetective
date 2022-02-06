package com.example.texdetective;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.texdetective.sqlite.FeedReaderContract;
import com.example.texdetective.sqlite.FeedReaderDbHelper;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    private Button signOutButton;
    private Button syncButton;
    private TextView usernameTextView;
    private FeedReaderDbHelper dbHelper;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        signOutButton = findViewById(R.id.signout_button);
        usernameTextView = findViewById(R.id.username_textView);
        syncButton = findViewById(R.id.sync_button);
        dbHelper = new FeedReaderDbHelper(this);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();
            signInLauncher.launch(signInIntent);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            signOutButton.setVisibility(View.VISIBLE);
            syncButton.setVisibility(View.VISIBLE);
            usernameTextView.setText("Sign in as: " + user.getEmail());
            usernameTextView.setVisibility(View.VISIBLE);
        }

        signOutButton.setOnClickListener(v -> {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(AccountActivity.this, "Sign Out Complete.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        });

        syncButton.setOnClickListener(v -> {
            clearSqlite(mAuth.getCurrentUser().getUid());
            readFromCloud();
        });
    }

    private void clearSqlite(String owner) {
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_OWNER + " LIKE ?";
        String[] selectionArgs = {owner};
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    private void readFromCloud() {
        db = FirebaseFirestore.getInstance();
        String owner = mAuth.getCurrentUser().getUid();
        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        Log.e(null, "Read from cloud");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    ArrayList<HashMap<String, String>> data = (ArrayList<HashMap<String, String>>) documentSnapshot.get("history");
                    SQLiteDatabase sqLiteDatabase = dbHelper.getWritableDatabase();
                    for (int i = data.size() - 1; i >= 0; i--) {
                        HashMap<String, String> map = data.get(i);

                        String title = "IMG_" + map.get("date") + ".jpg";
                        String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + title;
                        File cameraFile = new File(filePath);
                        Uri imageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", cameraFile);

                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageReference = storage.getReferenceFromUrl(map.get("url"));
                        storageReference.getFile(cameraFile)
                                .addOnSuccessListener(taskSnapshot -> {
                                    ContentValues values = new ContentValues();
                                    Log.e(null, "Read from cloud: " + map.get("id"));
                                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_ID, map.get("id"));
                                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_OWNER, owner);
                                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_URI, imageUri.toString());
                                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TEXT, map.get("text"));
                                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_DATE, map.get("date"));
                                    sqLiteDatabase.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                                    values.clear();
                                })
                                .addOnFailureListener(e -> {

                                });
                    }
                } else {
                    Log.e(null, "No doc");
                }
            } else {
                Log.e(null, "Task Failed");
            }
        });
        Toast.makeText(AccountActivity.this, "Sync Complete.", Toast.LENGTH_SHORT).show();
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            signOutButton.setVisibility(View.VISIBLE);
            syncButton.setVisibility(View.VISIBLE);
            usernameTextView.setText("Sign in as: " + user.getEmail());
            usernameTextView.setVisibility(View.VISIBLE);
            // ...
        } else {
            Toast.makeText(AccountActivity.this, "Sign In Failed.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}


