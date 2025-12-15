package com.example.reproductormp3.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.reproductormp3.database.AppDatabase;
import com.example.reproductormp3.database.SongDao;
import com.example.reproductormp3.models.Song;

import java.util.List;

public class SongRepository {

    private SongDao songDao;
    private LiveData<List<Song>> allSongs;

    public SongRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        songDao = database.songDao();
        allSongs = songDao.getAllSongs();
    }

    // ========== INSERTAR ==========
    public void insert(Song song) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.insert(song);
        });
    }

    public void insertAll(List<Song> songs) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.insertAll(songs);
        });
    }

    // ========== ACTUALIZAR ==========
    public void update(Song song) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.update(song);
        });
    }

    // ========== ELIMINAR ==========
    public void delete(Song song) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.delete(song);
        });
    }

    public void deleteAll() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.deleteAll();
        });
    }

    // ========== CONSULTAS ==========
    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }

    public LiveData<Song> getSongById(long songId) {
        return songDao.getSongById(songId);
    }

    public LiveData<List<Song>> searchSongs(String query) {
        return songDao.searchSongs(query);
    }

    public LiveData<List<Song>> getSongsByArtist(String artist) {
        return songDao.getSongsByArtist(artist);
    }

    public LiveData<List<Song>> getSongsByAlbum(String album) {
        return songDao.getSongsByAlbum(album);
    }

    public LiveData<List<Song>> getFavoriteSongs() {
        return songDao.getFavoriteSongs();
    }

    public LiveData<List<Song>> getMostPlayedSongs(int limit) {
        return songDao.getMostPlayedSongs(limit);
    }

    public LiveData<List<Song>> getRecentlyPlayedSongs(int limit) {
        return songDao.getRecentlyPlayedSongs(limit);
    }

    public LiveData<List<Song>> getRecentlyAddedSongs(int limit) {
        return songDao.getRecentlyAddedSongs(limit);
    }

    // ========== ACCIONES ==========
    public void toggleFavorite(long songId, boolean isFavorite) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.updateFavoriteStatus(songId, isFavorite);
        });
    }

    public void incrementPlayCount(long songId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            songDao.incrementPlayCount(songId, System.currentTimeMillis());
        });
    }

    // ========== ESTADÍSTICAS ==========
    public LiveData<Integer> getTotalSongsCount() {
        return songDao.getTotalSongsCount();
    }

    public LiveData<Long> getTotalDuration() {
        return songDao.getTotalDuration();
    }

    public LiveData<List<String>> getAllArtists() {
        return songDao.getAllArtists();
    }

    public LiveData<List<String>> getAllAlbums() {
        return songDao.getAllAlbums();
    }

    // ========== VERIFICACIÓN ==========
    public void checkIfSongExists(String path, OnSongExistsListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean exists = songDao.songExists(path);
            listener.onResult(exists);
        });
    }

    public interface OnSongExistsListener {
        void onResult(boolean exists);
    }
}
