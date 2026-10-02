package com.harmonyplayer.ui;

import com.harmonyplayer.model.LyricLine;
import com.harmonyplayer.model.SynchronizedLyrics;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class SyncLyricsPanel extends ScrollPane {

    private final VBox lyricsContainer;
    private final List<Label> lyricLabels;

    private SynchronizedLyrics currentLyrics;
    private int currentLineIndex = -1;
    private Timeline scrollTimeline;

    // Placeholder references (so we can recolor when theme changes)
    private Label placeholderIcon;

    // Theme colors (auto updated from MainWindow)
    private Color accentColor = Color.web("#3282b8");
    private Color inactiveTextColor = Color.web("#666666");
    private Color nextTextColor = Color.web("#b8b8b8");
    private Color activeTextColor = Color.WHITE;

    // Animation settings
    private static final double ACTIVE_FONT_SIZE = 24;
    private static final double INACTIVE_FONT_SIZE = 16;
    private static final double NEXT_FONT_SIZE = 18;
    private static final Duration FADE_DURATION = Duration.millis(300);
    private static final Duration SCALE_DURATION = Duration.millis(400);
    private static final Duration SCROLL_DURATION = Duration.millis(600);

    public SyncLyricsPanel() {
        this.lyricLabels = new ArrayList<>();
        this.lyricsContainer = new VBox(12);
        this.lyricsContainer.setAlignment(Pos.CENTER);
        this.lyricsContainer.setPadding(new Insets(40, 20, 40, 20));

        // IMPORTANT: ensure container itself is transparent
        lyricsContainer.setBackground(Background.EMPTY);

        setContent(lyricsContainer);
        setFitToWidth(true);

        // IMPORTANT: do NOT paint any background here
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setPannable(true);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVvalue(0);

        // IMPORTANT: when skin is created, clear viewport background too
        skinProperty().addListener((obs, old, skin) -> Platform.runLater(this::applyTransparentViewport));
        Platform.runLater(this::applyTransparentViewport);
    }

    private void applyTransparentViewport() {
        // clears the internal viewport that often blocks parent gradients
        Node viewport = lookup(".viewport");
        if (viewport instanceof Region) {
            ((Region) viewport).setBackground(Background.EMPTY);
            viewport.setStyle("-fx-background-color: transparent;");
        }

        // some skins also have a ".content" region
        Node content = lookup(".content");
        if (content instanceof Region) {
            ((Region) content).setBackground(Background.EMPTY);
            content.setStyle("-fx-background-color: transparent;");
        }
    }

    // ---------------- Theme API ----------------

    public void setThemeFromBase(Color base) {
        if (base == null) return;

        Color derivedAccent = Color.hsb(
                base.getHue(),
                clamp01(base.getSaturation() + 0.25),
                clamp01(base.getBrightness() + 0.35)
        );

        setAccentColor(derivedAccent);
    }

    public void setThemeFromGradient(Color top, Color bottom) {
        if (top != null) setAccentColor(top);
    }

    private void setAccentColor(Color accent) {
        if (accent == null) return;

        this.accentColor = accent;

        this.inactiveTextColor = Color.web("#666666");
        this.nextTextColor = Color.web("#b8b8b8");
        this.activeTextColor = Color.WHITE;

        if (placeholderIcon != null) {
            placeholderIcon.setTextFill(accentColor);
        }
    }

    // ---------------- Lyrics ----------------

    public void setLyrics(SynchronizedLyrics lyrics) {
        this.currentLyrics = lyrics;
        this.currentLineIndex = -1;
        lyricLabels.clear();
        lyricsContainer.getChildren().clear();

        if (lyrics == null || lyrics.isEmpty()) {
            showPlaceholder();
            Platform.runLater(this::applyTransparentViewport);
            return;
        }

        VBox topSpacer = new VBox();
        topSpacer.setPrefHeight(200);
        topSpacer.setBackground(Background.EMPTY);
        lyricsContainer.getChildren().add(topSpacer);

        for (int i = 0; i < lyrics.getLines().size(); i++) {
            LyricLine line = lyrics.getLines().get(i);
            Label label = createLyricLabel(line.getText(), i);
            lyricLabels.add(label);
            lyricsContainer.getChildren().add(label);
        }

        VBox bottomSpacer = new VBox();
        bottomSpacer.setPrefHeight(200);
        bottomSpacer.setBackground(Background.EMPTY);
        lyricsContainer.getChildren().add(bottomSpacer);

        Platform.runLater(this::applyTransparentViewport);
    }

    public void updateCurrentTime(long currentTimeMs) {
        if (currentLyrics == null || currentLyrics.isEmpty()) return;

        LyricLine currentLine = currentLyrics.getCurrentLine(currentTimeMs);
        if (currentLine == null) return;

        int newIndex = currentLyrics.getCurrentLineIndex();

        if (newIndex != currentLineIndex && newIndex >= 0 && newIndex < lyricLabels.size()) {
            if (currentLineIndex >= 0 && currentLineIndex < lyricLabels.size()) {
                animateDeactivate(lyricLabels.get(currentLineIndex));
            }

            animateActivate(lyricLabels.get(newIndex));

            if (newIndex + 1 < lyricLabels.size()) {
                animateNext(lyricLabels.get(newIndex + 1));
            }

            if (newIndex > 0) {
                animatePrevious(lyricLabels.get(newIndex - 1));
            }

            smoothScrollToLabel(newIndex);
            currentLineIndex = newIndex;
        }
    }

    private Label createLyricLabel(String text, int index) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, INACTIVE_FONT_SIZE));
        label.setTextFill(inactiveTextColor);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(8));
        label.setOpacity(0.5);

        GaussianBlur blur = new GaussianBlur(2);
        label.setEffect(blur);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(0.5);
        fadeIn.setDelay(Duration.millis(index * 50L));
        fadeIn.play();

        return label;
    }

    private void animateActivate(Label label) {
        Platform.runLater(() -> {
            label.getTransforms().clear();

            ScaleTransition scaleTransition = new ScaleTransition(SCALE_DURATION, label);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, label);
            fadeTransition.setToValue(1.0);

            Timeline fontAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.BOLD, INACTIVE_FONT_SIZE))),
                    new KeyFrame(SCALE_DURATION,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.BOLD, ACTIVE_FONT_SIZE),
                                    Interpolator.EASE_OUT))
            );

            Timeline colorAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.textFillProperty(), nextTextColor)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(label.textFillProperty(), activeTextColor, Interpolator.EASE_OUT))
            );

            DropShadow glow = new DropShadow();
            glow.setColor(accentColor);
            glow.setRadius(15);
            glow.setSpread(0.6);

            Timeline glowAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 0)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(glow.radiusProperty(), 15, Interpolator.EASE_OUT))
            );

            label.setEffect(glow);

            label.setStyle(
                    "-fx-background-color: linear-gradient(to right, " +
                            toRgba(accentColor, 0.18) + ", " +
                            toRgba(accentColor, 0.28) + ", " +
                            toRgba(accentColor, 0.18) + ");" +
                            "-fx-background-radius: 8px; -fx-padding: 12px;"
            );

            new ParallelTransition(scaleTransition, fadeTransition, fontAnimation, colorAnimation, glowAnimation).play();
        });
    }

    private void animateDeactivate(Label label) {
        Platform.runLater(() -> {
            ScaleTransition scaleTransition = new ScaleTransition(SCALE_DURATION, label);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.setInterpolator(Interpolator.EASE_IN);

            FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, label);
            fadeTransition.setToValue(0.4);

            Timeline fontAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.BOLD, ACTIVE_FONT_SIZE))),
                    new KeyFrame(SCALE_DURATION,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.NORMAL, INACTIVE_FONT_SIZE),
                                    Interpolator.EASE_IN))
            );

            Timeline colorAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.textFillProperty(), activeTextColor)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(label.textFillProperty(), inactiveTextColor, Interpolator.EASE_IN))
            );

            GaussianBlur blur = new GaussianBlur(2);
            Timeline blurAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(blur.radiusProperty(), 2, Interpolator.EASE_IN))
            );

            label.setEffect(blur);
            label.setStyle("");

            new ParallelTransition(scaleTransition, fadeTransition, fontAnimation, colorAnimation, blurAnimation).play();
        });
    }

    private void animateNext(Label label) {
        Platform.runLater(() -> {
            FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, label);
            fadeTransition.setToValue(0.7);

            Timeline fontAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.NORMAL, INACTIVE_FONT_SIZE))),
                    new KeyFrame(SCALE_DURATION,
                            new KeyValue(label.fontProperty(),
                                    Font.font("Segoe UI", FontWeight.NORMAL, NEXT_FONT_SIZE),
                                    Interpolator.EASE_OUT))
            );

            Timeline colorAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.textFillProperty(), inactiveTextColor)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(label.textFillProperty(), nextTextColor, Interpolator.EASE_OUT))
            );

            label.setEffect(new GaussianBlur(1));
            new ParallelTransition(fadeTransition, fontAnimation, colorAnimation).play();
        });
    }

    private void animatePrevious(Label label) {
        Platform.runLater(() -> {
            FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, label);
            fadeTransition.setToValue(0.4);

            Timeline colorAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(label.textFillProperty(), nextTextColor)),
                    new KeyFrame(FADE_DURATION,
                            new KeyValue(label.textFillProperty(), inactiveTextColor, Interpolator.EASE_IN))
            );

            label.setEffect(new GaussianBlur(2));
            new ParallelTransition(fadeTransition, colorAnimation).play();
        });
    }

    private void smoothScrollToLabel(int index) {
        Platform.runLater(() -> {
            if (index < 0 || index >= lyricLabels.size()) return;

            Label label = lyricLabels.get(index);

            double labelY = label.getLayoutY();
            double containerHeight = lyricsContainer.getHeight();
            double viewportHeight = getViewportBounds().getHeight();

            if (containerHeight <= viewportHeight) return;

            double targetVValue = (labelY - viewportHeight / 2 + label.getHeight() / 2)
                    / (containerHeight - viewportHeight);

            targetVValue = Math.max(0, Math.min(1, targetVValue));

            if (scrollTimeline != null) scrollTimeline.stop();

            scrollTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(vvalueProperty(), getVvalue())),
                    new KeyFrame(SCROLL_DURATION,
                            new KeyValue(vvalueProperty(), targetVValue,
                                    Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)))
            );
            scrollTimeline.play();
        });
    }

    private void showPlaceholder() {
        VBox placeholderBox = new VBox(15);
        placeholderBox.setAlignment(Pos.CENTER);
        placeholderBox.setPadding(new Insets(50));
        placeholderBox.setBackground(Background.EMPTY);

        placeholderIcon = new Label("♪");
        placeholderIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        placeholderIcon.setTextFill(accentColor);

        Label text1 = new Label("Synchronized Lyrics");
        text1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        text1.setTextFill(nextTextColor);

        Label text2 = new Label("Play a song with LRC format lyrics");
        text2.setFont(Font.font("Segoe UI", 14));
        text2.setTextFill(inactiveTextColor);
        text2.setWrapText(true);
        text2.setAlignment(Pos.CENTER);

        placeholderBox.getChildren().addAll(placeholderIcon, text1, text2);

        FadeTransition fade = new FadeTransition(Duration.millis(800), placeholderBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2), placeholderIcon);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);

        lyricsContainer.getChildren().add(placeholderBox);
        fade.play();
        pulse.play();
    }

    public void reset() {
        currentLineIndex = -1;
        for (Label label : lyricLabels) {
            animateDeactivate(label);
        }

        if (scrollTimeline != null) scrollTimeline.stop();

        scrollTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(vvalueProperty(), getVvalue())),
                new KeyFrame(SCROLL_DURATION, new KeyValue(vvalueProperty(), 0, Interpolator.EASE_BOTH))
        );
        scrollTimeline.play();
    }

    private String toRgba(Color c, double alpha) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, alpha);
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}