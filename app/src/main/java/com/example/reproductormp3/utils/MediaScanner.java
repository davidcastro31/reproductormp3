package com.example.reproductormp3.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.reproductormp3.models.Song;

import java.util.ArrayList;
import java.util.List;

public class MediaScanner {

    private static final String TAG = "MediaScanner";
    private Context context;

    public MediaScanner(Context context) {
        this.context = context;
    }

    /**
     * Escanea toda la música del dispositivo
     */
    public List<Song> scanMusicFiles() {
        List<Song> songs = new ArrayList<>();

        // Columnas que queremos obtener
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED
        };

        // Filtro: solo música (no notificaciones, alarmas, etc.)
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        // Ordenar por título
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        try (Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {

                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    String path = cursor.getString(dataColumn);
                    long duration = cursor.getLong(durationColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    int year = cursor.getInt(yearColumn);
                    long dateAdded = cursor.getLong(dateAddedColumn) * 1000; // Convertir a milisegundos

                    // Obtener URI de la carátula del álbum
                    String albumArtUri = getAlbumArtUri(albumId);

                    // Crear objeto Song
                    Song song = new Song(title, artist, album, path, duration, albumArtUri);
                    song.setYear(year);
                    song.setDateAdded(dateAdded);

                    // Intentar obtener más metadatos (género)
                    try {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(path);
                        String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                        song.setGenre(genre);
                        retriever.release();
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo obtener género de: " + path);
                    }

                    songs.add(song);

                } while (cursor.moveToNext());

                Log.i(TAG, "Se encontraron " + songs.size() + " canciones");
            } else {
                Log.w(TAG, "No se encontró música en el dispositivo");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al escanear música: " + e.getMessage());
        }

        return songs;
    }

    /**
     * Obtiene la URI de la carátula del álbum
     */
    private String getAlbumArtUri(long albumId) {
        Uri albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
        );
        return albumArtUri.toString();
    }

    /**
     * Extrae la carátula embebida en el archivo MP3
     */
    public Bitmap getEmbeddedAlbumArt(String path) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();

            if (art != null) {
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener carátula embebida: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si un archivo de audio existe
     */
    public boolean fileExists(String path) {
        java.io.File file = new java.io.File(path);
        return file.exists();
    }

    /**
     * Obtiene información de una canción específica
     */
    public Song getSongInfo(String path) {
        List<Song> songs = scanMusicFiles();
        for (Song song : songs) {
            if (song.getPath().equals(path)) {
                return song;
            }
        }
        return null;
    }
}
