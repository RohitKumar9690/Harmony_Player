package com.harmonyplayer.service;

import com.harmonyplayer.model.Song;
import javafx.scene.image.Image;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.ByteArrayInputStream;
import java.io.File;

public class AlbumArtService {

    private static final String DEFAULT_ALBUM_ART_URL = "https://via.placeholder.com/300x300/1a1a2e/ffffff?text=No+Album+Art";

    /**
     * Extract album art from audio file metadata
     */
    public Image extractAlbumArt(Song song) {
        if (song.isYouTube()) {
            return fetchAlbumArtFromWeb(song);
        }

        try {
            File file = new File(song.getFilePath());
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            if (tag != null) {
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    byte[] imageData = artwork.getBinaryData();
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
                    Image image = new Image(bis);
                    song.setAlbumArt(image);
                    return image;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract album art: " + e.getMessage());
        }

        return getDefaultAlbumArt();
    }

    /**
     * Fetch album art from web services
     */
    public Image fetchAlbumArtFromWeb(Song song) {
        try {
            // In production, you would use iTunes API or similar
            // For now, return default
            return getDefaultAlbumArt();
        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultAlbumArt();
        }
    }

    /**
     * Get default album art
     */
    public Image getDefaultAlbumArt() {
        try {
            return new Image(DEFAULT_ALBUM_ART_URL);
        } catch (Exception e) {
            // Create a simple placeholder
            return null;
        }
    }

    /**
     * Save album art to file
     */
    public void saveAlbumArt(Song song, File outputFile) {
        // Implementation for saving album art
    }
}