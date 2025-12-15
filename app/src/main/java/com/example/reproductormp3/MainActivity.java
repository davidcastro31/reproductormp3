package com.example.reproductormp3;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.ui.adapters.SongAdapter;
import com.example.reproductormp3.utils.MediaScanner;
import com.example.reproductormp3.utils.PermissionHelper;
import com.example.reproductormp3.viewmodel.SongViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private SongViewModel songViewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private LinearLayout emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar ViewModel
        songViewModel = new ViewModelProvider(this).get(SongViewModel.class);

        // Inicializar vistas
        recyclerView = findViewById(R.id.recyclerViewSongs);
        emptyView = findViewById(R.id.emptyView);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, this);
        recyclerView.setAdapter(adapter);

        // Verificar permisos
        if (PermissionHelper.hasStoragePermission(this)) {
            // Si tenemos permiso, escanear música
            scanAndLoadMusic();
        } else {
            // Si no, solicitar permiso
            PermissionHelper.requestStoragePermission(this);
        }

        // Observar cambios en las canciones
        observeSongs();
    }

    /**
     * Escanea y carga la música del dispositivo
     */
    private void scanAndLoadMusic() {
        Toast.makeText(this, "Escaneando música...", Toast.LENGTH_SHORT).show();

        // Ejecutar en segundo plano
        new Thread(() -> {
            MediaScanner scanner = new MediaScanner(this);
            List<Song> songs = scanner.scanMusicFiles();

            // Guardar en base de datos
            if (!songs.isEmpty()) {
                songViewModel.insertAll(songs);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Se encontraron " + songs.size() + " canciones",
                            Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "No se encontró música en el dispositivo",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Observa los cambios en la lista de canciones
     */
    private void observeSongs() {
        songViewModel.getAllSongs().observe(this, songs -> {
            if (songs != null && !songs.isEmpty()) {
                // Actualizar adapter
                adapter.setSongs(songs);
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                // Mostrar vista vacía
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Click en una canción
     */
    @Override
    public void onSongClick(Song song, int position) {
        Toast.makeText(this,
                "Reproduciendo: " + song.getTitle(),
                Toast.LENGTH_SHORT).show();

        // TODO: Reproducir canción
        // Aquí implementaremos el reproductor después
    }

    /**
     * Click en el menú de una canción
     */
    @Override
    public void onMenuClick(Song song, int position) {
        Toast.makeText(this,
                "Menú de: " + song.getTitle(),
                Toast.LENGTH_SHORT).show();

        // TODO: Mostrar menú con opciones
        // - Agregar a playlist
        // - Marcar como favorito
        // - Compartir
        // - Ver detalles
    }

    /**
     * Maneja la respuesta de solicitud de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
                scanAndLoadMusic();
            } else {
                // Permiso denegado
                Toast.makeText(this,
                        "Permiso denegado. No se puede acceder a la música",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}