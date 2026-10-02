package com.harmonyplayer.controller;

import com.harmonyplayer.model.Song;
import com.harmonyplayer.service.AudioEffectsService;
import com.harmonyplayer.service.LocalMusicPlayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DJController {
    
    private LocalMusicPlayer currentPlayer;
    private LocalMusicPlayer nextPlayer;
    private AudioEffectsService audioEffectsService;
    private ScheduledExecutorService scheduler;
    
    private boolean autoCrossfadeEnabled = false;
    private double crossfadeDuration = 3.0; // seconds
    private boolean beatMatchingEnabled = false;

    public DJController(AudioEffectsService audioEffectsService) {
        this.audioEffectsService = audioEffectsService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Enable automatic crossfade between songs
     */
    public void enableAutoCrossfade(boolean enable) {
        this.autoCrossfadeEnabled = enable;
    }

    /**
     * Set crossfade duration
     */
    public void setCrossfadeDuration(double seconds) {
        this.crossfadeDuration = Math.max(1.0, Math.min(10.0, seconds));
    }

    /**
     * Manually trigger crossfade to next song
     */
    public void crossfadeToNext(Song nextSong, Runnable onComplete) {
        if (currentPlayer == null) return;

        // Prepare next player
        nextPlayer = new LocalMusicPlayer();
        nextPlayer.play(nextSong);
        nextPlayer.setVolume(0);

        // Apply crossfade
        audioEffectsService.applyCrossfade(
            currentPlayer.getMediaPlayer(),
            nextPlayer.getMediaPlayer(),
            crossfadeDuration,
            () -> {
                // Swap players
                currentPlayer.stop();
                currentPlayer = nextPlayer;
                nextPlayer = null;
                
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        );
    }

    /**
     * Schedule auto-crossfade before song ends
     */
    public void scheduleAutoCrossfade(Song currentSong, Song nextSong, Runnable onComplete) {
        if (!autoCrossfadeEnabled || currentSong == null) return;

        long songDuration = currentSong.getDuration();
        long crossfadeStart = (long) (songDuration - crossfadeDuration);

        scheduler.schedule(
            () -> crossfadeToNext(nextSong, onComplete),
            crossfadeStart,
            TimeUnit.SECONDS
        );
    }

    /**
     * Enable beat matching (experimental)
     */
    public void enableBeatMatching(boolean enable) {
        this.beatMatchingEnabled = enable;
    }

    /**
     * Analyze BPM of song (simplified)
     */
    public double analyzeBPM(Song song) {
        // This would require complex audio analysis
        // Placeholder for now
        return 120.0; // Default BPM
    }

    /**
     * Sync playback speed to match BPM
     */
    public void syncBPM(double targetBPM, double currentBPM) {
        if (!beatMatchingEnabled) return;
        
        double rate = targetBPM / currentBPM;
        // Apply playback rate adjustment
        // Note: JavaFX MediaPlayer supports rate adjustment
    }

    public boolean isAutoCrossfadeEnabled() {
        return autoCrossfadeEnabled;
    }

    public double getCrossfadeDuration() {
        return crossfadeDuration;
    }

    public void setCurrentPlayer(LocalMusicPlayer player) {
        this.currentPlayer = player;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}