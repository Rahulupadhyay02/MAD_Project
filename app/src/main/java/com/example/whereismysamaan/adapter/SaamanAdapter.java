package com.example.whereismysamaan.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.whereismysamaan.R;
import com.example.whereismysamaan.model.Saaman;

import java.util.ArrayList;
import java.util.List;

public class SaamanAdapter extends RecyclerView.Adapter<SaamanAdapter.SaamanViewHolder> {

    private List<Saaman> saamanList;
    private OnSaamanClickListener listener;

    public SaamanAdapter() {
        this.saamanList = new ArrayList<>();
    }

    public SaamanAdapter(List<Saaman> saamanList) {
        this.saamanList = saamanList != null ? saamanList : new ArrayList<>();
    }

    @NonNull
    @Override
    public SaamanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saaman, parent, false);
        return new SaamanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaamanViewHolder holder, int position) {
        Saaman saaman = saamanList.get(position);
        holder.bind(saaman);
    }

    @Override
    public int getItemCount() {
        return saamanList.size();
    }

    public void setSaamanList(List<Saaman> saamanList) {
        this.saamanList = saamanList != null ? saamanList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void clearItems() {
        if (this.saamanList != null) {
            this.saamanList.clear();
            notifyDataSetChanged();
        }
    }

    public void addSaaman(Saaman saaman) {
        if (saaman != null) {
            this.saamanList.add(saaman);
            notifyItemInserted(this.saamanList.size() - 1);
        }
    }

    public void setOnSaamanClickListener(OnSaamanClickListener listener) {
        this.listener = listener;
    }

    public interface OnSaamanClickListener {
        void onSaamanClick(Saaman saaman, int position);
    }

    class SaamanViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSaamanName;
        private TextView tvSaamanDescription;
        private ImageView ivSaamanType;

        public SaamanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSaamanName = itemView.findViewById(R.id.tv_saaman_name);
            tvSaamanDescription = itemView.findViewById(R.id.tv_saaman_description);
            ivSaamanType = itemView.findViewById(R.id.iv_saaman_type);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSaamanClick(saamanList.get(position), position);
                }
            });
        }

        public void bind(Saaman saaman) {
            tvSaamanName.setText(saaman.getName());
            tvSaamanDescription.setText(saaman.getDescription());
            
            // First try to load from base64, then fallback to URL if available
            if (saaman.getImageBase64() != null && !saaman.getImageBase64().isEmpty()) {
                try {
                    // Decode base64 string to bitmap
                    byte[] decodedBytes = android.util.Base64.decode(saaman.getImageBase64(), android.util.Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    
                    if (bitmap != null) {
                        ivSaamanType.setImageBitmap(bitmap);
                    } else {
                        // Set a default image if decoding fails
                        ivSaamanType.setImageResource(R.drawable.ic_track);
                    }
                } catch (Exception e) {
                    // Set a default image if there's an error
                    ivSaamanType.setImageResource(R.drawable.ic_track);
                }
            } else if (saaman.getImageUrl() != null && !saaman.getImageUrl().isEmpty()) {
                // Load image URL with Glide as fallback
                Glide.with(itemView.getContext())
                    .load(saaman.getImageUrl())
                    .placeholder(R.drawable.ic_track)
                    .error(R.drawable.ic_track)
                    .into(ivSaamanType);
            } else {
                // Set a default image if no image is available
                ivSaamanType.setImageResource(R.drawable.ic_track);
            }
        }
    }
} 