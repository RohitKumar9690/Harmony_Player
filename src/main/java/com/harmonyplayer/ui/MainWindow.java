package com.harmonyplayer.ui;

import com.harmonyplayer.controller.*;
import com.harmonyplayer.model.Song;
import com.harmonyplayer.model.SynchronizedLyrics;
import com.harmonyplayer.service.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainWindow {
    private Stage primaryStage;
    private BorderPane rootPane;

    private MainController mainController;
    private LyricsService lyricsService;
    private AlbumArtService albumArtService;
    private AudioEffectsService audioEffectsService;
    private EqualizerController equalizerController;
    private DJController djController;
    private NetworkStreamController networkStreamController;

    // UI Components
    private VBox playlistPanel;
    private ListView<Song> playlistView;

    private Label nowPlayingLabel;
    private Label artistLabel;
    private Label albumLabel;
    private Label timeLabel;
    private Slider progressSlider;
    private Slider volumeSlider;

    // Keep references so we can recolor them when theme changes
    private Button prevButton;
    private Button nextButton;
    private Button stopButton;

    private Button playPauseButton;
    private Button shuffleButton;
    private Button repeatButton;

    private TextArea lyricsArea;
    private Label statusLabel;
    private ImageView albumArtView;
    private Label bitrateLabel;
    private ProgressIndicator loadingIndicator;

    // DJ Mode components
    private CheckBox autoCrossfadeCheck;
    private Slider crossfadeSlider;
    private VBox djControlsBox;

    // Mode buttons
    private Button equalizerButton;
    private ToggleButton djModeButton;
    private Button networkStreamButton;
    private Button editLyricsButton;

    // Playlist toggle button
    private ToggleButton playlistToggleButton;
    private boolean playlistVisible = true;

    // Lyrics panel enhanced
    private ToggleButton syncToggleButton;
    private StackPane lyricsStack;
    private VBox lyricsEmptyOverlay;
    private Label lyricsEmptyTitle;
    private Label lyricsEmptySubtitle;
    private Button lyricsEmptyAddButton;
    private Button lyricsEmptyRefreshButton;

    // Lyrics gradient background (NEW)
    private Region lyricsGradientBg;

    // Synchronized Lyrics components
    private SyncLyricsPanel syncLyricsPanel;
    private SynchronizedLyrics currentSyncLyrics;
    private boolean useSyncLyrics = false;

    private boolean isDraggingSlider = false;

    // -------- Theme --------
    private static final Color DEFAULT_BASE = Color.web("#16213e");
    private static final Color DEFAULT_ACCENT = Color.web("#0f4c75");
    private Color currentAccent = DEFAULT_ACCENT;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.mainController = new MainController();
        this.lyricsService = mainController.getLyricsService();
        this.albumArtService = new AlbumArtService();
        this.audioEffectsService = new AudioEffectsService();
        this.equalizerController = new EqualizerController(audioEffectsService);
        this.djController = new DJController(audioEffectsService);
        this.networkStreamController = new NetworkStreamController();

        setupControllerCallbacks();
    }

    private void setupControllerCallbacks() {
        mainController.setOnStatusUpdate(status -> Platform.runLater(() -> {
            if (statusLabel != null)
                statusLabel.setText(status);
        }));

        mainController.setOnSongChanged(song -> Platform.runLater(() -> {
            updateNowPlaying(song);

            // album art + gradient theme
            loadAlbumArt(song);

            // Always load plain lyrics
            loadLyrics();

            // Always attempt sync lyrics; auto-switch if available
            loadSynchronizedLyrics(true);
        }));

        PlayerController playerController = mainController.getPlayerController();

        playerController.setOnTimeUpdate(duration -> {
            if (networkStreamController != null && networkStreamController.isServerRunning()) {
                networkStreamController.updatePlaybackPositionMs((long) duration.toMillis());
            }
            if (!isDraggingSlider)
                Platform.runLater(() -> updateProgressBar(duration));
        });

        playerController.setOnPlaybackStarted(() -> Platform.runLater(() -> {
            if (playPauseButton != null) {
                playPauseButton.setText("⏸");
            }
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }

            // IMPORTANT: set MediaPlayer for equalizer AFTER a song creates a new
            // MediaPlayer
            equalizerController.setMediaPlayer(
                    mainController.getPlayerController().getActiveMediaPlayer());
        }));

        playerController.setOnPlaybackPaused(() -> Platform.runLater(() -> {
            if (playPauseButton != null)
                playPauseButton.setText("▶");
        }));

        playerController.setOnPlaybackStopped(() -> Platform.runLater(() -> {
            if (playPauseButton != null)
                playPauseButton.setText("▶");
            if (progressSlider != null)
                progressSlider.setValue(0);
            if (timeLabel != null)
                timeLabel.setText("0:00 / 0:00");
            if (syncLyricsPanel != null)
                syncLyricsPanel.reset();
        }));

        playerController.setOnSongEnded(() -> Platform.runLater(this::playNext));

        networkStreamController.setOnStatusUpdate(status -> Platform.runLater(() -> {
            if (statusLabel != null)
                statusLabel.setText("Network: " + status);
        }));
    }

    public Scene createScene() {
        rootPane = new BorderPane();
        applyThemeFromBase(DEFAULT_BASE); // apply default gradient + accent

        rootPane.setTop(createMenuBar());

        playlistPanel = createPlaylistPanel();
        rootPane.setLeft(playlistPanel);

        rootPane.setCenter(createPlayerPanel());
        rootPane.setRight(createLyricsPanel());
        rootPane.setBottom(createStatusBar());

        return new Scene(rootPane, 1400, 800);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");

        Menu fileMenu = new Menu("File");
        MenuItem addLocal = new MenuItem("Add Local Music");
        MenuItem addFolder = new MenuItem("Add Folder");
        MenuItem addYouTube = new MenuItem("Add from YouTube");
        MenuItem clearPlaylist = new MenuItem("Clear Playlist");
        MenuItem exit = new MenuItem("Exit");

        addLocal.setOnAction(e -> addLocalMusic());
        addFolder.setOnAction(e -> addFolderMusic());
        addYouTube.setOnAction(e -> showYouTubeDialog());
        clearPlaylist.setOnAction(e -> mainController.clearPlaylist());
        exit.setOnAction(e -> {
            shutdown();
            Platform.exit();
        });

        fileMenu.getItems().addAll(addLocal, addFolder, addYouTube,
                new SeparatorMenuItem(), clearPlaylist,
                new SeparatorMenuItem(), exit);

        Menu toolsMenu = new Menu("Tools");
        MenuItem equalizerItem = new MenuItem("Equalizer");
        MenuItem djModeItem = new MenuItem("DJ Mode");
        MenuItem networkStreamItem = new MenuItem("Multi-Device Streaming");
        MenuItem clearLyricsCache = new MenuItem("Clear Lyrics Cache");

        equalizerItem.setOnAction(e -> showEqualizerWindow());
        djModeItem.setOnAction(e -> toggleDJMode());
        networkStreamItem.setOnAction(e -> showNetworkStreamDialog());
        clearLyricsCache.setOnAction(e -> lyricsService.clearCache());

        toolsMenu.getItems().addAll(equalizerItem, djModeItem, networkStreamItem,
                new SeparatorMenuItem(), clearLyricsCache);

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);
        return menuBar;
    }

    // ================= Playlist Panel =================

    private VBox createPlaylistPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05);");

        Label title = new Label("PLAYLIST");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);

        TextField searchField = new TextField();
        searchField.setPromptText("Search songs...");
        searchField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white;");
        searchField.textProperty().addListener((obs, old, newVal) -> filterPlaylist(newVal));

        playlistView = new ListView<>();
        playlistView.setItems(mainController.getObservableSongs());
        playlistView.setPrefHeight(500);
        playlistView.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-border-color: rgba(255, 255, 255, 0.1);");

        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected != null)
                    playSong(selected);
            }
        });

        HBox playlistControls = new HBox(10);
        playlistControls.setAlignment(Pos.CENTER);

        Button removeButton = new Button("Remove");
        removeButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");
        removeButton.setOnAction(e -> removeSelectedSong());

        Button clearButton = new Button("Clear All");
        clearButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearButton.setOnAction(e -> confirmClearPlaylist());

        playlistControls.getChildren().addAll(removeButton, clearButton);

        VBox.setVgrow(playlistView, Priority.ALWAYS);
        panel.getChildren().addAll(title, searchField, playlistView, playlistControls);
        return panel;
    }

    // ================= Player Panel =================

    private VBox createPlayerPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setAlignment(Pos.CENTER);

        VBox albumArtBox = new VBox(10);
        albumArtBox.setAlignment(Pos.CENTER);

        albumArtView = new ImageView();
        albumArtView.setFitWidth(280);
        albumArtView.setFitHeight(280);
        albumArtView.setPreserveRatio(true);
        albumArtView.setImage(albumArtService.getDefaultAlbumArt());
        albumArtView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.2, 0, 6);");

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(50, 50);

        StackPane artStack = new StackPane(albumArtView, loadingIndicator);
        albumArtBox.getChildren().add(artStack);

        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setMaxWidth(520);

        nowPlayingLabel = new Label("No song playing");
        nowPlayingLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        nowPlayingLabel.setTextFill(Color.WHITE);
        nowPlayingLabel.setWrapText(true);

        artistLabel = new Label("");
        artistLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        artistLabel.setTextFill(Color.web("#b8b8b8"));

        albumLabel = new Label("");
        albumLabel.setFont(Font.font("System", 14));
        albumLabel.setTextFill(Color.web("#888"));

        bitrateLabel = new Label("");
        bitrateLabel.setFont(Font.font("System", 11));
        bitrateLabel.setTextFill(Color.web("#aaa"));

        infoBox.getChildren().addAll(nowPlayingLabel, artistLabel, albumLabel, bitrateLabel);

        // Progress slider in SECONDS
        VBox progressBox = new VBox(8);
        progressSlider = new Slider(0, 1, 0);
        progressSlider.setPrefWidth(520);
        progressSlider.setStyle("-fx-accent: " + toRgba(currentAccent, 1.0) + ";");

        progressSlider.setOnMousePressed(e -> isDraggingSlider = true);
        progressSlider.setOnMouseReleased(e -> {
            isDraggingSlider = false;
            mainController.getPlayerController().seek(progressSlider.getValue()); // seconds
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setTextFill(Color.WHITE);
        timeLabel.setFont(Font.font(13));

        progressBox.getChildren().addAll(progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        // Main controls
        HBox mainControls = new HBox(18);
        mainControls.setAlignment(Pos.CENTER);

        prevButton = new Button("⏮");
        playPauseButton = new Button("▶");
        nextButton = new Button("⏭");
        stopButton = new Button("⏹");

        playPauseButton.setPrefSize(70, 70);
        playPauseButton.setFont(Font.font(24));

        prevButton.setOnAction(e -> playPrevious());
        playPauseButton.setOnAction(e -> togglePlayPause());
        nextButton.setOnAction(e -> playNext());
        stopButton.setOnAction(e -> stop());

        styleControlButton(prevButton, 30);
        styleControlButton(playPauseButton, 30);
        styleControlButton(nextButton, 30);
        styleControlButton(stopButton, 30);

        mainControls.getChildren().addAll(prevButton, playPauseButton, nextButton, stopButton);

        // Secondary controls
        HBox secondaryControls = new HBox(12);
        secondaryControls.setAlignment(Pos.CENTER);

        playlistToggleButton = new ToggleButton("📃");
        playlistToggleButton.setSelected(true);
        playlistToggleButton.setOnAction(e -> setPlaylistVisible(playlistToggleButton.isSelected()));

        shuffleButton = new Button("🔀");
        repeatButton = new Button("🔁");
        equalizerButton = new Button("🎚");
        djModeButton = new ToggleButton("🎧");
        networkStreamButton = new Button("🔊");

        shuffleButton.setOnAction(e -> toggleShuffle());
        repeatButton.setOnAction(e -> toggleRepeat());
        equalizerButton.setOnAction(e -> showEqualizerWindow());
        djModeButton.setOnAction(e -> toggleDJMode());
        networkStreamButton.setOnAction(e -> showNetworkStreamDialog());

        styleModeButton(playlistToggleButton);
        styleModeButton(shuffleButton);
        styleModeButton(repeatButton);
        styleModeButton(equalizerButton);
        styleModeButton(djModeButton);
        styleModeButton(networkStreamButton);

        secondaryControls.getChildren().addAll(
                playlistToggleButton,
                shuffleButton, repeatButton, equalizerButton, djModeButton, networkStreamButton);

        djControlsBox = createDJControls();
        djControlsBox.setVisible(false);
        djControlsBox.setManaged(false);

        // Volume
        HBox volumeBox = new HBox(15);
        volumeBox.setAlignment(Pos.CENTER);

        Label volumeIcon = new Label("🔊");
        volumeIcon.setFont(Font.font(20));
        volumeIcon.setTextFill(Color.WHITE);

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(220);
        volumeSlider.setStyle("-fx-accent: " + toRgba(currentAccent, 1.0) + ";");
        volumeSlider.valueProperty().addListener(
                (obs, old, newVal) -> mainController.getPlayerController().setVolume(newVal.doubleValue()));

        Label volumePercent = new Label("50%");
        volumePercent.setTextFill(Color.WHITE);
        volumePercent.setMinWidth(45);
        volumeSlider.valueProperty().addListener(
                (obs, old, newVal) -> volumePercent.setText(String.format("%.0f%%", newVal.doubleValue() * 100)));

        volumeBox.getChildren().addAll(volumeIcon, volumeSlider, volumePercent);

        panel.getChildren().addAll(albumArtBox, infoBox, progressBox, mainControls,
                secondaryControls, djControlsBox, volumeBox);
        return panel;
    }

    private VBox createDJControls() {
        VBox djBox = new VBox(10);
        djBox.setAlignment(Pos.CENTER);
        djBox.setPadding(new Insets(15));
        djBox.setStyle("-fx-background-color: rgba(255, 69, 0, 0.2); -fx-background-radius: 10;");

        Label djLabel = new Label("DJ MODE ACTIVE");
        djLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        djLabel.setTextFill(Color.web("#ffb39c"));

        HBox crossfadeBox = new HBox(10);
        crossfadeBox.setAlignment(Pos.CENTER);

        autoCrossfadeCheck = new CheckBox("Auto Crossfade");
        autoCrossfadeCheck.setTextFill(Color.WHITE);
        autoCrossfadeCheck.setOnAction(e -> djController.enableAutoCrossfade(autoCrossfadeCheck.isSelected()));

        Label cfLabel = new Label("Duration:");
        cfLabel.setTextFill(Color.WHITE);

        crossfadeSlider = new Slider(1, 10, 3);
        crossfadeSlider.setPrefWidth(150);

        Label cfValue = new Label("3s");
        cfValue.setTextFill(Color.WHITE);
        cfValue.setMinWidth(30);

        crossfadeSlider.valueProperty().addListener((obs, old, newVal) -> {
            cfValue.setText(String.format("%.0fs", newVal.doubleValue()));
            djController.setCrossfadeDuration(newVal.doubleValue());
        });

        crossfadeBox.getChildren().addAll(autoCrossfadeCheck, cfLabel, crossfadeSlider, cfValue);
        djBox.getChildren().addAll(djLabel, crossfadeBox);

        return djBox;
    }

    // ================= Lyrics Panel (UPDATED with gradient) =================

    private VBox createLyricsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(380);
        panel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05);");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("LYRICS");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        syncToggleButton = new ToggleButton("Sync");
        syncToggleButton.setDisable(true);
        syncToggleButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white;");
        syncToggleButton.setOnAction(e -> toggleLyricsMode(syncToggleButton.isSelected(), false));

        editLyricsButton = new Button("Add/Edit");
        editLyricsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        editLyricsButton.setOnAction(e -> showLyricsEditor());

        headerBox.getChildren().addAll(title, spacer, syncToggleButton, editLyricsButton);

        // TextArea must be transparent so gradient background is visible
        lyricsArea = new TextArea();
        lyricsArea.setEditable(false);
        lyricsArea.setWrapText(true);
        lyricsArea.setPrefHeight(600);
        lyricsArea.setText("Lyrics will appear here...");
        lyricsArea.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-highlight-fill: " + toRgba(currentAccent, 0.60) + ";" +
                        "-fx-highlight-text-fill: white;");
        lyricsArea.setBackground(Background.EMPTY);
        lyricsArea.skinProperty().addListener((obs, old, skin) -> Platform.runLater(() -> {
            Region content = (Region) lyricsArea.lookup(".content");
            if (content != null)
                content.setBackground(Background.EMPTY);
        }));

        syncLyricsPanel = new SyncLyricsPanel();
        syncLyricsPanel.setPrefHeight(600);
        syncLyricsPanel.setVisible(false);
        syncLyricsPanel.setManaged(false);
        syncLyricsPanel.setBackground(Background.EMPTY);
        syncLyricsPanel.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Ensure scrollpane viewport doesn't paint over gradient (extra safety)
        syncLyricsPanel.skinProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            Node vp = syncLyricsPanel.lookup(".viewport");
            if (vp instanceof Region)
                ((Region) vp).setBackground(Background.EMPTY);
        }));

        lyricsEmptyOverlay = createLyricsEmptyOverlay();
        lyricsEmptyOverlay.setVisible(false);
        lyricsEmptyOverlay.setManaged(false);

        // Gradient background behind both TextArea and SyncLyricsPanel
        lyricsGradientBg = new Region();
        lyricsGradientBg.setMouseTransparent(true);
        lyricsGradientBg.setBackground(new Background(new BackgroundFill(
                createLyricsGradient(currentAccent),
                new CornerRadii(12),
                Insets.EMPTY)));

        lyricsStack = new StackPane(lyricsGradientBg, lyricsArea, syncLyricsPanel, lyricsEmptyOverlay);
        VBox.setVgrow(lyricsStack, Priority.ALWAYS);

        // Apply theme immediately
        updateLyricsGradient(currentAccent);
        syncLyricsPanel.setThemeFromBase(currentAccent);

        panel.getChildren().addAll(headerBox, lyricsStack);
        return panel;
    }

    private VBox createLyricsEmptyOverlay() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);
        box.setPadding(new Insets(25));
        box.setStyle(
                "-fx-background-color: rgba(0,0,0,0.35);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(255,255,255,0.10);" +
                        "-fx-border-radius: 12;");

        lyricsEmptyTitle = new Label("No lyrics found");
        lyricsEmptyTitle.setTextFill(Color.WHITE);
        lyricsEmptyTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        lyricsEmptySubtitle = new Label("Click Add to paste lyrics, or Refresh to search again.");
        lyricsEmptySubtitle.setTextFill(Color.web("#b8b8b8"));
        lyricsEmptySubtitle.setWrapText(true);
        lyricsEmptySubtitle.setMaxWidth(260);
        lyricsEmptySubtitle.setAlignment(Pos.CENTER);

        lyricsEmptyAddButton = new Button("➕ Add Lyrics");
        lyricsEmptyAddButton.setOnAction(e -> showLyricsEditor());

        lyricsEmptyRefreshButton = new Button("⟳ Refresh");
        lyricsEmptyRefreshButton.setOnAction(e -> {
            loadLyrics();
            loadSynchronizedLyrics(true);
        });

        styleAccentFillButton(lyricsEmptyAddButton);
        styleSoftButton(lyricsEmptyRefreshButton);

        HBox row = new HBox(10, lyricsEmptyAddButton, lyricsEmptyRefreshButton);
        row.setAlignment(Pos.CENTER);

        box.getChildren().addAll(lyricsEmptyTitle, lyricsEmptySubtitle, row);
        return box;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.web("#b8ffb8"));
        statusLabel.setFont(Font.font(12));

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label versionLabel = new Label("HarmonyPlayer v2.0");
        versionLabel.setTextFill(Color.web("#bbb"));
        versionLabel.setFont(Font.font(11));

        statusBar.getChildren().addAll(statusLabel, spacer1, versionLabel);
        return statusBar;
    }

    // ================= Playlist hide/show =================

    private void setPlaylistVisible(boolean show) {
        playlistVisible = show;
        if (rootPane == null)
            return;

        if (show) {
            if (playlistPanel == null)
                playlistPanel = createPlaylistPanel();
            rootPane.setLeft(playlistPanel);
        } else {
            rootPane.setLeft(null);
        }
    }

    // ================= Lyrics logic =================

    private void toggleLyricsMode(boolean useSync, boolean fromAuto) {
        useSyncLyrics = useSync;

        if (useSync)
            fadeSwap(lyricsArea, syncLyricsPanel);
        else
            fadeSwap(syncLyricsPanel, lyricsArea);

        refreshLyricsEmptyOverlay();
    }

    private void fadeSwap(Region hideNode, Region showNode) {
        hideNode.setVisible(false);
        hideNode.setManaged(false);

        showNode.setVisible(true);
        showNode.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(180), showNode);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void showLyricsEmptyOverlay(String title, String subtitle) {
        if (lyricsEmptyOverlay == null)
            return;

        lyricsEmptyTitle.setText(title != null ? title : "No lyrics found");
        lyricsEmptySubtitle.setText(subtitle != null ? subtitle : "");

        lyricsEmptyOverlay.setVisible(true);
        lyricsEmptyOverlay.setManaged(true);
        lyricsEmptyOverlay.toFront();

        FadeTransition ft = new FadeTransition(Duration.millis(180), lyricsEmptyOverlay);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void hideLyricsEmptyOverlay() {
        if (lyricsEmptyOverlay == null)
            return;
        lyricsEmptyOverlay.setVisible(false);
        lyricsEmptyOverlay.setManaged(false);
    }

    private void refreshLyricsEmptyOverlay() {
        Song current = mainController.getPlayerController().getCurrentSong();
        if (current == null) {
            showLyricsEmptyOverlay("No song playing", "Play a song to load lyrics.");
            return;
        }

        if (useSyncLyrics) {
            boolean hasSync = currentSyncLyrics != null && !currentSyncLyrics.isEmpty();
            if (hasSync)
                hideLyricsEmptyOverlay();
            else
                showLyricsEmptyOverlay("No synchronized lyrics (LRC)", "Click Add to paste LRC or plain lyrics.");
        } else {
            String text = lyricsArea != null ? lyricsArea.getText() : null;
            boolean hasPlain = text != null && !text.trim().isEmpty()
                    && !text.startsWith("Loading")
                    && !text.equalsIgnoreCase("Lyrics will appear here...");

            if (hasPlain)
                hideLyricsEmptyOverlay();
            else
                showLyricsEmptyOverlay("No lyrics found", "Click Add to paste lyrics, or Refresh to search again.");
        }
    }

    private void loadLyrics() {
        Song song = mainController.getPlayerController().getCurrentSong();
        if (song == null) {
            if (lyricsArea != null)
                lyricsArea.setText("");
            refreshLyricsEmptyOverlay();
            return;
        }

        lyricsArea.setText("Loading lyrics...");
        refreshLyricsEmptyOverlay();

        new Thread(() -> {
            String lyrics = lyricsService.fetchLyrics(song.getArtist(), song.getTitle());
            final String result = (lyrics == null) ? "" : lyrics;

            Platform.runLater(() -> {
                if (mainController.getPlayerController().getCurrentSong() != song)
                    return;
                lyricsArea.setText(result.trim().isEmpty() ? "" : result);
                refreshLyricsEmptyOverlay();
            });
        }, "lyrics-loader").start();
    }

    private void loadSynchronizedLyrics(boolean autoSwitch) {
        Song song = mainController.getPlayerController().getCurrentSong();
        currentSyncLyrics = null;

        if (syncToggleButton != null)
            syncToggleButton.setDisable(true);

        if (song == null) {
            refreshLyricsEmptyOverlay();
            return;
        }

        new Thread(() -> {
            SynchronizedLyrics lyrics = lyricsService.fetchSynchronizedLyrics(song.getArtist(), song.getTitle());

            Platform.runLater(() -> {
                if (mainController.getPlayerController().getCurrentSong() != song)
                    return;

                currentSyncLyrics = lyrics;
                boolean hasSync = (lyrics != null && !lyrics.isEmpty());

                if (syncToggleButton != null)
                    syncToggleButton.setDisable(!hasSync);
                if (syncLyricsPanel != null)
                    syncLyricsPanel.setLyrics(lyrics);

                if (autoSwitch && hasSync) {
                    if (syncToggleButton != null)
                        syncToggleButton.setSelected(true);
                    toggleLyricsMode(true, true);
                }

                refreshLyricsEmptyOverlay();
            });
        }, "sync-lyrics-loader").start();
    }

    // ================= Playback =================

    private void playSong(Song song) {
        if (song == null)
            return;
        mainController.playSong(song);

        if (networkStreamController != null && networkStreamController.isServerRunning()) {
            networkStreamController.startStreaming(song);
        }
        if (loadingIndicator != null)
            loadingIndicator.setVisible(true);
        mainController.playSong(song);
        if (playlistView != null)
            playlistView.getSelectionModel().select(song);

        // REQUIRED: start streaming current song so devices show it and can play it
        if (networkStreamController != null && networkStreamController.isServerRunning()) {
            networkStreamController.startStreaming(song);
        }
    }

    private void togglePlayPause() {
        PlayerController controller = mainController.getPlayerController();

        if (controller.isStopped() || controller.getCurrentSong() == null) {
            Song song = mainController.getCurrentPlaylist().getCurrentSong();
            if (song != null)
                playSong(song);
            else if (!mainController.getCurrentPlaylist().getSongs().isEmpty()) {
                playSong(mainController.getCurrentPlaylist().getSongs().get(0));
            }
        } else {
            controller.togglePlayPause();
        }
    }

    private void playNext() {
        Song next = mainController.getCurrentPlaylist().getNextSong();
        if (next != null) {
            mainController.playSong(next);
            if (playlistView != null)
                playlistView.getSelectionModel().select(next);
        }
    }

    private void playPrevious() {
        Song prev = mainController.getCurrentPlaylist().getPreviousSong();
        if (prev != null) {
            mainController.playSong(prev);
            if (playlistView != null)
                playlistView.getSelectionModel().select(prev);
        }
    }

    private void stop() {
        mainController.getPlayerController().stop();
        if (nowPlayingLabel != null)
            nowPlayingLabel.setText("No song playing");
        if (artistLabel != null)
            artistLabel.setText("");
        if (albumLabel != null)
            albumLabel.setText("");
        if (bitrateLabel != null)
            bitrateLabel.setText("");

        if (albumArtView != null)
            albumArtView.setImage(albumArtService.getDefaultAlbumArt());
        if (lyricsArea != null)
            lyricsArea.setText("Lyrics will appear here...");
        if (syncLyricsPanel != null)
            syncLyricsPanel.reset();

        currentSyncLyrics = null;
        if (syncToggleButton != null) {
            syncToggleButton.setSelected(false);
            syncToggleButton.setDisable(true);
        }

        applyThemeFromBase(DEFAULT_BASE);
        refreshLyricsEmptyOverlay();
    }

    private void toggleShuffle() {
        PlayerController controller = mainController.getPlayerController();
        controller.toggleShuffle();
        styleModeButton(shuffleButton);
    }

    private void toggleRepeat() {
        PlayerController controller = mainController.getPlayerController();
        controller.cycleRepeatMode();
        styleModeButton(repeatButton);
    }

    private void toggleDJMode() {
        boolean isVisible = djControlsBox.isVisible();
        djControlsBox.setVisible(!isVisible);
        djControlsBox.setManaged(!isVisible);
        styleModeButton(djModeButton);
    }

    // ================= Library / YouTube =================

    private void addLocalMusic() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.aac"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        if (files != null && !files.isEmpty()) {
            mainController.addLocalMusicFiles(files);
        }
    }

    private void addFolderMusic() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Music Folder");

        File directory = dirChooser.showDialog(primaryStage);
        if (directory != null) {
            scanDirectory(directory, true);
        }
    }

    private void scanDirectory(File directory, boolean recursive) {
        List<File> musicFiles = new ArrayList<>();
        String[] extensions = { ".mp3", ".wav", ".m4a", ".aac" };
        scanForMusicFiles(directory, musicFiles, extensions, recursive);

        if (!musicFiles.isEmpty()) {
            mainController.addLocalMusicFiles(musicFiles);
        }
    }

    private void scanForMusicFiles(File directory, List<File> musicFiles, String[] extensions, boolean recursive) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    for (String ext : extensions) {
                        if (name.endsWith(ext)) {
                            musicFiles.add(file);
                            break;
                        }
                    }
                } else if (file.isDirectory() && recursive) {
                    scanForMusicFiles(file, musicFiles, extensions, true);
                }
            }
        }
    }

    private void showYouTubeDialog() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("YouTube");
        a.setHeaderText("YouTube import");
        a.setContentText("Wire this to your YouTube importer.");
        a.showAndWait();
    }

    // ================= Misc actions =================

    private void showEqualizerWindow() {
        EqualizerWindow equalizerWindow = new EqualizerWindow(equalizerController);
        equalizerWindow.show();
    }

    private void showNetworkStreamDialog() {
        DeviceManagerDialog deviceDialog = new DeviceManagerDialog(networkStreamController);
        deviceDialog.show();
    }

    private void showLyricsEditor() {
        Song current = mainController.getPlayerController().getCurrentSong();
        if (current != null) {
            LyricsEditorDialog editor = new LyricsEditorDialog(current, lyricsService, this::loadLyrics);
            editor.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Song Playing");
            alert.setContentText("Please play a song first to add/edit lyrics.");
            alert.showAndWait();
        }
    }

    private void removeSelectedSong() {
        int selectedIdx = playlistView.getSelectionModel().getSelectedIndex();
        if (selectedIdx >= 0)
            mainController.removeSong(selectedIdx);
    }

    private void confirmClearPlaylist() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Playlist");
        confirm.setContentText("Remove all songs from playlist?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK)
                mainController.clearPlaylist();
        });
    }

    private void filterPlaylist(String query) {
        if (query == null || query.trim().isEmpty()) {
            playlistView.setItems(mainController.getObservableSongs());
        } else {
            List<Song> filtered = mainController.searchSongs(query);
            playlistView.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
        }
    }

    // ================= Album art + theme =================

    private void loadAlbumArt(Song song) {
        if (song == null)
            return;

        new Thread(() -> {
            Image art = albumArtService.extractAlbumArt(song);
            if (art == null)
                art = albumArtService.getDefaultAlbumArt();

            Color avg = computeAverageColor(art);
            Image finalArt = art;

            Platform.runLater(() -> {
                albumArtView.setImage(finalArt);
                applyThemeFromBase(avg);
            });
        }, "album-art-loader").start();
    }

    private void updateNowPlaying(Song song) {
        if (song != null) {
            nowPlayingLabel.setText(song.getTitle());
            artistLabel.setText(song.getArtist());
            albumLabel.setText(song.getAlbum() != null ? song.getAlbum() : "");

            String bitrateInfo = "";
            if (song.getBitrate() > 0)
                bitrateInfo += song.getBitrate() + " kbps";
            if (song.getFormat() != null) {
                bitrateInfo += (bitrateInfo.isEmpty() ? "" : " - ") + song.getFormat().toUpperCase();
            }
            bitrateLabel.setText(bitrateInfo);
        } else {
            nowPlayingLabel.setText("No song playing");
            artistLabel.setText("");
            albumLabel.setText("");
            bitrateLabel.setText("");
        }
    }

    private void updateProgressBar(Duration duration) {
        Song current = mainController.getPlayerController().getCurrentSong();
        if (current != null && current.getDuration() > 0) {
            double progress = duration.toSeconds();

            progressSlider.setMax(current.getDuration());
            progressSlider.setValue(progress);

            String currentTime = formatTime((int) progress);
            String totalTime = formatTime((int) current.getDuration());
            timeLabel.setText(currentTime + " / " + totalTime);

            if (useSyncLyrics && syncLyricsPanel != null) {
                long timeMs = (long) (progress * 1000);
                syncLyricsPanel.updateCurrentTime(timeMs);
            }
        }
    }

    private String formatTime(int seconds) {
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About HarmonyPlayer");
        alert.setHeaderText("HarmonyPlayer v2.0 Ultimate Edition");
        alert.setContentText("Advanced Music Player with Lyrics + Sync Lyrics.");
        alert.showAndWait();
    }

    private void shutdown() {
        mainController.shutdown();
        networkStreamController.shutdown();
        djController.shutdown();
    }

    // ================== Theme helpers ==================

    private Color computeAverageColor(Image image) {
        if (image == null)
            return DEFAULT_BASE;
        PixelReader pr = image.getPixelReader();
        if (pr == null)
            return DEFAULT_BASE;

        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        if (w <= 0 || h <= 0)
            return DEFAULT_BASE;

        int stepX = Math.max(1, w / 60);
        int stepY = Math.max(1, h / 60);

        double r = 0, g = 0, b = 0;
        long count = 0;

        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                Color c = pr.getColor(x, y);
                if (c.getOpacity() < 0.6)
                    continue;
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }

        if (count == 0)
            return DEFAULT_BASE;
        return new Color(r / count, g / count, b / count, 1.0);
    }

    private Color deriveAccent(Color base) {
        if (base == null)
            return DEFAULT_ACCENT;
        return Color.hsb(
                base.getHue(),
                clamp01(base.getSaturation() + 0.35),
                clamp01(base.getBrightness() + 0.55));
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void applyThemeFromBase(Color albumBase) {
        if (rootPane == null)
            return;
        if (albumBase == null)
            albumBase = DEFAULT_BASE;

        // Darken for readable background
        Color base = albumBase.interpolate(Color.BLACK, 0.40);
        Color top = base.brighter();
        Color bottom = base.darker().darker();

        // Accent stays vivid
        currentAccent = deriveAccent(albumBase);

        // Root background gradient
        rootPane.setStyle(String.format(
                "-fx-background-color: linear-gradient(to bottom, %s, %s);",
                toRgba(top, 1.0),
                toRgba(bottom, 1.0)));

        // Recolor buttons/sliders/indicator
        restyleAllThemedControls();

        // Lyrics gradient + sync lyrics theme
        updateLyricsGradient(currentAccent);
        if (syncLyricsPanel != null)
            syncLyricsPanel.setThemeFromBase(currentAccent);
    }

    private void restyleAllThemedControls() {
        styleControlButton(prevButton, 30);
        styleControlButton(playPauseButton, 30);
        styleControlButton(nextButton, 30);
        styleControlButton(stopButton, 30);

        styleModeButton(playlistToggleButton);
        styleModeButton(shuffleButton);
        styleModeButton(repeatButton);
        styleModeButton(equalizerButton);
        styleModeButton(djModeButton);
        styleModeButton(networkStreamButton);

        if (progressSlider != null)
            progressSlider.setStyle("-fx-accent: " + toRgba(currentAccent, 1.0) + ";");
        if (volumeSlider != null)
            volumeSlider.setStyle("-fx-accent: " + toRgba(currentAccent, 1.0) + ";");
        if (loadingIndicator != null)
            loadingIndicator.setStyle("-fx-progress-color: " + toRgba(currentAccent, 1.0) + ";");

        if (lyricsEmptyAddButton != null)
            styleAccentFillButton(lyricsEmptyAddButton);
        if (lyricsEmptyRefreshButton != null)
            styleSoftButton(lyricsEmptyRefreshButton);
    }

    private void styleControlButton(Button b, double radius) {
        if (b == null)
            return;

        b.setFont(b.getFont() == null ? Font.font("System", 20) : b.getFont());
        b.setPrefSize(b == playPauseButton ? 70 : 60, b == playPauseButton ? 70 : 60);

        Color base = currentAccent;
        Color hover = currentAccent.brighter();
        Color pressed = currentAccent.darker();

        String baseStyle = "-fx-background-color: " + toRgba(base, 0.92) + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + toRgba(hover, 0.98) + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-cursor: hand;";
        String pressedStyle = "-fx-background-color: " + toRgba(pressed, 0.98) + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-cursor: hand;";

        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
        b.setOnMouseExited(e -> b.setStyle(baseStyle));
        b.setOnMousePressed(e -> b.setStyle(pressedStyle));
        b.setOnMouseReleased(e -> b.setStyle(hoverStyle));
    }

    private void styleModeButton(ButtonBase b) {
        if (b == null)
            return;

        boolean selected = (b instanceof ToggleButton) && ((ToggleButton) b).isSelected();

        if (selected) {
            b.setStyle("-fx-background-color: " + toRgba(currentAccent, 0.85) + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-background-radius: 6;"
                    + "-fx-cursor: hand;");
        } else {
            b.setStyle("-fx-background-color: rgba(255,255,255,0.18);"
                    + "-fx-text-fill: white;"
                    + "-fx-background-radius: 6;"
                    + "-fx-cursor: hand;");
        }
    }

    private void styleAccentFillButton(Button b) {
        if (b == null)
            return;
        b.setStyle("-fx-background-color: " + toRgba(currentAccent, 0.95) + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;");
    }

    private void styleSoftButton(Button b) {
        if (b == null)
            return;
        b.setStyle("-fx-background-color: rgba(255,255,255,0.18);"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 8;");
    }

    // ================== Lyrics gradient helpers ==================

    private void updateLyricsGradient(Color accent) {
        if (accent == null)
            return;

        if (lyricsGradientBg != null) {
            lyricsGradientBg.setBackground(new Background(new BackgroundFill(
                    createLyricsGradient(accent),
                    new CornerRadii(12),
                    Insets.EMPTY)));
        }

        // also keep TextArea selection highlight in sync
        if (lyricsArea != null) {
            lyricsArea.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-control-inner-background: transparent;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-highlight-fill: " + toRgba(accent, 0.60) + ";" +
                            "-fx-highlight-text-fill: white;");
        }
    }

    private LinearGradient createLyricsGradient(Color accent) {
        Color top = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.22);
        Color mid = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.10);
        Color bottom = new Color(0, 0, 0, 0.48);

        return new LinearGradient(
                0, 0, 0, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, top),
                new Stop(0.55, mid),
                new Stop(1.0, bottom));
    }

    private String toRgba(Color c, double alpha) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, alpha);
    }
}