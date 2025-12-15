package com.example.reproductormp3.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.reproductormp3.models.Song;

import java.util.List;

@Dao
public interface SongDao {

    // ========== INSERTAR ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Song song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Song> songs);

    // ========== ACTUALIZAR ==========
    @Update
    void update(Song song);

    // ========== ELIMINAR ==========
    @Delete
    void delete(Song song);

    @Query("DELETE FROM songs")
    void deleteAll();

    // ========== CONSULTAS BÁSICAS ==========
    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs WHERE id = :songId")
    LiveData<Song> getSongById(long songId);

    @Query("SELECT * FROM songs WHERE path = :path")
    Song getSongByPath(String path);

    // ========== BÚSQUEDA ==========
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' " +
            "OR artist LIKE '%' || :query || '%' " +
            "OR album LIKE '%' || :query || '%' " +
            "ORDER BY title ASC")
    LiveData<List<Song>> searchSongs(String query);

    // ========== FILTROS ==========
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    LiveData<List<Song>> getSongsByArtist(String artist);

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title")
    LiveData<List<Song>> getSongsByAlbum(String album);

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title")
    LiveData<List<Song>> getSongsByGenre(String genre);

    // ========== FAVORITOS ==========
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title")
    LiveData<List<Song>> getFavoriteSongs();

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    void updateFavoriteStatus(long songId, boolean isFavorite);

    // ========== REPRODUCCIÓN ==========
    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :songId")
    void incrementPlayCount(long songId, long timestamp);

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    LiveData<List<Song>> getMostPlayedSongs(int limit);

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    LiveData<List<Song>> getRecentlyPlayedSongs(int limit);

    // ========== ARTISTAS Y ÁLBUMES (DISTINCT) ==========
    @Query("SELECT DISTINCT artist FROM songs WHERE artist IS NOT NULL ORDER BY artist")
    LiveData<List<String>> getAllArtists();

    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL ORDER BY album")
    LiveData<List<String>> getAllAlbums();

    @Query("SELECT DISTINCT genre FROM songs WHERE genre IS NOT NULL ORDER BY genre")
    LiveData<List<String>> getAllGenres();

    // ========== ESTADÍSTICAS ==========
    @Query("SELECT COUNT(*) FROM songs")
    LiveData<Integer> getTotalSongsCount();

    @Query("SELECT SUM(duration) FROM songs")
    LiveData<Long> getTotalDuration();

    @Query("SELECT COUNT(DISTINCT artist) FROM songs WHERE artist IS NOT NULL")
    LiveData<Integer> getTotalArtistsCount();

    @Query("SELECT COUNT(DISTINCT album) FROM songs WHERE album IS NOT NULL")
    LiveData<Integer> getTotalAlbumsCount();

    // ========== ORDENAMIENTO ==========
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    LiveData<List<Song>> getRecentlyAddedSongs(int limit);

    @Query("SELECT * FROM songs ORDER BY year DESC")
    LiveData<List<Song>> getSongsByYear();

    // ========== VERIFICACIÓN ==========
    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE path = :path)")
    boolean songExists(String path);
}
