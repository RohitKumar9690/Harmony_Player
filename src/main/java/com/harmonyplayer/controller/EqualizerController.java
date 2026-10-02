package com.harmonyplayer.controller;

import com.harmonyplayer.model.EqualizerPreset;
import com.harmonyplayer.service.AudioEffectsService;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EqualizerController {

    private final AudioEffectsService audioEffectsService;
    private MediaPlayer currentMediaPlayer;

    private EqualizerPreset currentPreset;
    private List<EqualizerPreset> presets;

    // Always keep these in sync with what is applied
    private final double[] customBands = new double[10];

    public EqualizerController(AudioEffectsService audioEffectsService) {
        this.audioEffectsService = audioEffectsService;
        initializePresets();
        // start with flat values
        System.arraycopy(currentPreset.getBandValues(), 0, customBands, 0, customBands.length);
    }

    private void initializePresets() {
        presets = new ArrayList<>();
        presets.add(EqualizerPreset.flat());
        presets.add(EqualizerPreset.rock());
        presets.add(EqualizerPreset.pop());
        presets.add(EqualizerPreset.jazz());
        presets.add(EqualizerPreset.classical());
        presets.add(EqualizerPreset.bass());
        presets.add(EqualizerPreset.treble());
        presets.add(EqualizerPreset.vocal());

        currentPreset = presets.get(0); // Flat
    }

    /** MUST be called whenever a new song creates a new MediaPlayer */
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.currentMediaPlayer = mediaPlayer;
        applyCurrentPreset();
    }

    public void applyPreset(EqualizerPreset preset) {
        if (preset == null) return;

        this.currentPreset = preset;

        // IMPORTANT: keep customBands synced to preset so sliders show correct values
        double[] values = preset.getBandValues();
        if (values != null) {
            for (int i = 0; i < customBands.length && i < values.length; i++) {
                customBands[i] = values[i];
            }
        }

        applyCurrentPreset();
    }

    public void applyPreset(String presetName) {
        if (presetName == null) return;
        for (EqualizerPreset preset : presets) {
            if (preset.getName().equalsIgnoreCase(presetName)) {
                applyPreset(preset);
                return;
            }
        }
    }

    private void applyCurrentPreset() {
        if (currentMediaPlayer == null) return;

        // Apply the currently selected preset (or current customBands)
        double[] valuesToApply = (currentPreset != null && currentPreset.getBandValues() != null)
                ? currentPreset.getBandValues()
                : customBands;

        audioEffectsService.applyEqualizer(currentMediaPlayer, valuesToApply);
    }

    public void setBandValue(int bandIndex, double value) {
        if (bandIndex < 0 || bandIndex >= customBands.length) return;

        customBands[bandIndex] = value;

        // Switch to "Custom" behavior: keep preset name but apply custom array
        if (currentMediaPlayer != null) {
            audioEffectsService.applyEqualizer(currentMediaPlayer, customBands);
        }
    }

    public void saveCustomPreset(String name) {
        if (name == null || name.isBlank()) name = "Custom";
        EqualizerPreset custom = new EqualizerPreset(name, Arrays.copyOf(customBands, customBands.length));
        presets.add(custom);
        currentPreset = custom;
    }

    public void resetEqualizer() {
        applyPreset(EqualizerPreset.flat());
    }

    public List<EqualizerPreset> getPresets() {
        return new ArrayList<>(presets);
    }

    public EqualizerPreset getCurrentPreset() {
        return currentPreset;
    }

    public double[] getCurrentBandValues() {
        // Return what we are actually using for sliders
        return Arrays.copyOf(customBands, customBands.length);
    }
}