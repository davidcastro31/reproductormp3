package com.example.reproductormp3.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.example.reproductormp3.models.Song;
import com.example.reproductormp3.repository.SongRepository;

import java.util.List;

public class SongViewModel extends AndroidViewModel {

    private SongRepository repository;
    private LiveData<List<Song>> allSongs;

    public SongViewModel(@NonNull Application application) {
        super(application);
        repository = new SongRepository(application);
        allSongs = repository.getAllSongs();
    }

    // ========== INSERTAR ==========
    public void insert(Song song) {
        repository.insert(song);
    }

    public void insertAll(List<Song> songs) {
        repository.insertAll(songs);
    }

    // ========== ACTUALIZAR ==========
    public void update(Song song) {
        repository.update(song);
    }

    // ========== ELIMINAR ==========
    public void delete(Song song) {
        repository.delete(song);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    // ========== CONSULTAS ==========
    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }

    public LiveData<Song> getSongById(long songId) {
        return repository.getSongById(songId);
    }

    public LiveData<List<Song>> searchSongs(String query) {
        return repository.searchSongs(query);
    }

    public LiveData<List<Song>> getSongsByArtist(String artist) {
        return repository.getSongsByArtist(artist);
    }

    public LiveData<List<Song>> getSongsByAlbum(String album) {
        return repository.getSongsByAlbum(album);
    }

    public LiveData<List<Song>> getFavoriteSongs() {
        return repository.getFavoriteSongs();
    }

    public LiveData<List<Song>> getMostPlayedSongs(int limit) {
        return repository.getMostPlayedSongs(limit);
    }

    public LiveData<List<Song>> getRecentlyPlayedSongs(int limit) {
        return repository.getRecentlyPlayedSongs(limit);
    }

    public LiveData<List<Song>> getRecentlyAddedSongs(int limit) {
        return repository.getRecentlyAddedSongs(limit);
    }

    // ========== ACCIONES ==========
    public void toggleFavorite(long songId, boolean isFavorite) {
        repository.toggleFavorite(songId, isFavorite);
    }

    public void incrementPlayCount(long songId) {
        repository.incrementPlayCount(songId);
    }

    // ========== ESTADÍSTICAS ==========
    public LiveData<Integer> getTotalSongsCount() {
        return repository.getTotalSongsCount();
    }

    public LiveData<Long> getTotalDuration() {
        return repository.getTotalDuration();
    }

    public LiveData<List<String>> getAllArtists() {
        return repository.getAllArtists();
    }

    public LiveData<List<String>> getAllAlbums() {
        return repository.getAllAlbums();
    }
}
