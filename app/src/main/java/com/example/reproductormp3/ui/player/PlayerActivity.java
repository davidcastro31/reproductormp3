package com.example.reproductormp3.ui.player;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.reproductormp3.R;
import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.utils.MusicPlayer;

public class PlayerActivity extends AppCompatActivity {

    private ImageView albumArt;
    private TextView songTitle, artistName, currentTime, totalTime;
    private SeekBar seekBar;
    private ImageButton btnBack, btnFavorite, btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat, btnMore;

    private MusicPlayer musicPlayer;
    private Song currentSong;
    private Handler handler = new Handler();
    private boolean isSeekBarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeViews();
        musicPlayer = MusicPlayer.getInstance();
        currentSong = musicPlayer.getCurrentSong();

        if (currentSong != null) {
            displaySongInfo();
            setupControls();
            startSeekBarUpdate();
        } else {
            Toast.makeText(this, "No hay canción reproduciéndose", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        albumArt = findViewById(R.id.albumArt);
        songTitle = findViewById(R.id.songTitle);
        artistName = findViewById(R.id.artistName);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.seekBar);

        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnMore = findViewById(R.id.btnMore);
    }

    private void displaySongInfo() {
        songTitle.setText(currentSong.getTitle());
        artistName.setText(currentSong.getArtist());
        totalTime.setText(currentSong.getFormattedDuration());

        // Carátula
        if (currentSong.getAlbumArtUri() != null && !currentSong.getAlbumArtUri().isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(currentSong.getAlbumArtUri()))
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .centerCrop()
                    .into(albumArt);
        } else {
            albumArt.setImageResource(R.drawable.ic_music_placeholder);
        }

        // SeekBar
        seekBar.setMax((int) currentSong.getDuration());

        // Favorito
        updateFavoriteButton();
    }

    private void setupControls() {
        // Botón volver
        btnBack.setOnClickListener(v -> finish());

        // Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            } else {
                musicPlayer.resume();
            }
            updatePlayPauseButton();
        });

        // Favorito
        btnFavorite.setOnClickListener(v -> {
            currentSong.setFavorite(!currentSong.isFavorite());
            updateFavoriteButton();
            // TODO: Actualizar en BD
            String msg = currentSong.isFavorite() ? "❤️ Agregado a favoritos" : "Removido de favoritos";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicPlayer.seekTo(seekBar.getProgress());
                isSeekBarTracking = false;
            }
        });

        // Botones que aún no funcionan
        btnPrevious.setOnClickListener(v ->
                Toast.makeText(this, "⏮️ Anterior (próximamente)", Toast.LENGTH_SHORT).show());

        btnNext.setOnClickListener(v ->
                Toast.makeText(this, "⏭️ Siguiente (próximamente)", Toast.LENGTH_SHORT).show());

        btnShuffle.setOnClickListener(v ->
                Toast.makeText(this, "🔀 Aleatorio (próximamente)", Toast.LENGTH_SHORT).show());

        btnRepeat.setOnClickListener(v ->
                Toast.makeText(this, "🔁 Repetir (próximamente)", Toast.LENGTH_SHORT).show());

        btnMore.setOnClickListener(v ->
                Toast.makeText(this, "Más opciones (próximamente)", Toast.LENGTH_SHORT).show());

        updatePlayPauseButton();
    }

    private void startSeekBarUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (musicPlayer.isPlaying() && !isSeekBarTracking) {
                    int current = musicPlayer.getCurrentPosition();
                    seekBar.setProgress(current);
                    currentTime.setText(formatTime(current));
                }
                handler.postDelayed(this, 1000); // Actualizar cada segundo
            }
        }, 1000);
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(musicPlayer.isPlaying() ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void updateFavoriteButton() {
        if (currentSong.isFavorite()) {
            btnFavorite.setImageResource(android.R.drawable.star_big_on);
            btnFavorite.setColorFilter(getResources().getColor(R.color.dabri_accent));
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_big_off);
            btnFavorite.setColorFilter(getResources().getColor(R.color.dabri_text_secondary));
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
