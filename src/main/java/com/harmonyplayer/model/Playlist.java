package com.harmonyplayer.model;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String name;
    private List<Song> songs;
    private int currentIndex;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
        this.currentIndex = 0;
    }

    public void addSong(Song song) {
        songs.add(song);
    }

    public void removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            songs.remove(index);
            if (currentIndex >= songs.size() && songs.size() > 0) {
                currentIndex = songs.size() - 1;
            }
        }
    }

    public Song getCurrentSong() {
        if (songs.isEmpty() || currentIndex < 0 || currentIndex >= songs.size()) {
            return null;
        }
        return songs.get(currentIndex);
    }

    public Song getNextSong() {
        if (songs.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % songs.size();
        return songs.get(currentIndex);
    }

    public Song getPreviousSong() {
        if (songs.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + songs.size()) % songs.size();
        return songs.get(currentIndex);
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < songs.size()) {
            this.currentIndex = index;
        }
    }

    // Getters
    public String getName() { 
        return name; 
    }
    
    public List<Song> getSongs() { 
        return songs; 
    }
    
    public int getCurrentIndex() { 
        return currentIndex; 
    }
}