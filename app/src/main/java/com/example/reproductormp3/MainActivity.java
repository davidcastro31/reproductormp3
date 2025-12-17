package com.example.reproductormp3;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.ui.adapters.SongAdapter;
import com.example.reproductormp3.ui.player.PlayerActivity;
import com.example.reproductormp3.utils.MediaScanner;
import com.example.reproductormp3.utils.MusicPlayer;
import com.example.reproductormp3.utils.PermissionHelper;
import com.example.reproductormp3.viewmodel.SongViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private SongViewModel songViewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private LinearLayout emptyView;
    private MusicPlayer musicPlayer;

    // Mini Player
    private CardView miniPlayer;
    private ImageView miniPlayerAlbumArt;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageButton btnPlayPause;
    private ImageButton btnClose;
    private ImageButton btnFavorites;

    private boolean hasScanned = false;
    private boolean showingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
            songViewModel = new ViewModelProvider(this).get(SongViewModel.class);

            initializeViews();
            musicPlayer = MusicPlayer.getInstance();
            setupMusicPlayerListener();
            setupRecyclerView();
            checkPermissions();
            observeSongs();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewSongs);
        emptyView = findViewById(R.id.emptyView);
        miniPlayer = findViewById(R.id.miniPlayer);
        miniPlayerAlbumArt = findViewById(R.id.miniPlayerAlbumArt);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnClose = findViewById(R.id.btnClose);

        setupMiniPlayerControls();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageButton btnRefresh = toolbar.findViewById(R.id.btnRefresh);
        btnFavorites = toolbar.findViewById(R.id.btnFavorites);

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> showRescanDialog());
        }
        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> toggleFavoritesView());
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    private void checkPermissions() {
        if (PermissionHelper.hasStoragePermission(this)) {
            checkIfNeedsScan();
        } else {
            PermissionHelper.requestStoragePermission(this);
        }
    }

    private void checkIfNeedsScan() {
        Observer<List<Song>> observer = new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                songViewModel.getAllSongs().removeObserver(this);
                if (songs == null || songs.isEmpty()) {
                    if (!hasScanned) scanAndLoadMusic();
                } else {
                    hasScanned = true;
                    Toast.makeText(MainActivity.this, "✓ " + songs.size() + " canciones", Toast.LENGTH_SHORT).show();
                }
            }
        };
        songViewModel.getAllSongs().observe(this, observer);
    }

    private void scanAndLoadMusic() {
        if (hasScanned) return;
        hasScanned = true;
        new Handler().postDelayed(this::performScan, 500);
    }

    private void showRescanDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Actualizar biblioteca")
                .setMessage("¿Buscar canciones nuevas?")
                .setPositiveButton("Actualizar", (d, w) -> {
                    Toast.makeText(this, "🔄 Buscando...", Toast.LENGTH_SHORT).show();
                    performScan();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void performScan() {
        new Thread(() -> {
            try {
                List<Song> scannedSongs = new MediaScanner(this).scanMusicFiles();
                if (scannedSongs.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No se encontró música", Toast.LENGTH_LONG).show());
                    return;
                }

                runOnUiThread(() -> {
                    Observer<List<Song>> observer = new Observer<List<Song>>() {
                        @Override
                        public void onChanged(List<Song> existing) {
                            songViewModel.getAllSongs().removeObserver(this);
                            new Thread(() -> {
                                int newCount = 0;
                                if (existing != null && !existing.isEmpty()) {
                                    for (Song scanned : scannedSongs) {
                                        boolean exists = false;
                                        for (Song ex : existing) {
                                            if (scanned.getPath().equals(ex.getPath())) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            songViewModel.insert(scanned);
                                            newCount++;
                                        }
                                    }
                                    int count = newCount;
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                            count > 0 ? "✓ " + count + " nuevas" : "✓ Sin nuevas",
                                            Toast.LENGTH_SHORT).show());
                                } else {
                                    songViewModel.insertAll(scannedSongs);
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                            "✓ " + scannedSongs.size() + " encontradas", Toast.LENGTH_SHORT).show());
                                }
                            }).start();
                        }
                    };
                    songViewModel.getAllSongs().observe(MainActivity.this, observer);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void observeSongs() {
        songViewModel.getAllSongs().observe(this, songs -> {
            if (!showingFavorites) {
                if (songs != null && !songs.isEmpty()) {
                    adapter.setSongs(songs);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onSongClick(Song song, int position) {
        musicPlayer.playSong(this, song);
        showMiniPlayer(song);
        songViewModel.incrementPlayCount(song.getId());
    }

    private void showMiniPlayer(Song song) {
        miniPlayer.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(song.getTitle());
        miniPlayerArtist.setText(song.getArtist());

        if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(song.getAlbumArtUri()))
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .centerCrop()
                    .into(miniPlayerAlbumArt);
        } else {
            miniPlayerAlbumArt.setImageResource(R.drawable.ic_music_placeholder);
        }
        updatePlayPauseButton();
    }

    private void setupMiniPlayerControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) musicPlayer.pause();
            else musicPlayer.resume();
            updatePlayPauseButton();
        });

        btnClose.setOnClickListener(v -> {
            musicPlayer.stop();
            miniPlayer.setVisibility(View.GONE);
        });

        miniPlayer.setOnClickListener(v -> {
            // Abrir pantalla de reproductor completo
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            startActivity(intent);
        });
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(musicPlayer.isPlaying() ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void setupMusicPlayerListener() {
        musicPlayer.setOnPlayerStateChangeListener(new MusicPlayer.OnPlayerStateChangeListener() {
            @Override
            public void onPlaying(Song song) { updatePlayPauseButton(); }
            @Override
            public void onPaused() { updatePlayPauseButton(); }
            @Override
            public void onStopped() { updatePlayPauseButton(); }
            @Override
            public void onCompletion() {
                Toast.makeText(MainActivity.this, "Canción finalizada", Toast.LENGTH_SHORT).show();
                miniPlayer.setVisibility(View.GONE);
            }
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMenuClick(Song song, int position) {
        String favoriteText = song.isFavorite() ? "Quitar de favoritos" : "Agregar a favoritos";
        String[] options = {favoriteText, "Agregar a lista", "Compartir", "Ver detalles", "Eliminar"};

        new AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setItems(options, (d, w) -> {
                    switch (w) {
                        case 0: toggleFavorite(song); break;
                        case 1: Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show(); break;
                        case 3: showSongDetails(song); break;
                        case 4: confirmDelete(song); break;
                    }
                })
                .show();
    }

    private void toggleFavorite(Song song) {
        boolean newStatus = !song.isFavorite();
        song.setFavorite(newStatus);
        songViewModel.toggleFavorite(song.getId(), newStatus);

        String msg = newStatus ? "❤️ Agregado a favoritos" : "Removido de favoritos";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        if (showingFavorites && !newStatus) {
            new Handler().postDelayed(this::refreshFavoritesView, 300);
        }
    }

    private void toggleFavoritesView() {
        showingFavorites = !showingFavorites;
        updateFavoritesButtonColor();

        if (showingFavorites) {
            Observer<List<Song>> observer = new Observer<List<Song>>() {
                @Override
                public void onChanged(List<Song> favs) {
                    songViewModel.getFavoriteSongs().removeObserver(this);
                    if (favs != null && !favs.isEmpty()) {
                        adapter.setSongs(favs);
                        Toast.makeText(MainActivity.this, "❤️ " + favs.size() + " favoritos", Toast.LENGTH_SHORT).show();
                    } else {
                        showingFavorites = false;
                        updateFavoritesButtonColor();
                        Toast.makeText(MainActivity.this, "No hay favoritos", Toast.LENGTH_LONG).show();
                    }
                }
            };
            songViewModel.getFavoriteSongs().observe(this, observer);
        } else {
            Observer<List<Song>> observer = new Observer<List<Song>>() {
                @Override
                public void onChanged(List<Song> all) {
                    songViewModel.getAllSongs().removeObserver(this);
                    if (all != null && !all.isEmpty()) {
                        adapter.setSongs(all);
                        Toast.makeText(MainActivity.this, "📚 Todas las canciones", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            songViewModel.getAllSongs().observe(this, observer);
        }
    }

    private void refreshFavoritesView() {
        Observer<List<Song>> observer = new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> favs) {
                songViewModel.getFavoriteSongs().removeObserver(this);
                if (favs != null && !favs.isEmpty()) {
                    adapter.setSongs(favs);
                } else {
                    showingFavorites = false;
                    updateFavoritesButtonColor();
                    observeSongs();
                }
            }
        };
        songViewModel.getFavoriteSongs().observe(this, observer);
    }

    private void updateFavoritesButtonColor() {
        if (btnFavorites != null) {
            btnFavorites.setColorFilter(getResources().getColor(
                    showingFavorites ? R.color.dabri_primary : R.color.dabri_accent));
        }
    }

    private void showSongDetails(Song song) {
        String details = "🎵 " + song.getTitle() + "\n\n" +
                "👤 " + song.getArtist() + "\n" +
                "💿 " + song.getAlbum() + "\n" +
                "⏱️ " + song.getFormattedDuration();

        new AlertDialog.Builder(this)
                .setTitle("Detalles")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmDelete(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Eliminar \"" + song.getTitle() + "\"?\n\nSolo se elimina de Dabri Music.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    boolean wasFavorite = song.isFavorite();
                    songViewModel.delete(song);

                    // Mensaje según si era favorita o no
                    if (wasFavorite) {
                        Toast.makeText(this, "🗑️ Eliminada (también de favoritos)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "🗑️ Eliminada", Toast.LENGTH_SHORT).show();
                    }

                    // Si estamos viendo favoritos, actualizar la vista
                    if (showingFavorites) {
                        new Handler().postDelayed(this::refreshFavoritesView, 300);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
                scanAndLoadMusic();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
    }
}