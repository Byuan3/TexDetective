package com.example.texdetective;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.texdetective.historyholder.HistoryHolder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 */
public class HistoryListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_HISTORY_TEXT = "history-string";
    private static final String ARG_HISTORY_DATE = "history-date";
    private static final String ARG_HISTORY_URIS = "uris";

    private int mColumnCount = 0;
    private ArrayList<String> imageUris = new ArrayList<>();
    private final ArrayList<Bitmap> mHistoryBitmaps = new ArrayList<>();
    private ArrayList<String> mHistoryDate = new ArrayList<>();
    private ArrayList<String> imageDetectedHistory = new ArrayList<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryListFragment() {
    }

    @SuppressWarnings("unused")
    public static HistoryListFragment newInstance(int columnCount,
                                                  ArrayList<String> mHistoryDate,
                                                  ArrayList<String> imageDetectedHistory,
                                                  ArrayList<Uri> imageUris) {
        HistoryListFragment fragment = new HistoryListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putSerializable(ARG_HISTORY_TEXT, imageDetectedHistory);
        args.putSerializable(ARG_HISTORY_DATE, mHistoryDate);
        args.putSerializable(ARG_HISTORY_URIS, imageUris);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = requireArguments().getInt(ARG_COLUMN_COUNT);
            mHistoryDate = (ArrayList<String>) requireArguments().getSerializable(ARG_HISTORY_DATE);
            imageDetectedHistory = (ArrayList<String>) requireArguments().getSerializable(ARG_HISTORY_TEXT);
            imageUris = (ArrayList<String>) requireArguments().getSerializable(ARG_HISTORY_URIS);
        } else {
            Log.e(null, "Arguments is null");
        }

        Log.e(null, "Fragment: " + imageUris.toString());
        for (String uriString : imageUris) {
            try {
                Uri uri = Uri.parse(uriString);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                mHistoryBitmaps.add(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
                Log.e(null, "No Permission");
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            try {
                HistoryHolder historyHolder = new HistoryHolder(mHistoryBitmaps, mHistoryDate, mColumnCount, imageDetectedHistory, imageUris);
                recyclerView.setAdapter(new MyItemRecyclerViewAdapter(historyHolder.ITEM_LIST));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return view;
    }
}