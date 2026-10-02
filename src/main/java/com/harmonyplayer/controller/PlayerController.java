package com.harmonyplayer.controller;

import com.harmonyplayer.model.Song;
import com.harmonyplayer.service.LocalMusicPlayer;
import com.harmonyplayer.service.YouTubePlayer;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class PlayerController {

    private final LocalMusicPlayer localPlayer;
    private final YouTubePlayer youTubePlayer;

    private Song currentSong;
    private PlayerState state;
    private boolean shuffle;
    private RepeatMode repeatMode;

    private Consumer<Duration> onTimeUpdate;
    private Consumer<Song> onSongChanged;
    private Consumer<PlayerState> onStateChanged;

    private Runnable onPlaybackStarted;
    private Runnable onPlaybackPaused;
    private Runnable onPlaybackStopped;
    private Runnable onSongEnded;

    public enum PlayerState {
        STOPPED, PLAYING, PAUSED, LOADING
    }

    public enum RepeatMode {
        OFF, ONE, ALL
    }

    public PlayerController(LocalMusicPlayer localPlayer, YouTubePlayer youTubePlayer) {
        this.localPlayer = localPlayer;
        this.youTubePlayer = youTubePlayer;

        this.state = PlayerState.STOPPED;
        this.shuffle = false;
        this.repeatMode = RepeatMode.OFF;

        setupCallbacks();
    }

    private void setupCallbacks() {
        if (localPlayer != null) {
            localPlayer.setOnTimeUpdate(duration -> runFx(() -> {
                if (onTimeUpdate != null) onTimeUpdate.accept(duration);
            }));

            localPlayer.setOnSongEnd(() -> runFx(() -> {
                if (repeatMode == RepeatMode.ONE && currentSong != null) {
                    play(currentSong, null, null);
                } else {
                    if (onSongEnded != null) onSongEnded.run();
                }
            }));
        }
    }

    // ---------- IMPORTANT NEW METHOD (for EqualizerController) ----------

    /**
     * Returns the active JavaFX MediaPlayer for LOCAL playback.
     * For YouTube playback this returns null.
     */
    public MediaPlayer getActiveMediaPlayer() {
        if (currentSong == null || currentSong.isYouTube()) return null;
        return localPlayer != null ? localPlayer.getMediaPlayer() : null;
    }

    // ---------- Play API ----------

    public void play(Song song) {
        play(song, null, null);
    }

    public void play(Song song, Runnable onSuccess, Consumer<String> onError) {
        if (song == null) {
            if (onError != null) onError.accept("No song selected");
            return;
        }

        // Stop current playback (switch track)
        stopInternal(false);

        currentSong = song;
        setState(PlayerState.LOADING);

        if (song.isYouTube()) {
            playYouTubeSong(song, onSuccess, onError);
        } else {
            playLocalSong(song, onSuccess, onError);
        }
    }

    private void playLocalSong(Song song, Runnable onSuccess, Consumer<String> onError) {
        try {
            if (localPlayer == null) throw new IllegalStateException("LocalMusicPlayer is null");

            localPlayer.play(song);
            setState(PlayerState.PLAYING);

            runFx(() -> {
                if (onSongChanged != null) onSongChanged.accept(song);
                if (onPlaybackStarted != null) onPlaybackStarted.run();
                if (onSuccess != null) onSuccess.run();
            });

        } catch (Exception e) {
            e.printStackTrace();
            setState(PlayerState.STOPPED);
            runFx(() -> {
                if (onPlaybackStopped != null) onPlaybackStopped.run();
                if (onError != null) onError.accept("Error playing local song: " + e.getMessage());
            });
        }
    }

    private void playYouTubeSong(Song song, Runnable onSuccess, Consumer<String> onError) {
        try {
            if (youTubePlayer == null) throw new IllegalStateException("YouTubePlayer is null");

            youTubePlayer.playYouTubeVideo(song,
                    () -> runFx(() -> {
                        setState(PlayerState.PLAYING);
                        if (onSongChanged != null) onSongChanged.accept(song);
                        if (onPlaybackStarted != null) onPlaybackStarted.run();
                        if (onSuccess != null) onSuccess.run();
                    }),
                    error -> runFx(() -> {
                        setState(PlayerState.STOPPED);
                        if (onPlaybackStopped != null) onPlaybackStopped.run();
                        if (onError != null) onError.accept(error);
                    })
            );

        } catch (Exception e) {
            e.printStackTrace();
            setState(PlayerState.STOPPED);
            runFx(() -> {
                if (onPlaybackStopped != null) onPlaybackStopped.run();
                if (onError != null) onError.accept("Error playing YouTube song: " + e.getMessage());
            });
        }
    }

    // ---------- Transport ----------

    public void pause() {
        if (state != PlayerState.PLAYING) return;

        if (isYouTubeCurrent()) {
            invokeIfExists(youTubePlayer, "pause");
        } else {
            if (localPlayer != null) localPlayer.pause();
        }

        setState(PlayerState.PAUSED);
        runFx(() -> {
            if (onPlaybackPaused != null) onPlaybackPaused.run();
        });
    }

    public void resume() {
        if (state != PlayerState.PAUSED) return;

        if (isYouTubeCurrent()) {
            invokeIfExists(youTubePlayer, "resume");
            invokeIfExists(youTubePlayer, "play");
        } else {
            if (localPlayer != null) localPlayer.resume();
        }

        setState(PlayerState.PLAYING);
        runFx(() -> {
            if (onPlaybackStarted != null) onPlaybackStarted.run();
        });
    }

    public void togglePlayPause() {
        if (state == PlayerState.PLAYING) {
            pause();
        } else if (state == PlayerState.PAUSED) {
            resume();
        } else if (state == PlayerState.STOPPED && currentSong != null) {
            play(currentSong, null, null);
        }
    }

    public void stop() {
        stopInternal(true);
        currentSong = null;
        setState(PlayerState.STOPPED);

        runFx(() -> {
            if (onPlaybackStopped != null) onPlaybackStopped.run();
        });
    }

    private void stopInternal(boolean stopEverything) {
        if (localPlayer != null) {
            try { localPlayer.stop(); } catch (Exception ignored) {}
        }
        if (youTubePlayer != null) {
            invokeIfExists(youTubePlayer, "stop");
        }
        if (stopEverything) {
            // optional extra cleanup
        }
    }

    public void seek(double seconds) {
        if (seconds < 0) seconds = 0;

        if (isYouTubeCurrent()) {
            invokeIfExists(youTubePlayer, "seek", double.class, seconds);
        } else {
            if (localPlayer != null) localPlayer.seek(seconds);
        }
    }

    public void setVolume(double volume) {
        volume = Math.max(0.0, Math.min(1.0, volume));

        if (isYouTubeCurrent()) {
            invokeIfExists(youTubePlayer, "setVolume", double.class, volume);
        } else {
            if (localPlayer != null) localPlayer.setVolume(volume);
        }
    }

    public double getVolume() {
        if (!isYouTubeCurrent() && localPlayer != null) return localPlayer.getVolume();

        Object v = invokeIfExists(youTubePlayer, "getVolume");
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.5;
    }

    // ---------- Time getters ----------

    public Duration getCurrentTime() {
        if (!isYouTubeCurrent() && localPlayer != null) {
            return localPlayer.getCurrentTime();
        }
        Object v = invokeIfExists(youTubePlayer, "getCurrentTime");
        if (v instanceof Duration) return (Duration) v;
        return Duration.ZERO;
    }

    public long getCurrentTimeMillis() {
        return (long) getCurrentTime().toMillis();
    }

    // ---------- Modes ----------

    public void toggleShuffle() { shuffle = !shuffle; }
    public void setShuffle(boolean shuffle) { this.shuffle = shuffle; }
    public boolean isShuffle() { return shuffle; }

    public void cycleRepeatMode() {
        switch (repeatMode) {
            case OFF -> repeatMode = RepeatMode.ALL;
            case ALL -> repeatMode = RepeatMode.ONE;
            case ONE -> repeatMode = RepeatMode.OFF;
        }
    }

    public void setRepeatMode(RepeatMode mode) { this.repeatMode = mode; }
    public RepeatMode getRepeatMode() { return repeatMode; }

    // ---------- State ----------

    public PlayerState getState() { return state; }

    private void setState(PlayerState newState) {
        this.state = newState;
        runFx(() -> {
            if (onStateChanged != null) onStateChanged.accept(newState);
        });
    }

    public boolean isPlaying() { return state == PlayerState.PLAYING; }
    public boolean isPaused() { return state == PlayerState.PAUSED; }
    public boolean isStopped() { return state == PlayerState.STOPPED; }

    public Song getCurrentSong() { return currentSong; }

    // ---------- Callbacks ----------

    public void setOnTimeUpdate(Consumer<Duration> callback) { this.onTimeUpdate = callback; }
    public void setOnSongChanged(Consumer<Song> callback) { this.onSongChanged = callback; }
    public void setOnStateChanged(Consumer<PlayerState> callback) { this.onStateChanged = callback; }

    public void setOnPlaybackStarted(Runnable callback) { this.onPlaybackStarted = callback; }
    public void setOnPlaybackPaused(Runnable callback) { this.onPlaybackPaused = callback; }
    public void setOnPlaybackStopped(Runnable callback) { this.onPlaybackStopped = callback; }
    public void setOnSongEnded(Runnable callback) { this.onSongEnded = callback; }

    // ---------- Internals ----------

    private boolean isYouTubeCurrent() {
        return currentSong != null && currentSong.isYouTube();
    }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    private static Object invokeIfExists(Object target, String methodName, Class<?> paramType, Object arg) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName, paramType);
            m.setAccessible(true);
            return m.invoke(target, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeIfExists(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}