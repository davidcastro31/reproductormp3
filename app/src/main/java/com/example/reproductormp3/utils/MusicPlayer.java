package com.example.reproductormp3.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.example.reproductormp3.models.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayer {

    private static final String TAG = "MusicPlayer";
    private static MusicPlayer instance;
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private OnPlayerStateChangeListener listener;

    // Cola de reproducción
    private List<Song> playlist = new ArrayList<>();
    private List<Song> originalPlaylist = new ArrayList<>();
    private int currentIndex = 0;

    // Modos de reproducción
    private boolean isShuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;

    public enum RepeatMode {
        OFF,    // No repetir
        ONE,    // Repetir una canción
        ALL     // Repetir toda la lista
    }

    public interface OnPlayerStateChangeListener {
        void onPlaying(Song song);
        void onPaused();
        void onStopped();
        void onCompletion();
        void onError(String error);
    }

    private MusicPlayer() {
        mediaPlayer = new MediaPlayer();
        setupMediaPlayer();
    }

    public static synchronized MusicPlayer getInstance() {
        if (instance == null) {
            instance = new MusicPlayer();
        }
        return instance;
    }

    private void setupMediaPlayer() {
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.i(TAG, "Canción completada");

            // Manejar según el modo de repetición
            if (repeatMode == RepeatMode.ONE) {
                // Repetir la misma canción
                playCurrentSong();
            } else if (hasNext()) {
                // Reproducir siguiente
                playNext();
            } else if (repeatMode == RepeatMode.ALL) {
                // Volver al inicio
                currentIndex = 0;
                playCurrentSong();
            } else {
                // Terminar reproducción
                if (listener != null) {
                    listener.onCompletion();
                }
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Error en MediaPlayer: " + what + ", " + extra);
            if (listener != null) {
                listener.onError("Error al reproducir");
            }
            return true;
        });
    }

    public void setOnPlayerStateChangeListener(OnPlayerStateChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Establece la lista de reproducción y reproduce una canción
     */
    public void playWithPlaylist(Context context, Song song, List<Song> playlist) {
        if (playlist == null || playlist.isEmpty()) {
            playSong(context, song);
            return;
        }

        // Guardar playlist
        this.originalPlaylist = new ArrayList<>(playlist);
        this.playlist = new ArrayList<>(playlist);

        // Encontrar la posición de la canción
        currentIndex = 0;
        for (int i = 0; i < this.playlist.size(); i++) {
            if (this.playlist.get(i).getId() == song.getId()) {
                currentIndex = i;
                break;
            }
        }

        // Si shuffle está activado, mezclar (pero mantener la canción actual primero)
        if (isShuffleEnabled) {
            applyShuffle();
        }

        playCurrentSong();
    }

    /**
     * Reproduce una canción individual (sin playlist)
     */
    public void playSong(Context context, Song song) {
        if (song == null || song.getPath() == null) {
            if (listener != null) {
                listener.onError("Canción inválida");
            }
            return;
        }

        try {
            // Si es la misma canción y está pausada, reanudar
            if (currentSong != null && currentSong.getId() == song.getId() && !isPlaying()) {
                resume();
                return;
            }

            // Detener la canción actual
            stop();

            // Configurar nueva canción
            currentSong = song;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            if (listener != null) {
                listener.onPlaying(song);
            }

            Log.i(TAG, "Reproduciendo: " + song.getTitle());

        } catch (IOException e) {
            Log.e(TAG, "Error al reproducir canción: " + e.getMessage());
            if (listener != null) {
                listener.onError("No se pudo reproducir la canción");
            }
        }
    }

    /**
     * Reproduce la canción actual de la playlist
     */
    private void playCurrentSong() {
        if (playlist.isEmpty() || currentIndex >= playlist.size()) {
            return;
        }

        Song song = playlist.get(currentIndex);

        try {
            stop();
            currentSong = song;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            if (listener != null) {
                listener.onPlaying(song);
            }

            Log.i(TAG, "Reproduciendo: " + song.getTitle());

        } catch (IOException e) {
            Log.e(TAG, "Error al reproducir: " + e.getMessage());
            if (listener != null) {
                listener.onError("No se pudo reproducir");
            }
        }
    }

    /**
     * Reproduce la siguiente canción
     */
    public void playNext() {
        if (playlist.isEmpty()) {
            return;
        }

        if (hasNext()) {
            currentIndex++;
            playCurrentSong();
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0;
            playCurrentSong();
        }
    }

    /**
     * Reproduce la canción anterior
     */
    public void playPrevious() {
        if (playlist.isEmpty()) {
            return;
        }

        // Si llevamos más de 3 segundos, reiniciar la canción actual
        if (getCurrentPosition() > 3000) {
            seekTo(0);
            return;
        }

        if (hasPrevious()) {
            currentIndex--;
            playCurrentSong();
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = playlist.size() - 1;
            playCurrentSong();
        }
    }

    /**
     * Activa/desactiva el modo aleatorio
     */
    public void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;

        if (isShuffleEnabled) {
            applyShuffle();
        } else {
            // Restaurar orden original
            playlist = new ArrayList<>(originalPlaylist);
            // Encontrar la posición actual en la lista original
            if (currentSong != null) {
                for (int i = 0; i < playlist.size(); i++) {
                    if (playlist.get(i).getId() == currentSong.getId()) {
                        currentIndex = i;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Aplica shuffle manteniendo la canción actual primero
     */
    private void applyShuffle() {
        if (playlist.isEmpty()) return;

        Song current = playlist.get(currentIndex);
        playlist.remove(currentIndex);
        Collections.shuffle(playlist);
        playlist.add(0, current);
        currentIndex = 0;
    }

    /**
     * Cambia el modo de repetición
     */
    public void toggleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = RepeatMode.ONE;
                break;
            case ONE:
                repeatMode = RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = RepeatMode.OFF;
                break;
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) {
                listener.onPaused();
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (listener != null && currentSong != null) {
                listener.onPlaying(currentSong);
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            if (listener != null) {
                listener.onStopped();
            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (Exception e) {
                Log.e(TAG, "Error al hacer seek: " + e.getMessage());
            }
        }
    }

    public boolean hasNext() {
        return !playlist.isEmpty() && currentIndex < playlist.size() - 1;
    }

    public boolean hasPrevious() {
        return !playlist.isEmpty() && currentIndex > 0;
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public List<Song> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentSong = null;
        playlist.clear();
        originalPlaylist.clear();
        instance = null;
    }
}