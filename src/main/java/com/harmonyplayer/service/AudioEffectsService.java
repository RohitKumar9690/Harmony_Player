package com.harmonyplayer.service;

import javafx.application.Platform;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

import java.util.Arrays;

public class AudioEffectsService {

    private static final int NUM_BANDS = 10;

    // Standard 10-band equalizer frequencies (Hz)
    private static final double[] FREQS = {32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    private double[] currentBands = new double[NUM_BANDS];

    /**
     * Apply equalizer to media player (10-band).
     * bandValues are gains in dB. Will be clamped to JavaFX allowed range (-24..+12).
     */
    public void applyEqualizer(MediaPlayer mediaPlayer, double[] bandValues) {
        if (mediaPlayer == null) return;

        AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
        eq.setEnabled(true);

        ensureTenBands(eq);

        double[] normalized = normalizeBands(bandValues);

        for (int i = 0; i < NUM_BANDS; i++) {
            EqualizerBand b = eq.getBands().get(i);
            b.setGain(normalized[i]);
        }

        // store a copy (not the same reference)
        currentBands = Arrays.copyOf(normalized, NUM_BANDS);
    }

    /** Reset equalizer to flat */
    public void resetEqualizer(MediaPlayer mediaPlayer) {
        applyEqualizer(mediaPlayer, new double[NUM_BANDS]);
    }

    /** Returns a copy of current band gains */
    public double[] getCurrentBands() {
        return Arrays.copyOf(currentBands, currentBands.length);
    }

    /** Optional helper if you still need this elsewhere */
    public static double getFrequencyForBand(int bandIndex) {
        if (bandIndex >= 0 && bandIndex < FREQS.length) return FREQS[bandIndex];
        return 0;
    }

    // -------------------- Crossfade --------------------

    public void applyCrossfade(MediaPlayer currentPlayer,
                               MediaPlayer nextPlayer,
                               double durationSeconds,
                               Runnable onComplete) {

        if (currentPlayer == null) return;
        if (durationSeconds <= 0) durationSeconds = 0.1;

        final double startVolume = currentPlayer.getVolume();
        final int steps = 30;
        final long stepMs = (long) ((durationSeconds * 1000.0) / steps);

        Thread t = new Thread(() -> {
            try {
                for (int i = 0; i <= steps; i++) {
                    final double t01 = i / (double) steps;

                    final double fadeOut = startVolume * (1.0 - t01);
                    final double fadeIn = startVolume * (t01);

                    Platform.runLater(() -> {
                        currentPlayer.setVolume(Math.max(0, fadeOut));
                        if (nextPlayer != null) {
                            nextPlayer.setVolume(Math.min(startVolume, fadeIn));
                        }
                    });

                    Thread.sleep(Math.max(5, stepMs));
                }

                if (onComplete != null) Platform.runLater(onComplete);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "crossfade-thread");

        t.setDaemon(true);
        t.start();
    }

    // -------------------- Internal helpers --------------------

    private void ensureTenBands(AudioEqualizer eq) {
        // Always rebuild to guarantee known frequencies/band count
        eq.getBands().clear();

        for (double f : FREQS) {
            // bandwidth: choose something reasonable. (JavaFX examples often use 1.0, but that’s very narrow.)
            // A practical simple bandwidth is half the frequency (wide enough for audible effect).
            double bandwidth = Math.max(50, f * 0.5);
            eq.getBands().add(new EqualizerBand(f, bandwidth, 0.0));
        }
    }

    private double[] normalizeBands(double[] bandValues) {
        double[] out = new double[NUM_BANDS];

        for (int i = 0; i < NUM_BANDS; i++) {
            double v = 0.0;
            if (bandValues != null && i < bandValues.length) v = bandValues[i];

            // Clamp to JavaFX allowed range
            v = Math.max(EqualizerBand.MIN_GAIN, Math.min(EqualizerBand.MAX_GAIN, v));
            out[i] = v;
        }
        return out;
    }
}