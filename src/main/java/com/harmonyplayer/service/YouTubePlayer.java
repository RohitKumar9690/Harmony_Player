package com.harmonyplayer.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.harmonyplayer.model.Song;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class YouTubePlayer {
    private YoutubeDownloader downloader;
    private LocalMusicPlayer localPlayer;
    private File tempDir;

    public YouTubePlayer(LocalMusicPlayer localPlayer) {
        this.downloader = new YoutubeDownloader();
        this.localPlayer = localPlayer;
        this.tempDir = new File(System.getProperty("java.io.tmpdir"), "HarmonyPlayer");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    public void playYouTubeVideo(Song song, Runnable onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                String videoId = extractVideoId(song.getYoutubeUrl());
                File outputFile = new File(tempDir, videoId + ".mp4");
                
                // If already downloaded, play directly
                if (outputFile.exists()) {
                    song.setFilePath(outputFile.getAbsolutePath());
                    localPlayer.play(song);
                    if (onSuccess != null) {
                        javafx.application.Platform.runLater(onSuccess);
                    }
                    return;
                }
                
                RequestVideoInfo request = new RequestVideoInfo(videoId);
                Response<VideoInfo> response = downloader.getVideoInfo(request);
                VideoInfo video = response.data();
                
                List<AudioFormat> audioFormats = video.audioFormats();
                if (audioFormats.isEmpty()) {
                    if (onError != null) {
                        javafx.application.Platform.runLater(() -> 
                            onError.accept("No audio format available"));
                    }
                    return;
                }
                
                AudioFormat bestAudio = audioFormats.get(0);
                
                RequestVideoFileDownload downloadRequest = 
                    new RequestVideoFileDownload(bestAudio)
                        .saveTo(tempDir)
                        .renameTo(videoId)
                        .overwriteIfExists(true)
                        .callback(new YoutubeCallback<File>() {
                            @Override
                            public void onFinished(File file) {
                                song.setFilePath(file.getAbsolutePath());
                                localPlayer.play(song);
                                if (onSuccess != null) {
                                    javafx.application.Platform.runLater(onSuccess);
                                }
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                if (onError != null) {
                                    javafx.application.Platform.runLater(() -> 
                                        onError.accept(throwable.getMessage()));
                                }
                            }
                        });
                
                downloader.downloadVideoFile(downloadRequest);
                
            } catch (Exception e) {
                e.printStackTrace();
                if (onError != null) {
                    javafx.application.Platform.runLater(() -> 
                        onError.accept("Error: " + e.getMessage()));
                }
            }
        }).start();
    }

    private String extractVideoId(String url) {
        String videoId = url;
        if (url.contains("youtube.com/watch?v=")) {
            videoId = url.split("v=")[1].split("&")[0];
        } else if (url.contains("youtu.be/")) {
            videoId = url.split("youtu.be/")[1].split("\\?")[0];
        }
        return videoId;
    }

    public void cleanup() {
        if (tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }
}