package com.harmonyplayer.model;

public class EqualizerPreset {
    private String name;
    private double[] bandValues; // 10 bands

    public EqualizerPreset(String name, double[] bandValues) {
        this.name = name;
        this.bandValues = bandValues;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double[] getBandValues() { return bandValues; }
    public void setBandValues(double[] bandValues) { this.bandValues = bandValues; }

    // Predefined presets
    public static EqualizerPreset flat() {
        return new EqualizerPreset("Flat", new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    }

    public static EqualizerPreset rock() {
        return new EqualizerPreset("Rock", new double[]{5, 3, -1, -2, 1, 2, 4, 5, 5, 5});
    }

    public static EqualizerPreset pop() {
        return new EqualizerPreset("Pop", new double[]{-1, 2, 4, 5, 3, 0, -1, -1, -1, 0});
    }

    public static EqualizerPreset jazz() {
        return new EqualizerPreset("Jazz", new double[]{3, 2, 1, 2, -1, -1, 0, 1, 2, 3});
    }

    public static EqualizerPreset classical() {
        return new EqualizerPreset("Classical", new double[]{4, 3, 2, 1, -1, -1, 0, 2, 3, 4});
    }

    public static EqualizerPreset bass() {
        return new EqualizerPreset("Bass Boost", new double[]{6, 5, 4, 2, 1, 0, 0, 0, 0, 0});
    }

    public static EqualizerPreset treble() {
        return new EqualizerPreset("Treble Boost", new double[]{0, 0, 0, 0, 1, 2, 4, 5, 6, 6});
    }

    public static EqualizerPreset vocal() {
        return new EqualizerPreset("Vocal", new double[]{-2, -1, 1, 3, 4, 4, 3, 1, 0, -1});
    }
}