package com.harmonyplayer.ui;

import com.harmonyplayer.controller.EqualizerController;
import com.harmonyplayer.model.EqualizerPreset;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class EqualizerWindow extends Stage {
    
    private EqualizerController equalizerController;
    private Slider[] bandSliders;
    private Label[] bandLabels;
    private ComboBox<String> presetComboBox;
    private static final String[] BAND_FREQUENCIES = {
        "32Hz", "64Hz", "125Hz", "250Hz", "500Hz", 
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    };

    public EqualizerWindow(EqualizerController equalizerController) {
        this.equalizerController = equalizerController;
        this.bandSliders = new Slider[10];
        this.bandLabels = new Label[10];
        
        setTitle("Equalizer");
        setScene(createScene());
        setResizable(false);
    }

    private Scene createScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        Label title = new Label("AUDIO EQUALIZER");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        HBox presetBox = createPresetSelector();
        HBox bandsBox = createEqualizerBands();
        HBox controlsBox = createControlButtons();

        root.getChildren().addAll(title, presetBox, bandsBox, controlsBox);

        return new Scene(root, 800, 400);
    }

    private HBox createPresetSelector() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        Label label = new Label("Preset:");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font(14));

        presetComboBox = new ComboBox<>();
        presetComboBox.setPrefWidth(200);
        
        for (EqualizerPreset preset : equalizerController.getPresets()) {
            presetComboBox.getItems().add(preset.getName());
        }
        
        presetComboBox.setValue("Flat");
        presetComboBox.setOnAction(e -> {
            String selected = presetComboBox.getValue();
            equalizerController.applyPreset(selected);
            updateSlidersFromPreset(selected);
        });

        box.getChildren().addAll(label, presetComboBox);
        return box;
    }

    private HBox createEqualizerBands() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        for (int i = 0; i < 10; i++) {
            VBox bandBox = createBandControl(i);
            box.getChildren().add(bandBox);
        }

        return box;
    }

    private VBox createBandControl(int bandIndex) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);

        bandLabels[bandIndex] = new Label("0 dB");
        bandLabels[bandIndex].setTextFill(Color.WHITE);
        bandLabels[bandIndex].setFont(Font.font(10));

        Slider slider = new Slider(-12, 12, 0);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefHeight(200);
        slider.setMajorTickUnit(6);
        slider.setMinorTickCount(1);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(true);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = newVal.doubleValue();
            bandLabels[bandIndex].setText(String.format("%.1f dB", value));
            equalizerController.setBandValue(bandIndex, value);
            presetComboBox.setValue("Custom");
        });

        bandSliders[bandIndex] = slider;

        Label freqLabel = new Label(BAND_FREQUENCIES[bandIndex]);
        freqLabel.setTextFill(Color.WHITE);
        freqLabel.setFont(Font.font(11));

        box.getChildren().addAll(bandLabels[bandIndex], slider, freqLabel);
        return box;
    }

    private HBox createControlButtons() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));

        Button resetButton = new Button("Reset");
        resetButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        resetButton.setOnAction(e -> resetEqualizer());

        Button saveButton = new Button("Save Preset");
        saveButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        saveButton.setOnAction(e -> saveCustomPreset());

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        closeButton.setOnAction(e -> close());

        box.getChildren().addAll(resetButton, saveButton, closeButton);
        return box;
    }

    private void updateSlidersFromPreset(String presetName) {
        for (EqualizerPreset preset : equalizerController.getPresets()) {
            if (preset.getName().equals(presetName)) {
                double[] values = preset.getBandValues();
                for (int i = 0; i < Math.min(values.length, bandSliders.length); i++) {
                    bandSliders[i].setValue(values[i]);
                }
                break;
            }
        }
    }

    private void resetEqualizer() {
        equalizerController.resetEqualizer();
        for (Slider slider : bandSliders) {
            slider.setValue(0);
        }
        presetComboBox.setValue("Flat");
    }

    private void saveCustomPreset() {
        TextInputDialog dialog = new TextInputDialog("My Preset");
        dialog.setTitle("Save Preset");
        dialog.setHeaderText("Save Custom Equalizer Preset");
        dialog.setContentText("Preset name:");

        dialog.showAndWait().ifPresent(name -> {
            equalizerController.saveCustomPreset(name);
            presetComboBox.getItems().add(name);
            presetComboBox.setValue(name);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Preset Saved");
            alert.setContentText("Preset saved successfully!");
            alert.showAndWait();
        });
    }
}