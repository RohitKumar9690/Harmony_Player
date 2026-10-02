package com.harmonyplayer.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.harmonyplayer.model.Song;
import com.harmonyplayer.model.SynchronizedLyrics;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LyricsService {
    
    private static final String LYRICS_CACHE_DIR = System.getProperty("user.home") + 
                                                   "/.harmonyplayer/lyrics/";
    private static final String LRC_CACHE_DIR = System.getProperty("user.home") + 
                                               "/.harmonyplayer/lyrics/lrc/";

    public LyricsService() {
        new File(LYRICS_CACHE_DIR).mkdirs();
        new File(LRC_CACHE_DIR).mkdirs();
    }

    /**
     * Fetch plain text lyrics
     */
    public String fetchLyrics(String artist, String title) {
        String cachedLyrics = getCachedLyrics(artist, title);
        if (cachedLyrics != null) {
            return cachedLyrics;
        }

        String lyrics = fetchFromLyricsOvh(artist, title);

        if (lyrics != null && !lyrics.contains("not found")) {
            cacheLyrics(artist, title, lyrics);
        }

        return lyrics != null ? lyrics : getNotFoundMessage();
    }

    /**
     * Fetch synchronized lyrics (LRC format)
     */
    public SynchronizedLyrics fetchSynchronizedLyrics(String artist, String title) {
        // Check cache first
        SynchronizedLyrics cached = getCachedLRC(artist, title);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Try to fetch from online sources
        SynchronizedLyrics lyrics = fetchLRCFromOnline(artist, title);
        
        if (lyrics != null && !lyrics.isEmpty()) {
            cacheLRC(artist, title, lyrics);
            return lyrics;
        }

        return new SynchronizedLyrics();
    }

    /**
     * Fetch LRC from online sources
     */
    private SynchronizedLyrics fetchLRCFromOnline(String artist, String title) {
        try {
            // Using lrclib.net API (free LRC lyrics)
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.toString());
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
            
            String url = String.format("https://lrclib.net/api/get?artist_name=%s&track_name=%s", 
                                      encodedArtist, encodedTitle);
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    
                    if (jsonObject.has("syncedLyrics")) {
                        String lrcContent = jsonObject.get("syncedLyrics").getAsString();
                        return SynchronizedLyrics.fromLRC(lrcContent);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch LRC: " + e.getMessage());
        }
        
        return null;
    }

    private String fetchFromLyricsOvh(String artist, String title) {
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.toString());
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
            
            String url = String.format("https://api.lyrics.ovh/v1/%s/%s", 
                                      encodedArtist, encodedTitle);
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    
                    if (jsonObject.has("lyrics")) {
                        return jsonObject.get("lyrics").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("lyrics.ovh failed: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Cache plain text lyrics
     */
    public void cacheLyrics(String artist, String title, String lyrics) {
        try {
            String filename = sanitizeFilename(artist + "_" + title) + ".txt";
            Path path = Paths.get(LYRICS_CACHE_DIR + filename);
            Files.write(path, lyrics.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to cache lyrics: " + e.getMessage());
        }
    }

    /**
     * Cache LRC lyrics
     */
    public void cacheLRC(String artist, String title, SynchronizedLyrics lyrics) {
        try {
            String filename = sanitizeFilename(artist + "_" + title) + ".lrc";
            Path path = Paths.get(LRC_CACHE_DIR + filename);
            Files.write(path, lyrics.toLRC().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to cache LRC: " + e.getMessage());
        }
    }

    /**
     * Get cached plain text lyrics
     */
    public String getCachedLyrics(String artist, String title) {
        try {
            String filename = sanitizeFilename(artist + "_" + title) + ".txt";
            Path path = Paths.get(LYRICS_CACHE_DIR + filename);
            
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Failed to read cached lyrics: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get cached LRC lyrics
     */
    public SynchronizedLyrics getCachedLRC(String artist, String title) {
        try {
            String filename = sanitizeFilename(artist + "_" + title) + ".lrc";
            Path path = Paths.get(LRC_CACHE_DIR + filename);
            
            if (Files.exists(path)) {
                String lrcContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                return SynchronizedLyrics.fromLRC(lrcContent);
            }
        } catch (IOException e) {
            System.err.println("Failed to read cached LRC: " + e.getMessage());
        }
        return null;
    }

    /**
     * Save custom synchronized lyrics
     */
    public void saveCustomLyrics(Song song, String lyrics) {
        song.setLyrics(lyrics);
        cacheLyrics(song.getArtist(), song.getTitle(), lyrics);
    }

    /**
     * Save synchronized lyrics
     */
    public void saveSynchronizedLyrics(Song song, SynchronizedLyrics lyrics) {
        cacheLRC(song.getArtist(), song.getTitle(), lyrics);
    }

    public void clearCache() {
        try {
            File cacheDir = new File(LYRICS_CACHE_DIR);
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            
            File lrcCacheDir = new File(LRC_CACHE_DIR);
            File[] lrcFiles = lrcCacheDir.listFiles();
            if (lrcFiles != null) {
                for (File file : lrcFiles) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to clear cache: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String getNotFoundMessage() {
        return "Lyrics not found.\n\n" +
               "Options:\n" +
               "• Click 'Edit Lyrics' to add manually\n" +
               "• Check song title and artist spelling\n" +
               "• Verify internet connection\n" +
               "• Try refreshing after a moment\n\n" +
               "You can also paste lyrics from other sources.";
    }
}