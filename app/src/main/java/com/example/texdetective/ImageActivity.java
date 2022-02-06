package com.example.texdetective;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.InputStream;

public class ImageActivity extends AppCompatActivity {

    private Uri currentImageUri = null;
    private String currentDetectedTextString = "";

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        Intent intent = getIntent();
        if (intent.hasExtra("imagePath")) {
            currentImageUri = Uri.parse(intent.getStringExtra("imagePath"));
        }
        if (intent.hasExtra("text")) {
            currentDetectedTextString = intent.getStringExtra("text");
        }

        TextView textView = findViewById(R.id.imageTextView);
        ImageView imageView = findViewById(R.id.imageView);
        textView.setText(currentDetectedTextString);
        try {
            if (intent.hasExtra("imagePath")) {
                InputStream inputStream = getContentResolver().openInputStream(currentImageUri);
                Drawable d = Drawable.createFromStream(inputStream, currentImageUri.toString());
                imageView.setImageDrawable(d);
            } else {
                Log.e(null, "onCreate: failed");
            }
            imageView.setAdjustViewBounds(true);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Image retrieve failed!", Toast.LENGTH_LONG).show();
        }
    }
}