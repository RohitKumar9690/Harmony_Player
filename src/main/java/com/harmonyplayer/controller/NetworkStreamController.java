package com.harmonyplayer.controller;

import com.harmonyplayer.model.Song;
import com.harmonyplayer.service.WebStreamService;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class NetworkStreamController {

    private final WebStreamService service = new WebStreamService();
    private Consumer<String> onStatusUpdate;

    public void startServer() {
        service.startServer();
        status("Server: " + service.getConnectionURL());
    }

    public void stopServer() {
        service.stopServer();
        status("Server stopped");
    }

    public boolean isServerRunning() {
        return service.isStreaming();
    }

    public String getConnectionURL() { return service.getConnectionURL(); }
    public String getQRCodeURL() { return service.getQRCodeURL(); }
    public int getConnectedClientsCount() { return service.getConnectedClientsCount(); }
    public List<String> getConnectedDevices() { return service.getConnectedDevices(); }

    public void startStreaming(Song song) {
        if (song == null) { status("No song"); return; }

        if (!service.isStreaming()) startServer();

        if (song.isYouTube()) {
            status("Cannot stream YouTube directly. Download first.");
            return;
        }

        File f = new File(song.getFilePath());
        if (!f.isFile()) { status("File not found: " + song.getFilePath()); return; }

        service.setCurrentSong(song.getTitle(), song.getArtist());
        service.setAudioFile(f);
        service.resumeStreaming();

        status("Streaming: " + song.getTitle());
    }

    public void pauseStreaming() {
        service.stopStreaming();
        status("Streaming paused");
    }

    public void resumeStreaming() {
        service.resumeStreaming();
        status("Streaming resumed");
    }

    /** MainWindow must call this from time updates for sync */
    public void updatePlaybackPositionMs(long ms) {
        service.updatePlaybackPositionMs(ms);
    }

    public void setOnStatusUpdate(Consumer<String> cb) { this.onStatusUpdate = cb; }
    private void status(String s) { if (onStatusUpdate != null) onStatusUpdate.accept(s); }

    public void shutdown() { stopServer(); }
}