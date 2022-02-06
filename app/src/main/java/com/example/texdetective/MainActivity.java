package com.example.texdetective;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.texdetective.data.HistoryWrapper;
import com.example.texdetective.sqlite.FeedReaderContract;
import com.example.texdetective.sqlite.FeedReaderDbHelper;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.gu.toolargetool.TooLargeTool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    TextRecognizer recognizer;

    private String currentDetectedTextString = "";
    private Uri currentImageUri = null;
    private String currentIdString = "";
    private String currentDateString = "";
    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_HISTORY_TEXT = "history-string";
    private static final String ARG_HISTORY_DATE = "history-date";
    private static final String ARG_HISTORY_URIS = "uris";

    private ArrayList<String> imageDateHistory = new ArrayList<>();
    private ArrayList<String> imageDetectedHistory = new ArrayList<>();
    private ArrayList<String> imageUris = new ArrayList<>();
    private ArrayList<String> imageID = new ArrayList<>();
    private FeedReaderDbHelper dbHelper;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private MaterialToolbar toolbar;

    private ActivityResultLauncher<String> mGetContent;
    private ActivityResultLauncher<Intent> mTakePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new FeedReaderDbHelper(this);

        firebaseStorage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String title = "IMG_" + Calendar.getInstance().getTime().toString() + ".jpg";
                        String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + title;
                        File cameraFile = new File(filePath);
                        currentIdString = String.valueOf(Calendar.getInstance().getTimeInMillis());
                        currentDateString = Calendar.getInstance().getTime().toString();
                        currentImageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", cameraFile);
                        try {
                            InputStream is = getContentResolver().openInputStream(uri);
                            OutputStream os = getContentResolver().openOutputStream(currentImageUri);
                            FileUtils.copy(is, os);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        textDetection();
                    } else {
                        Toast.makeText(getApplicationContext(), "No Image Selected!", Toast.LENGTH_LONG).show();
                    }
                });

        mTakePicture = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        textDetection();
                    } else {
                        Toast.makeText(getApplicationContext(), "No Image Captured!", Toast.LENGTH_LONG).show();
                    }
                });

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("TexDetective                            En");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(R.array.add_photo, (dialogInterface, i) -> {
                if (i == 0) {
                    mGetContent.launch("image/*");
                } else if (i == 1) {
                    takeAPicture();
                } else if (i == 3) {
                    //readFromCloud();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.show();
        });


        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_HISTORY_DATE, imageDateHistory);
        bundle.putSerializable(ARG_HISTORY_TEXT, imageDetectedHistory);
        bundle.putSerializable(ARG_HISTORY_URIS, imageUris);
        bundle.putString(ARG_COLUMN_COUNT, String.valueOf(imageDateHistory.size()));

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragmentContainerView, HistoryListFragment.class, bundle)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            Log.e(null, "Current user: " + mAuth.getCurrentUser().getEmail());
            //readFromCloud();
        } else {
            //readFromSQLite();
            Log.e(null, "Current user: null");
        }

        //clearMemo();
        readFromSQLite();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //saveDetectionResultToSQLite();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.english) {
            toolbar.setTitle("TexDetective                            En");
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            return true;
        } else if (item.getItemId() == R.id.chinese) {
            toolbar.setTitle("TexDetective                            Cn");
            recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            return true;
        } else if (item.getItemId() == R.id.korean) {
            toolbar.setTitle("TexDetective                            Kr");
            recognizer =TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            return true;
        } else if (item.getItemId() == R.id.japanese) {
            toolbar.setTitle("TexDetective                            Jp");
            recognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            return true;
        }
        return false;
    }

    private void takeAPicture() {
        currentDateString = Calendar.getInstance().getTime().toString();
        currentIdString = String.valueOf(Calendar.getInstance().getTimeInMillis());
        String title = "IMG_" + currentDateString + ".jpg";
        String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + title;
        File cameraFile = new File(filePath);
        currentImageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", cameraFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
        mTakePicture.launch(intent);
    }

    private void textDetection() {
        InputImage image;
        try {
            image = InputImage.fromFilePath(getApplicationContext(), currentImageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.invalid_image_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    Toast.makeText(getApplicationContext(), R.string.image_process_success, Toast.LENGTH_LONG).show();
                    currentDetectedTextString = visionText.getText();
                    openImage();
                })
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), R.string.image_process_failed, Toast.LENGTH_LONG).show());
    }

    private void openImage() {
        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra("imagePath", currentImageUri.toString());
        intent.putExtra("text", currentDetectedTextString);
        startActivity(intent);
        addDetectionHistoryToMemo();
        saveDetectionResultToSQLite();
        if (mAuth.getCurrentUser() != null) {
            uploadFile(currentIdString, currentDateString, currentDetectedTextString, currentImageUri);
        }
        currentImageUri = null;
        currentDetectedTextString = "";
        currentDateString = "";
        currentIdString = "";
    }

    private void addDetectionHistoryToMemo() {
        try {
            imageDetectedHistory.add(currentDetectedTextString);
            imageDateHistory.add(currentDateString);
            imageUris.add(currentImageUri.toString());
            Log.e(null, "ImageURI added to MEMO");
            imageID.add(currentIdString);
        } catch (Exception e) {
            Log.e(null, "addDetectionHistoryToMemo: Can not add to memo");
        }
    }

    private void saveDetectionResultToSQLite() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.e(null, "Sizes: " + imageUris.size());
        String owner = "offline";
        if (mAuth.getCurrentUser() != null) {
            owner = mAuth.getCurrentUser().getUid();
        }
        for (int i = 0; i < imageUris.size(); i++) {
            ContentValues values = new ContentValues();
            Log.e(null, imageID.get(i) + " is saved");
            values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_ID, imageID.get(i));
            values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_OWNER, owner);
            values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_URI, imageUris.get(i));
            values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TEXT, imageDetectedHistory.get(i));
            values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_DATE, imageDateHistory.get(i));
            db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
            values.clear();
        }
    }

    private void readFromSQLite() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Log.e(null, "Read from sqlite");
        clearMemo();
        String[] projection = {
                //BaseColumns._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_OWNER,
                FeedReaderContract.FeedEntry.COLUMN_NAME_URI,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TEXT,
                FeedReaderContract.FeedEntry.COLUMN_NAME_DATE
        };

        String sortOrder = FeedReaderContract.FeedEntry.COLUMN_NAME_ID + " DESC";

        Cursor cursor;
        cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        if (cursor.isLast()) {
            Log.e(null, "Cursor is last");
        }

        while (cursor.moveToNext()) {
            //Long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
            String historyID = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_ID));
            String historyOwner = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_OWNER));
            String historyUri = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_URI));
            String historyText = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TEXT));
            String historyDate = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_DATE));
            Log.e(null, historyID + " is loaded");
            String owner;
            if (mAuth.getCurrentUser() != null) {
                owner = mAuth.getCurrentUser().getUid();
            } else {
                owner = "offline";
            }
            if (historyOwner.equals(owner)) {
                imageID.add(historyID);
                imageUris.add(historyUri);
                imageDetectedHistory.add(historyText);
                imageDateHistory.add(historyDate);
            }
        }

        Log.e(null, "Try to read");

        cursor.close();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_HISTORY_DATE, imageDateHistory);
        bundle.putSerializable(ARG_HISTORY_TEXT, imageDetectedHistory);
        bundle.putSerializable(ARG_HISTORY_URIS, imageUris);
        bundle.putString(ARG_COLUMN_COUNT, String.valueOf(imageDateHistory.size()));

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryListFragment.class, bundle)
                .commit();
    }

    private void clearMemo() {
        imageUris.clear();
        imageDetectedHistory.clear();
        imageDateHistory.clear();
        imageID.clear();
    }

    private void uploadFile(String idString, String dateString, String textString, Uri imageUri) {
        Map<String, Object> history = new HashMap<>();
        storageReference = firebaseStorage.getReference();
        StorageReference imageRef = storageReference.child("images/" + imageUri.getLastPathSegment());
        UploadTask uploadTask = imageRef.putFile(imageUri);
        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                history.put("history", new ArrayList<>());
                DocumentReference dr = db.collection("users").document(mAuth.getCurrentUser().getUid());
                dr.get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        DocumentSnapshot document = task1.getResult();
                        if (!document.exists()) {
                            dr.set(history, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.e(null, "DocumentSnapshot successfully written!");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(null, "Error writing document", e);
                                    });
                        }
                        dr.update("history", FieldValue.arrayUnion(new HistoryWrapper(idString, dateString, textString, downloadUri.toString())));
                    }
                });
            } else {
                // Handle failures
                // ...
            }
        });
    }

}