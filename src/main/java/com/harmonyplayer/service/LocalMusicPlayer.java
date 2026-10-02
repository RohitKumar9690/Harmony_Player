package com.harmonyplayer.service;

import com.harmonyplayer.model.Song;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

public class LocalMusicPlayer {
    private MediaPlayer mediaPlayer;
    private Song currentSong;

    private Consumer<Duration> onTimeUpdate;
    private Runnable onSongEnd;

    private double currentVolume = 0.5;

    public void play(Song song) {
        stop();

        if (song == null || song.getFilePath() == null || song.getFilePath().isBlank()) {
            throw new RuntimeException("Invalid song or file path");
        }

        File file = new File(song.getFilePath());
        if (!file.isFile()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }

        try {
            String uri = file.toURI().toString();
            System.out.println("Playing: " + uri);

            Media media = new Media(uri);

            // IMPORTANT: catch media creation/decoding problems (often unsupported format)
            media.setOnError(() -> fail("Media error: " + media.getError()));

            mediaPlayer = new MediaPlayer(media);

            // IMPORTANT: catch player errors (decode/playback errors)
            mediaPlayer.setOnError(() -> fail("MediaPlayer error: " + mediaPlayer.getError()));

            mediaPlayer.setOnReady(() -> {
                // This runs on JavaFX thread
                try {
                    Duration total = media.getDuration();
                    if (total != null && !total.isUnknown()) {
                        song.setDuration((long) total.toSeconds());
                    }
                    mediaPlayer.setVolume(currentVolume);
                    currentSong = song;

                    mediaPlayer.play(); // start only when READY (more reliable)
                } catch (Exception ex) {
                    fail("OnReady failed: " + ex.getMessage());
                }
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (onTimeUpdate != null) onTimeUpdate.accept(newTime);
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (onSongEnd != null) onSongEnd.run();
            });

        } catch (MediaException me) {
            me.printStackTrace();
            throw new RuntimeException("JavaFX MediaException: " + me.getMessage(), me);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to play song: " + e.getMessage(), e);
        }
    }

    private void fail(String msg) {
        System.err.println(msg);
        // cleanup player to avoid stuck state
        Platform.runLater(this::stop);
    }

    public void pause() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    public void resume() {
        if (mediaPlayer != null) mediaPlayer.play();
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } finally {
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(Math.max(0, seconds)));
        }
    }

    public void setVolume(double volume) {
        currentVolume = Math.max(0.0, Math.min(1.0, volume));
        if (mediaPlayer != null) mediaPlayer.setVolume(currentVolume);
    }

    public double getVolume() {
        return currentVolume;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void setOnTimeUpdate(Consumer<Duration> callback) {
        this.onTimeUpdate = callback;
    }

    public void setOnSongEnd(Runnable callback) {
        this.onSongEnd = callback;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public Duration getCurrentTime() {
        return mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setRate(double rate) {
        if (mediaPlayer != null) mediaPlayer.setRate(rate);
    }

    public double getRate() {
        return mediaPlayer != null ? mediaPlayer.getRate() : 1.0;
    }
}