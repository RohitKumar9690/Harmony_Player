package com.harmonyplayer.model;

public class LyricLine implements Comparable<LyricLine> {
    private long timestamp; // milliseconds
    private String text;
    private boolean isActive;

    public LyricLine(long timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
        this.isActive = false;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public double getTimeInSeconds() {
        return timestamp / 1000.0;
    }

    @Override
    public int compareTo(LyricLine other) {
        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public String toString() {
        return formatTimestamp() + " " + text;
    }

    public String formatTimestamp() {
        long minutes = timestamp / 60000;
        long seconds = (timestamp % 60000) / 1000;
        long millis = (timestamp % 1000) / 10;
        return String.format("[%02d:%02d.%02d]", minutes, seconds, millis);
    }

    /**
     * Parse LRC format line: [mm:ss.xx]lyrics
     */
    public static LyricLine parseLRC(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        line = line.trim();
        if (!line.startsWith("[") || !line.contains("]")) {
            return null;
        }

        try {
            int endBracket = line.indexOf("]");
            String timeStr = line.substring(1, endBracket);
            String text = line.substring(endBracket + 1).trim();

            // Parse time [mm:ss.xx]
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                return null;
            }

            int minutes = Integer.parseInt(parts[0]);
            String[] secParts = parts[1].split("\\.");
            int seconds = Integer.parseInt(secParts[0]);
            int centiseconds = secParts.length > 1 ? Integer.parseInt(secParts[1]) : 0;

            long timestamp = (minutes * 60000L) + (seconds * 1000L) + (centiseconds * 10L);
            return new LyricLine(timestamp, text);

        } catch (Exception e) {
            return null;
        }
    }
}