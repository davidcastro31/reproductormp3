package com.example.reproductormp3.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "songs")
public class Song {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String artist;
    private String album;
    private String path;          // Ruta del archivo
    private long duration;        // En milisegundos
    private String albumArtUri;   // URI de la carátula (local o URL)
    private int playCount;        // Contador de reproducciones
    private long lastPlayed;      // Timestamp última reproducción
    private boolean isFavorite;
    private String genre;
    private int year;
    private long dateAdded;       // Timestamp cuando se agregó

    // Constructor completo
    public Song(String title, String artist, String album, String path,
                long duration, String albumArtUri) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
        this.albumArtUri = albumArtUri;
        this.playCount = 0;
        this.isFavorite = false;
        this.dateAdded = System.currentTimeMillis();
    }

    // Constructor vacío (requerido por Room)
    @Ignore
    public Song() {
        this.dateAdded = System.currentTimeMillis();
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist != null ? artist : "Artista Desconocido";
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album != null ? album : "Álbum Desconocido";
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(String albumArtUri) {
        this.albumArtUri = albumArtUri;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Métodos útiles
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getDisplayTitle() {
        return title != null && !title.isEmpty() ? title : "Sin título";
    }

    @Override
    public String toString() {
        return "Song{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                '}';
    }
}
