package com.harmonyplayer.controller;

import com.harmonyplayer.model.Playlist;
import com.harmonyplayer.model.Song;
import com.harmonyplayer.service.LocalMusicPlayer;
import com.harmonyplayer.service.LyricsService;
import com.harmonyplayer.service.YouTubePlayer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainController {

    private Playlist currentPlaylist;
    private final List<Playlist> allPlaylists;

    private LocalMusicPlayer localPlayer;
    private YouTubePlayer youTubePlayer;
    private LyricsService lyricsService;
    private PlayerController playerController;

    private final ObservableList<Song> observableSongs;

    private Consumer<String> onStatusUpdate;
    private Consumer<Song> onSongChanged;

    public MainController() {
        this.currentPlaylist = new Playlist("Default Playlist");
        this.allPlaylists = new ArrayList<>();
        this.allPlaylists.add(currentPlaylist);
        this.observableSongs = FXCollections.observableArrayList();

        initializeServices();
    }

    private void initializeServices() {
        this.localPlayer = new LocalMusicPlayer();
        this.youTubePlayer = new YouTubePlayer(localPlayer);
        this.lyricsService = new LyricsService();
        this.playerController = new PlayerController(localPlayer, youTubePlayer);

        // ---- Bridge PlayerController events to MainController callbacks ----
        playerController.setOnSongChanged(song -> {
            // Keep playlist index in sync
            if (song != null) {
                int idx = currentPlaylist.getSongs().indexOf(song);
                if (idx >= 0) currentPlaylist.setCurrentIndex(idx);
            }

            if (onSongChanged != null) onSongChanged.accept(song);
        });

        playerController.setOnPlaybackStarted(() -> {
            Song s = playerController.getCurrentSong();
            if (onStatusUpdate != null && s != null) {
                onStatusUpdate.accept("Playing: " + s.getTitle());
            }
        });

        playerController.setOnPlaybackPaused(() -> {
            Song s = playerController.getCurrentSong();
            if (onStatusUpdate != null && s != null) {
                onStatusUpdate.accept("Paused: " + s.getTitle());
            }
        });

        playerController.setOnPlaybackStopped(() -> {
            if (onStatusUpdate != null) {
                onStatusUpdate.accept("Stopped");
            }
        });
    }

    // ---------------- Playlists ----------------

    public Playlist createPlaylist(String name) {
        Playlist playlist = new Playlist(name);
        allPlaylists.add(playlist);
        return playlist;
    }

    public boolean deletePlaylist(String name) {
        if (name == null) return false;
        if (name.equals("Default Playlist")) return false;

        boolean removed = allPlaylists.removeIf(p -> name.equals(p.getName()));

        // Safety: if current playlist was deleted, fall back
        if (removed && currentPlaylist != null && name.equals(currentPlaylist.getName())) {
            currentPlaylist = allPlaylists.isEmpty() ? new Playlist("Default Playlist") : allPlaylists.get(0);
            refreshObservableList();
        }
        return removed;
    }

    public void switchPlaylist(Playlist playlist) {
        if (playlist != null) {
            this.currentPlaylist = playlist;
            refreshObservableList();
        }
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(allPlaylists);
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    // ---------------- Library add/remove ----------------

    public void addLocalMusicFiles(List<File> files) {
        if (files == null || files.isEmpty()) return;

        Thread t = new Thread(() -> {
            int addedCount = 0;

            for (File file : files) {
                try {
                    Song song = extractSongFromFile(file);
                    if (song != null) {
                        addedCount++;

                        Platform.runLater(() -> {
                            currentPlaylist.addSong(song);
                            observableSongs.add(song);
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error adding file: " + file.getName());
                    e.printStackTrace();
                }
            }

            final int finalCount = addedCount;
            Platform.runLater(() -> {
                if (onStatusUpdate != null) {
                    onStatusUpdate.accept("Added " + finalCount + " song(s)");
                }
            });

        }, "local-music-import");
        t.setDaemon(true);
        t.start();
    }

    public void addLocalMusicFile(File file) {
        if (file == null) return;
        List<File> files = new ArrayList<>();
        files.add(file);
        addLocalMusicFiles(files);
    }

    public void addYouTubeSong(String url, String title, String artist) {
        if (url == null || url.trim().isEmpty()) {
            if (onStatusUpdate != null) onStatusUpdate.accept("Invalid YouTube URL");
            return;
        }

        Song song = new Song(
                (title == null || title.isEmpty()) ? "YouTube Song" : title,
                (artist == null || artist.isEmpty()) ? "Unknown Artist" : artist,
                url,
                true
        );

        currentPlaylist.addSong(song);
        observableSongs.add(song);

        if (onStatusUpdate != null) {
            onStatusUpdate.accept("Added YouTube song: " + song.getTitle());
        }
    }

    public void removeSong(int index) {
        if (index >= 0 && index < currentPlaylist.getSongs().size()) {
            Song removed = currentPlaylist.getSongs().get(index);
            currentPlaylist.removeSong(index);
            observableSongs.remove(removed);

            if (onStatusUpdate != null) onStatusUpdate.accept("Removed: " + removed.getTitle());
        }
    }

    public void removeSong(Song song) {
        if (song == null) return;
        int index = currentPlaylist.getSongs().indexOf(song);
        if (index >= 0) removeSong(index);
    }

    public void clearPlaylist() {
        currentPlaylist.getSongs().clear();
        observableSongs.clear();

        if (onStatusUpdate != null) onStatusUpdate.accept("Playlist cleared");
    }

    // ---------------- Metadata ----------------

    private Song extractSongFromFile(File file) {
        if (file == null || !file.isFile()) return null;

        String title = file.getName().replaceFirst("[.][^.]+$", "");
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        String genre = "";
        int year = 0;
        int bitrate = 0;
        String format = getFileExtension(file);

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            if (tag != null) {
                String tagTitle = tag.getFirst(FieldKey.TITLE);
                String tagArtist = tag.getFirst(FieldKey.ARTIST);
                String tagAlbum = tag.getFirst(FieldKey.ALBUM);
                String tagGenre = tag.getFirst(FieldKey.GENRE);
                String tagYear = tag.getFirst(FieldKey.YEAR);

                if (tagTitle != null && !tagTitle.isEmpty()) title = tagTitle;
                if (tagArtist != null && !tagArtist.isEmpty()) artist = tagArtist;
                if (tagAlbum != null && !tagAlbum.isEmpty()) album = tagAlbum;
                if (tagGenre != null && !tagGenre.isEmpty()) genre = tagGenre;
                if (tagYear != null && !tagYear.isEmpty()) {
                    try { year = Integer.parseInt(tagYear); } catch (NumberFormatException ignored) {}
                }
            }

            if (audioFile.getAudioHeader() != null) {
                try {
                    bitrate = (int) audioFile.getAudioHeader().getBitRateAsNumber();
                } catch (Throwable ignored) {
                    // some formats/headers may not support this
                }
            }

        } catch (Exception e) {
            System.err.println("Could not read metadata for: " + file.getName());
        }

        Song song = new Song(title, artist, file.getAbsolutePath());
        song.setAlbum(album);
        song.setGenre(genre);
        song.setYear(year);
        song.setBitrate(bitrate);
        song.setFormat(format);

        return song;
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) return name.substring(lastDot + 1);
        return "";
    }

    // ---------------- Search ----------------

    public List<Song> searchSongs(String query) {
        List<Song> results = new ArrayList<>();
        if (query == null) return results;

        String lowerQuery = query.toLowerCase();

        for (Song song : currentPlaylist.getSongs()) {
            if (song.getTitle().toLowerCase().contains(lowerQuery) ||
                song.getArtist().toLowerCase().contains(lowerQuery) ||
                (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(lowerQuery)) ||
                (song.getGenre() != null && song.getGenre().toLowerCase().contains(lowerQuery))) {
                results.add(song);
            }
        }
        return results;
    }

    // ---------------- Playback ----------------

    public void playSong(Song song) {
        if (song == null) return;

        int index = currentPlaylist.getSongs().indexOf(song);
        if (index >= 0) currentPlaylist.setCurrentIndex(index);

        // PlayerController already fires onSongChanged + onPlaybackStarted,
        // so we only handle error here.
        playerController.play(song, null, error -> {
            if (onStatusUpdate != null) onStatusUpdate.accept("Error: " + error);
        });
    }

    public void playSongAtIndex(int index) {
        List<Song> songs = currentPlaylist.getSongs();
        if (index >= 0 && index < songs.size()) {
            currentPlaylist.setCurrentIndex(index);
            playSong(songs.get(index));
        }
    }

    // Optional convenience
    public void playNext() {
        Song next = currentPlaylist.getNextSong();
        if (next != null) playSong(next);
    }

    public void playPrevious() {
        Song prev = currentPlaylist.getPreviousSong();
        if (prev != null) playSong(prev);
    }

    // ---------------- Getters ----------------

    public PlayerController getPlayerController() {
        return playerController;
    }

    public LyricsService getLyricsService() {
        return lyricsService;
    }

    public ObservableList<Song> getObservableSongs() {
        return observableSongs;
    }

    private void refreshObservableList() {
        observableSongs.setAll(currentPlaylist.getSongs());
    }

    // ---------------- Callbacks ----------------

    public void setOnStatusUpdate(Consumer<String> callback) {
        this.onStatusUpdate = callback;
    }

    public void setOnSongChanged(Consumer<Song> callback) {
        this.onSongChanged = callback;
    }

    // ---------------- Shutdown ----------------

    public void shutdown() {
        playerController.stop();
        youTubePlayer.cleanup();
    }
}