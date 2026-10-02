package com.harmonyplayer.ui;

import com.harmonyplayer.model.Song;
import com.harmonyplayer.service.LyricsService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LyricsEditorDialog extends Stage {
    
    private Song song;
    private LyricsService lyricsService;
    private TextArea lyricsTextArea;
    private Runnable onSave;

    public LyricsEditorDialog(Song song, LyricsService lyricsService, Runnable onSave) {
        this.song = song;
        this.lyricsService = lyricsService;
        this.onSave = onSave;
        
        setTitle("Edit Lyrics - " + song.getTitle());
        setScene(createScene());
        setWidth(600);
        setHeight(500);
    }

    private Scene createScene() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label title = new Label("Edit Lyrics");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        Label songInfo = new Label(song.getTitle() + " - " + song.getArtist());
        songInfo.setFont(Font.font(14));
        songInfo.setTextFill(Color.web("#b8b8b8"));

        lyricsTextArea = new TextArea();
        lyricsTextArea.setWrapText(true);
        lyricsTextArea.setPrefHeight(300);
        lyricsTextArea.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 14px;");
        
        String existingLyrics = song.getLyrics();
        if (existingLyrics == null || existingLyrics.isEmpty()) {
            existingLyrics = lyricsService.getCachedLyrics(song.getArtist(), song.getTitle());
        }
        if (existingLyrics != null) {
            lyricsTextArea.setText(existingLyrics);
        }

        HBox buttonBox = createButtons();

        VBox.setVgrow(lyricsTextArea, Priority.ALWAYS);
        root.getChildren().addAll(title, songInfo, lyricsTextArea, buttonBox);

        return new Scene(root);
    }

    private HBox createButtons() {
        HBox box = new HBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER);

        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        saveButton.setOnAction(e -> saveLyrics());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        cancelButton.setOnAction(e -> close());

        Button clearButton = new Button("Clear");
        clearButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        clearButton.setOnAction(e -> lyricsTextArea.clear());

        box.getChildren().addAll(saveButton, clearButton, cancelButton);
        return box;
    }

    private void saveLyrics() {
        String lyrics = lyricsTextArea.getText();
        lyricsService.saveCustomLyrics(song, lyrics);
        
        if (onSave != null) {
            onSave.run();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Lyrics saved successfully!");
        alert.showAndWait();

        close();
    }
}