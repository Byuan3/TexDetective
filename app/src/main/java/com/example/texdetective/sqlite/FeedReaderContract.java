package com.example.texdetective.sqlite;

import android.provider.BaseColumns;

public class FeedReaderContract {
    private FeedReaderContract() {}

    public static class FeedEntry implements BaseColumns {

        public static final String TABLE_NAME = "history";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_URI = "uri";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_OWNER = "owner";
    }
}
