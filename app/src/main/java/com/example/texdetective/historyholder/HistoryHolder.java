package com.example.texdetective.historyholder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HistoryHolder {

    public final ArrayList<HistoryItem> ITEM_LIST = new ArrayList<>();

    public HistoryHolder(ArrayList<Bitmap> bitmaps,
                         ArrayList<String> dates,
                         int id,
                         ArrayList<String> detectedText,
                         ArrayList<String> uriPaths) {
        for (int i = 0; i < bitmaps.size(); i++) {
            HistoryItem historyItem = new HistoryItem(bitmaps.get(i), dates.get(i), String.valueOf(id), detectedText.get(i), uriPaths.get(i));
            ITEM_LIST.add(historyItem);
        }
    }

    public static class HistoryItem {
        public final String dateString;
        public final String id;
        public final Bitmap d;
        public final String text;
        public final String uri;
        public HistoryItem(Bitmap d, String dateString, String id, String text, String uri) {
            this.id = id;
            this.dateString = dateString;
            this.d = d;
            this.text = text;
            this.uri = uri;
        }

        @NonNull
        @Override
        public String toString() {
            return  "ImageID : " + id + " Date: " + dateString;
        }
    }
}
