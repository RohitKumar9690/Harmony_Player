package com.harmonyplayer.model;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

public class Album {
    private String name;
    private String artist;
    private int year;
    private String genre;
    private List<Song> songs;
    private Image albumArt;
    private String albumArtPath;

    public Album(String name, String artist) {
        this.name = name;
        this.artist = artist;
        this.songs = new ArrayList<>();
    }

    public void addSong(Song song) {
        songs.add(song);
        song.setAlbum(name);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public List<Song> getSongs() { return songs; }

    public Image getAlbumArt() { return albumArt; }
    public void setAlbumArt(Image albumArt) { this.albumArt = albumArt; }

    public String getAlbumArtPath() { return albumArtPath; }
    public void setAlbumArtPath(String albumArtPath) { this.albumArtPath = albumArtPath; }

    @Override
    public String toString() {
        return name + " - " + artist + " (" + songs.size() + " songs)";
    }
}