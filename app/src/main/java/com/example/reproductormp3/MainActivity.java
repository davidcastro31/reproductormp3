package com.example.reproductormp3;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.ui.adapters.SongAdapter;
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
    private ImageButton btnFavorites; // Referencia al botón de favoritos

    private boolean hasScanned = false;
    private boolean showingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            // Inicializar ViewModel
            songViewModel = new ViewModelProvider(this).get(SongViewModel.class);

            // Inicializar vistas
            initializeViews();

            // Inicializar reproductor
            musicPlayer = MusicPlayer.getInstance();
            setupMusicPlayerListener();

            // Configurar RecyclerView
            setupRecyclerView();

            // Verificar permisos
            checkPermissions();

            // Observar cambios en las canciones
            observeSongs();

        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewSongs);
        emptyView = findViewById(R.id.emptyView);

        // Mini Player
        miniPlayer = findViewById(R.id.miniPlayer);
        miniPlayerAlbumArt = findViewById(R.id.miniPlayerAlbumArt);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnClose = findViewById(R.id.btnClose);

        setupMiniPlayerControls();

        // Toolbar y botones
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Botón de actualizar
        ImageButton btnRefresh = toolbar.findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> rescanMusic());
        }

        // Botón de favoritos
        btnFavorites = toolbar.findViewById(R.id.btnFavorites);
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
            // En lugar de escanear directamente, verificar si hay canciones primero
            checkIfNeedsScan();
        } else {
            PermissionHelper.requestStoragePermission(this);
        }
    }

    /**
     * Verifica si necesita escanear (solo si la BD está vacía)
     */
    private void checkIfNeedsScan() {
        // Crear un observer que se elimine después de ejecutarse una vez
        androidx.lifecycle.Observer<List<Song>> oneTimeObserver = new androidx.lifecycle.Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                // Remover inmediatamente después de recibir los datos
                songViewModel.getAllSongs().removeObserver(this);

                if (songs == null || songs.isEmpty()) {
                    // BD vacía, escanear
                    if (!hasScanned) {
                        scanAndLoadMusic();
                    }
                } else {
                    // Ya hay canciones, no escanear
                    hasScanned = true;
                    Toast.makeText(MainActivity.this, "✓ " + songs.size() + " canciones cargadas", Toast.LENGTH_SHORT).show();
                }
            }
        };

        songViewModel.getAllSongs().observe(this, oneTimeObserver);
    }

    private void scanAndLoadMusic() {
        if (hasScanned) return;
        hasScanned = true;

        new Handler().postDelayed(this::performScan, 500);
    }

    /**
     * Re-escanea la música para encontrar canciones nuevas
     */
    private void rescanMusic() {
        new AlertDialog.Builder(this)
                .setTitle("Actualizar biblioteca")
                .setMessage("¿Buscar canciones nuevas?\n\nEsto puede tardar unos segundos.")
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    Toast.makeText(this, "🔄 Buscando canciones nuevas...", Toast.LENGTH_SHORT).show();
                    performScan();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void performScan() {
        new Thread(() -> {
            try {
                MediaScanner scanner = new MediaScanner(this);
                List<Song> scannedSongs = scanner.scanMusicFiles();

                if (!scannedSongs.isEmpty()) {
                    runOnUiThread(() -> {
                        // Observer de una sola vez
                        androidx.lifecycle.Observer<List<Song>> oneTimeObserver = new androidx.lifecycle.Observer<List<Song>>() {
                            @Override
                            public void onChanged(List<Song> existingSongs) {
                                songViewModel.getAllSongs().removeObserver(this);

                                new Thread(() -> {
                                    try {
                                        int newCount = 0;

                                        if (existingSongs != null && !existingSongs.isEmpty()) {
                                            // Filtrar duplicados
                                            for (Song scannedSong : scannedSongs) {
                                                boolean exists = false;
                                                for (Song existingSong : existingSongs) {
                                                    if (scannedSong.getPath().equals(existingSong.getPath())) {
                                                        exists = true;
                                                        break;
                                                    }
                                                }
                                                if (!exists) {
                                                    songViewModel.insert(scannedSong);
                                                    newCount++;
                                                }
                                            }

                                            int finalNewCount = newCount;
                                            runOnUiThread(() -> {
                                                if (finalNewCount > 0) {
                                                    Toast.makeText(MainActivity.this,
                                                            "✓ " + finalNewCount + " canciones nuevas",
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this,
                                                            "✓ No hay canciones nuevas",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            // Base de datos vacía, insertar todas
                                            songViewModel.insertAll(scannedSongs);
                                            runOnUiThread(() -> {
                                                Toast.makeText(MainActivity.this,
                                                        "✓ " + scannedSongs.size() + " canciones encontradas",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(MainActivity.this,
                                                    "Error al procesar canciones",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        };

                        songViewModel.getAllSongs().observe(MainActivity.this, oneTimeObserver);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "No se encontró música en el dispositivo",
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Error al escanear: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void observeSongs() {
        songViewModel.getAllSongs().observe(this, songs -> {
            // Solo actualizar la UI si NO estamos en modo favoritos
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
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            } else {
                musicPlayer.resume();
            }
            updatePlayPauseButton();
        });

        btnClose.setOnClickListener(v -> {
            musicPlayer.stop();
            miniPlayer.setVisibility(View.GONE);
        });

        miniPlayer.setOnClickListener(v -> {
            Toast.makeText(this, "Pantalla completa (próximamente)", Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePlayPauseButton() {
        if (musicPlayer.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void setupMusicPlayerListener() {
        musicPlayer.setOnPlayerStateChangeListener(new MusicPlayer.OnPlayerStateChangeListener() {
            @Override
            public void onPlaying(Song song) {
                updatePlayPauseButton();
            }

            @Override
            public void onPaused() {
                updatePlayPauseButton();
            }

            @Override
            public void onStopped() {
                updatePlayPauseButton();
            }

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
        String[] options = {
                "Agregar a favoritos",
                "Agregar a lista",
                "Compartir",
                "Ver detalles",
                "Eliminar"
        };

        new AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            toggleFavorite(song);
                            break;
                        case 1:
                            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            showSongDetails(song);
                            break;
                        case 4:
                            confirmDelete(song);
                            break;
                    }
                })
                .show();
    }

    private void toggleFavorite(Song song) {
        boolean newStatus = !song.isFavorite();
        song.setFavorite(newStatus);
        songViewModel.toggleFavorite(song.getId(), newStatus);
        String message = newStatus ? "❤️ Agregado a favoritos" : "Eliminado de favoritos";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Alterna entre mostrar todas las canciones y solo favoritos
     */
    private void toggleFavoritesView() {
        if (showingFavorites) {
            // Volver a mostrar todas las canciones
            showingFavorites = false;
            updateFavoritesButtonColor();

            // Observer de una sola vez
            androidx.lifecycle.Observer<List<Song>> oneTimeObserver = new androidx.lifecycle.Observer<List<Song>>() {
                @Override
                public void onChanged(List<Song> allSongs) {
                    songViewModel.getAllSongs().removeObserver(this);
                    if (allSongs != null && !allSongs.isEmpty()) {
                        adapter.setSongs(allSongs);
                        Toast.makeText(MainActivity.this, "📚 Todas las canciones", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            songViewModel.getAllSongs().observe(this, oneTimeObserver);

        } else {
            // Mostrar solo favoritos
            showingFavorites = true;
            updateFavoritesButtonColor();

            // Observer de una sola vez
            androidx.lifecycle.Observer<List<Song>> oneTimeObserver = new androidx.lifecycle.Observer<List<Song>>() {
                @Override
                public void onChanged(List<Song> favoriteSongs) {
                    songViewModel.getFavoriteSongs().removeObserver(this);
                    if (favoriteSongs != null && !favoriteSongs.isEmpty()) {
                        adapter.setSongs(favoriteSongs);
                        Toast.makeText(MainActivity.this, "❤️ " + favoriteSongs.size() + " favoritos", Toast.LENGTH_SHORT).show();
                    } else {
                        showingFavorites = false;
                        updateFavoritesButtonColor();
                        Toast.makeText(MainActivity.this, "No tienes canciones favoritas aún", Toast.LENGTH_LONG).show();
                    }
                }
            };
            songViewModel.getFavoriteSongs().observe(this, oneTimeObserver);
        }
    }

    /**
     * Actualiza el color del botón de favoritos según el estado
     */
    private void updateFavoritesButtonColor() {
        if (btnFavorites != null) {
            if (showingFavorites) {
                // Cambiar a color primario (verde) cuando está activo
                btnFavorites.setColorFilter(getResources().getColor(R.color.dabri_primary));
            } else {
                // Color normal (accent - rojo) cuando no está activo
                btnFavorites.setColorFilter(getResources().getColor(R.color.dabri_accent));
            }
        }
    }

    private void showFavorites() {
        if (showingFavorites) {
            // Ya está en favoritos, volver a mostrar TODAS
            showingFavorites = false;
            songViewModel.getAllSongs().observe(this, allSongs -> {
                if (allSongs != null && !allSongs.isEmpty()) {
                    adapter.setSongs(allSongs);
                    Toast.makeText(this, "Mostrando todas las canciones", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Mostrar solo favoritos
            showingFavorites = true;
            songViewModel.getFavoriteSongs().observe(this, favoriteSongs -> {
                if (favoriteSongs != null && !favoriteSongs.isEmpty()) {
                    adapter.setSongs(favoriteSongs);
                    Toast.makeText(this, "❤️ " + favoriteSongs.size() + " favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    showingFavorites = false; // Volver al modo normal
                    Toast.makeText(this, "No tienes favoritos", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showSongDetails(Song song) {
        String details = "🎵 " + song.getTitle() + "\n\n" +
                "👤 Artista: " + song.getArtist() + "\n" +
                "💿 Álbum: " + song.getAlbum() + "\n" +
                "⏱️ Duración: " + song.getFormattedDuration();

        new AlertDialog.Builder(this)
                .setTitle("Detalles")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmDelete(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Eliminar \"" + song.getTitle() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    songViewModel.delete(song);
                    Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show();
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