package com.example.reproductormp3.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.example.reproductormp3.models.Song;

import java.io.IOException;

public class MusicPlayer {

    private static final String TAG = "MusicPlayer";
    private static MusicPlayer instance;
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private OnPlayerStateChangeListener listener;

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
            if (listener != null) {
                listener.onCompletion();
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
     * Reproduce una canción
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
     * Pausa la reproducción
     */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) {
                listener.onPaused();
            }
        }
    }

    /**
     * Reanuda la reproducción
     */
    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (listener != null && currentSong != null) {
                listener.onPlaying(currentSong);
            }
        }
    }

    /**
     * Detiene la reproducción
     */
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

    /**
     * Verifica si está reproduciendo
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * Obtiene la canción actual
     */
    public Song getCurrentSong() {
        return currentSong;
    }

    /**
     * Obtiene la duración total
     */
    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    /**
     * Obtiene la posición actual
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * Salta a una posición
     */
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * Libera recursos
     */
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentSong = null;
        instance = null;
    }
}
