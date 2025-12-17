package com.example.reproductormp3.ui.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.reproductormp3.R;
import com.example.reproductormp3.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private Context context;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
        void onMenuClick(Song song, int position);
    }

    public SongAdapter(Context context, OnSongClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        // Título
        holder.tvSongTitle.setText(song.getDisplayTitle());

        // Artista
        holder.tvArtist.setText(song.getArtist());

        // Duración
        holder.tvDuration.setText(song.getFormattedDuration());

        // Carátula del álbum
        if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()) {
            Glide.with(context)
                    .load(Uri.parse(song.getAlbumArtUri()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .centerCrop()
                    .into(holder.imgAlbumArt);
        } else {
            holder.imgAlbumArt.setImageResource(R.drawable.ic_music_placeholder);
        }

        // Click en la canción
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song, position);
            }
        });

        // Click en el menú
        holder.btnMenu.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public Song getSongAt(int position) {
        return songs.get(position);
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAlbumArt;
        TextView tvSongTitle;
        TextView tvArtist;
        TextView tvDuration;
        ImageButton btnMenu;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumArt = itemView.findViewById(R.id.imgAlbumArt);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
