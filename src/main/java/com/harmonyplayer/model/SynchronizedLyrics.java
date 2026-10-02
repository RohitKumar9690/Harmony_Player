package com.harmonyplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizedLyrics {
    private String title;
    private String artist;
    private String album;
    private String creator;
    private List<LyricLine> lines;
    private int currentLineIndex;

    public SynchronizedLyrics() {
        this.lines = new ArrayList<>();
        this.currentLineIndex = -1;
    }

    public void addLine(LyricLine line) {
        lines.add(line);
        Collections.sort(lines);
    }

    public void addLine(long timestamp, String text) {
        addLine(new LyricLine(timestamp, text));
    }

    public LyricLine getCurrentLine(long currentTimeMs) {
        if (lines.isEmpty()) {
            return null;
        }

        // Find the current line based on timestamp
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (currentTimeMs >= lines.get(i).getTimestamp()) {
                currentLineIndex = i;
                return lines.get(i);
            }
        }

        return null;
    }

    public LyricLine getNextLine() {
        if (currentLineIndex >= 0 && currentLineIndex < lines.size() - 1) {
            return lines.get(currentLineIndex + 1);
        }
        return null;
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }

    public void reset() {
        currentLineIndex = -1;
        for (LyricLine line : lines) {
            line.setActive(false);
        }
    }

    public List<LyricLine> getLines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        for (LyricLine line : lines) {
            sb.append(line.getText()).append("\n");
        }
        return sb.toString();
    }

    public String toLRC() {
        StringBuilder sb = new StringBuilder();
        
        // Add metadata
        if (title != null) sb.append("[ti:").append(title).append("]\n");
        if (artist != null) sb.append("[ar:").append(artist).append("]\n");
        if (album != null) sb.append("[al:").append(album).append("]\n");
        if (creator != null) sb.append("[by:").append(creator).append("]\n");
        sb.append("\n");

        // Add lyrics
        for (LyricLine line : lines) {
            sb.append(line.toString()).append("\n");
        }

        return sb.toString();
    }

    public static SynchronizedLyrics fromLRC(String lrcContent) {
        SynchronizedLyrics lyrics = new SynchronizedLyrics();
        
        if (lrcContent == null || lrcContent.trim().isEmpty()) {
            return lyrics;
        }

        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // Parse metadata
            if (line.startsWith("[ti:")) {
                lyrics.title = line.substring(4, line.indexOf("]"));
            } else if (line.startsWith("[ar:")) {
                lyrics.artist = line.substring(4, line.indexOf("]"));
            } else if (line.startsWith("[al:")) {
                lyrics.album = line.substring(4, line.indexOf("]"));
            } else if (line.startsWith("[by:")) {
                lyrics.creator = line.substring(4, line.indexOf("]"));
            } else {
                // Parse lyric line
                LyricLine lyricLine = LyricLine.parseLRC(line);
                if (lyricLine != null) {
                    lyrics.addLine(lyricLine);
                }
            }
        }

        return lyrics;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
}