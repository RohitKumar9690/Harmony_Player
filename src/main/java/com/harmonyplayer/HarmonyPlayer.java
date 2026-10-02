package com.harmonyplayer;

import com.harmonyplayer.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HarmonyPlayer extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HarmonyPlayer - Ultimate Music Player");
        
        MainWindow mainWindow = new MainWindow(primaryStage);
        Scene scene = mainWindow.createScene();
        
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
        
        primaryStage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}