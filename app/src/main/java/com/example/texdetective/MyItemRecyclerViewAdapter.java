package com.example.texdetective;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.texdetective.databinding.FragmentHistoryListBinding;
import com.example.texdetective.historyholder.HistoryHolder.HistoryItem;
import java.util.List;

public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private final List<HistoryItem> itemsList;

    public MyItemRecyclerViewAdapter(List<HistoryItem> itemsList) {
        this.itemsList = itemsList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentHistoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mDateTextView.setText(itemsList.get(position).dateString);
        holder.mImageView.setImageBitmap(itemsList.get(position).d);
        holder.mLinearLayout.setOnClickListener(view -> {
            openImage(view, itemsList.get(position).uri, itemsList.get(position).text);
        });
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    public void openImage(View view, String uriPath, String currentDetectedTextString) {
        Intent intent = new Intent(view.getContext(), ImageActivity.class);
        intent.putExtra("imagePath", uriPath);
        intent.putExtra("text", currentDetectedTextString);
        view.getContext().startActivity(intent);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mImageView;
        public final TextView mDateTextView;
        public final LinearLayout mLinearLayout;
        //public HistoryHolder.HistoryItem mHistoryItem;

        public ViewHolder(FragmentHistoryListBinding binding) {
            super(binding.getRoot());
            mImageView = binding.historyImageView;
            mDateTextView = binding.historyDateView;
            mLinearLayout = binding.historyItemLinearLayout;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDateTextView.getText() + "'";
        }
    }
}