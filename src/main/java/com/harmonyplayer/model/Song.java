package com.harmonyplayer.model;

import javafx.scene.image.Image;

public class Song {
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int year;
    private String filePath;
    private String youtubeUrl;
    private long duration;
    private boolean isYouTube;
    private Image albumArt;
    private String albumArtPath;
    private String lyrics;
    private int bitrate;
    private String format;

    // Constructor for local songs
    public Song(String title, String artist, String filePath) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.isYouTube = false;
        this.duration = 0;
    }

    // Constructor for YouTube songs
    public Song(String title, String artist, String youtubeUrl, boolean isYouTube) {
        this.title = title;
        this.artist = artist;
        this.youtubeUrl = youtubeUrl;
        this.isYouTube = isYouTube;
        this.duration = 0;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public boolean isYouTube() { return isYouTube; }
    public void setYouTube(boolean youTube) { isYouTube = youTube; }

    public Image getAlbumArt() { return albumArt; }
    public void setAlbumArt(Image albumArt) { this.albumArt = albumArt; }

    public String getAlbumArtPath() { return albumArtPath; }
    public void setAlbumArtPath(String albumArtPath) { this.albumArtPath = albumArtPath; }

    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    @Override
    public String toString() {
        return title + " - " + artist;
    }

    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Title: ").append(title).append("\n");
        info.append("Artist: ").append(artist).append("\n");
        if (album != null && !album.isEmpty()) {
            info.append("Album: ").append(album).append("\n");
        }
        if (genre != null && !genre.isEmpty()) {
            info.append("Genre: ").append(genre).append("\n");
        }
        if (year > 0) {
            info.append("Year: ").append(year).append("\n");
        }
        if (duration > 0) {
            info.append("Duration: ").append(formatDuration(duration)).append("\n");
        }
        if (bitrate > 0) {
            info.append("Bitrate: ").append(bitrate).append(" kbps\n");
        }
        if (format != null) {
            info.append("Format: ").append(format).append("\n");
        }
        return info.toString();
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
}