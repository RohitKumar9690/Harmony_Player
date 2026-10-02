package com.harmonyplayer.ui;

import com.harmonyplayer.controller.NetworkStreamController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DeviceManagerDialog extends Stage {
    
    private NetworkStreamController networkController;
    private ListView<String> connectedDevicesList;
    private Label statusLabel;
    private Label urlLabel;
    private Button startServerButton;
    private ImageView qrCodeView;
    private Label clientCountLabel;

    public DeviceManagerDialog(NetworkStreamController networkController) {
        this.networkController = networkController;
        
        setTitle("🌐 Multi-Device Web Streaming");
        setScene(createScene());
        setWidth(700);
        setHeight(600);

        networkController.setOnStatusUpdate(status -> {
            Platform.runLater(() -> statusLabel.setText(status));
        });
        
        // Update client count periodically
        startClientCountUpdater();
    }

    private Scene createScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Header
        VBox header = createHeader();
        
        // Server controls
        HBox serverBox = createServerControls();
        
        // Connection info
        VBox connectionBox = createConnectionInfo();
        
        // QR Code
        VBox qrBox = createQRCodeBox();
        
        // Connected devices
        VBox devicesBox = createDevicesList();
        
        // Instructions
        VBox instructions = createInstructions();

        root.getChildren().addAll(header, serverBox, connectionBox, qrBox, devicesBox, instructions);
        return new Scene(root);
    }

    private VBox createHeader() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        
        Label title = new Label("🌐 Multi-Device Web Streaming");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        
        Label subtitle = new Label("No app installation required - Just open a browser!");
        subtitle.setFont(Font.font(14));
        subtitle.setTextFill(Color.web("#b8b8b8"));
        
        statusLabel = new Label("Ready to start");
        statusLabel.setTextFill(Color.web("#4caf50"));
        statusLabel.setFont(Font.font(12));
        
        box.getChildren().addAll(title, subtitle, statusLabel);
        return box;
    }

    private HBox createServerControls() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10;");

        startServerButton = new Button("🚀 Start Server");
        startServerButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                                  "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30;");
        startServerButton.setOnAction(e -> toggleServer());
        
        clientCountLabel = new Label("📱 0 devices connected");
        clientCountLabel.setTextFill(Color.WHITE);
        clientCountLabel.setFont(Font.font(14));

        box.getChildren().addAll(startServerButton, clientCountLabel);
        return box;
    }

    private VBox createConnectionInfo() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: rgba(50, 130, 184, 0.2); -fx-background-radius: 10;");

        Label infoLabel = new Label("📍 Connection URL:");
        infoLabel.setTextFill(Color.WHITE);
        infoLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        urlLabel = new Label("Server not started");
        urlLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        urlLabel.setTextFill(Color.web("#3282b8"));
        urlLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                         "-fx-padding: 10; -fx-background-radius: 5;");

        Button copyButton = new Button("📋 Copy URL");
        copyButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        copyButton.setOnAction(e -> copyURLToClipboard());

        box.getChildren().addAll(infoLabel, urlLabel, copyButton);
        return box;
    }

    private VBox createQRCodeBox() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10;");

        Label qrLabel = new Label("📱 Scan to Connect:");
        qrLabel.setTextFill(Color.WHITE);
        qrLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        qrCodeView = new ImageView();
        qrCodeView.setFitWidth(200);
        qrCodeView.setFitHeight(200);
        qrCodeView.setPreserveRatio(true);
        qrCodeView.setStyle("-fx-background-color: white; -fx-padding: 10;");

        box.getChildren().addAll(qrLabel, qrCodeView);
        box.setVisible(false);
        return box;
    }

    private VBox createDevicesList() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label label = new Label("✅ Connected Devices:");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setTextFill(Color.WHITE);

        connectedDevicesList = new ListView<>();
        connectedDevicesList.setPrefHeight(150);
        connectedDevicesList.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");

        box.getChildren().addAll(label, connectedDevicesList);
        return box;
    }

    private VBox createInstructions() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10;");
        
        Label title = new Label("📖 How to Connect:");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);
        
        String[] steps = {
            "1️⃣ Click 'Start Server' button above",
            "2️⃣ Open web browser on any device (phone, tablet, computer)",
            "3️⃣ Enter the URL shown above OR scan the QR code",
            "4️⃣ Start playing music - it will stream to all connected devices!",
            "5️⃣ No app installation needed!"
        };
        
        VBox stepsBox = new VBox(5);
        for (String step : steps) {
            Label stepLabel = new Label(step);
            stepLabel.setTextFill(Color.web("#b8b8b8"));
            stepLabel.setFont(Font.font(12));
            stepLabel.setWrapText(true);
            stepsBox.getChildren().add(stepLabel);
        }
        
        box.getChildren().addAll(title, stepsBox);
        return box;
    }

    private void toggleServer() {
        if (networkController.isServerRunning()) {
            networkController.stopServer();
            startServerButton.setText("🚀 Start Server");
            startServerButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                                      "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30;");
            urlLabel.setText("Server stopped");
            qrCodeView.getParent().setVisible(false);
        } else {
            networkController.startServer();
            startServerButton.setText("⏹ Stop Server");
            startServerButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                                      "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30;");
            
            String url = networkController.getConnectionURL();
            urlLabel.setText(url);
            
            // Load QR code
            loadQRCode();
            qrCodeView.getParent().setVisible(true);
        }
    }

    private void loadQRCode() {
        new Thread(() -> {
            try {
                String qrUrl = networkController.getQRCodeURL();
                Image qrImage = new Image(qrUrl);
                Platform.runLater(() -> qrCodeView.setImage(qrImage));
            } catch (Exception e) {
                System.err.println("Failed to load QR code");
            }
        }).start();
    }

    private void copyURLToClipboard() {
        String url = networkController.getConnectionURL();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(url);
        clipboard.setContent(content);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Copied");
        alert.setHeaderText(null);
        alert.setContentText("URL copied to clipboard!\n" + url);
        alert.showAndWait();
    }

    private void startClientCountUpdater() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                int count = networkController.getConnectedClientsCount();
                clientCountLabel.setText("📱 " + count + " device" + (count != 1 ? "s" : "") + " connected");
                
                // Update device list
                connectedDevicesList.getItems().setAll(networkController.getConnectedDevices());
            })
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
}