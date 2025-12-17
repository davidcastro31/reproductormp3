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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.reproductormp3.R;
import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.utils.MusicPlayer;
import com.example.reproductormp3.viewmodel.SongViewModel;

public class PlayerActivity extends AppCompatActivity {

    private ImageView albumArt;
    private TextView songTitle, artistName, currentTime, totalTime;
    private SeekBar seekBar;
    private ImageButton btnBack, btnFavorite, btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat, btnMore;

    private MusicPlayer musicPlayer;
    private SongViewModel songViewModel;
    private Song currentSong;
    private Handler handler = new Handler();
    private boolean isSeekBarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeViews();
        musicPlayer = MusicPlayer.getInstance();
        songViewModel = new ViewModelProvider(this).get(SongViewModel.class);

        currentSong = musicPlayer.getCurrentSong();

        if (currentSong != null) {
            displaySongInfo();
            setupControls();
            setupMusicPlayerListener();
            startSeekBarUpdate();
            updateAllButtons();
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

        // Siguiente
        btnNext.setOnClickListener(v -> {
            if (musicPlayer.hasNext() || musicPlayer.getRepeatMode() == MusicPlayer.RepeatMode.ALL) {
                musicPlayer.playNext();
            } else {
                Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
            }
        });

        // Anterior
        btnPrevious.setOnClickListener(v -> {
            if (musicPlayer.hasPrevious() || musicPlayer.getRepeatMode() == MusicPlayer.RepeatMode.ALL) {
                musicPlayer.playPrevious();
            } else {
                Toast.makeText(this, "Primera canción", Toast.LENGTH_SHORT).show();
            }
        });

        // Shuffle
        btnShuffle.setOnClickListener(v -> {
            musicPlayer.toggleShuffle();
            updateShuffleButton();
            String msg = musicPlayer.isShuffleEnabled() ? "🔀 Aleatorio activado" : "🔀 Aleatorio desactivado";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Repeat
        btnRepeat.setOnClickListener(v -> {
            musicPlayer.toggleRepeatMode();
            updateRepeatButton();
            String msg = getRepeatMessage();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Favorito
        btnFavorite.setOnClickListener(v -> {
            currentSong.setFavorite(!currentSong.isFavorite());
            songViewModel.toggleFavorite(currentSong.getId(), currentSong.isFavorite());
            updateFavoriteButton();
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

        // Más opciones
        btnMore.setOnClickListener(v ->
                Toast.makeText(this, "Más opciones (próximamente)", Toast.LENGTH_SHORT).show());

        updateAllButtons();
    }

    private void setupMusicPlayerListener() {
        musicPlayer.setOnPlayerStateChangeListener(new MusicPlayer.OnPlayerStateChangeListener() {
            @Override
            public void onPlaying(Song song) {
                currentSong = song;
                runOnUiThread(() -> {
                    displaySongInfo();
                    updatePlayPauseButton();
                });
            }

            @Override
            public void onPaused() {
                runOnUiThread(() -> updatePlayPauseButton());
            }

            @Override
            public void onStopped() {
                runOnUiThread(() -> updatePlayPauseButton());
            }

            @Override
            public void onCompletion() {
                runOnUiThread(() -> {
                    Toast.makeText(PlayerActivity.this, "Reproducción finalizada", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
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
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateAllButtons() {
        updatePlayPauseButton();
        updateShuffleButton();
        updateRepeatButton();
        updateFavoriteButton();
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(musicPlayer.isPlaying() ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void updateShuffleButton() {
        if (musicPlayer.isShuffleEnabled()) {
            btnShuffle.setColorFilter(getResources().getColor(R.color.dabri_primary));
        } else {
            btnShuffle.setColorFilter(getResources().getColor(R.color.dabri_text_secondary));
        }
    }

    private void updateRepeatButton() {
        switch (musicPlayer.getRepeatMode()) {
            case OFF:
                btnRepeat.setColorFilter(getResources().getColor(R.color.dabri_text_secondary));
                btnRepeat.setImageResource(android.R.drawable.ic_menu_rotate);
                break;
            case ONE:
                btnRepeat.setColorFilter(getResources().getColor(R.color.dabri_primary));
                btnRepeat.setImageResource(android.R.drawable.ic_menu_rotate);
                break;
            case ALL:
                btnRepeat.setColorFilter(getResources().getColor(R.color.dabri_primary));
                btnRepeat.setImageResource(android.R.drawable.ic_menu_rotate);
                break;
        }
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

    private String getRepeatMessage() {
        switch (musicPlayer.getRepeatMode()) {
            case OFF:
                return "🔁 Repetir desactivado";
            case ONE:
                return "🔂 Repetir una canción";
            case ALL:
                return "🔁 Repetir todas";
            default:
                return "";
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
