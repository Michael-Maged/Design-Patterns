package org.nonprofit.gui.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * The look of every screen: one stylesheet, kept here as text and loaded as a
 * {@code data:} URL. Plain {@code javac} does not copy .css files to the
 * output folder, so a stylesheet kept in Java always applies, from the
 * command line and from VS Code alike.
 *
 * Style classes to use in a screen: {@code screen}, {@code screen-title},
 * {@code screen-subtitle}, {@code section}, {@code section-title},
 * {@code toolbar}, {@code info-bar}, {@code highlight}, {@code status-bar},
 * {@code mono} (most are set for you by {@link Ui}).
 */
public final class Theme {

    private static final String CSS = """
            .root {
                -fx-font-size: 13px;
                -fx-background-color: #f3f5f8;
            }
            .sidebar {
                -fx-background-color: #1f2d3d;
                -fx-padding: 18 10 18 10;
                -fx-spacing: 4;
            }
            .sidebar .app-name {
                -fx-text-fill: white;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-wrap-text: true;
            }
            .sidebar .app-tagline {
                -fx-text-fill: #93a4b8;
                -fx-font-size: 11px;
                -fx-padding: 0 0 14 0;
            }
            .sidebar .toggle-button {
                -fx-background-color: transparent;
                -fx-text-fill: #cfd8e3;
                -fx-alignment: center-left;
                -fx-padding: 9 12 9 12;
                -fx-background-radius: 6;
                -fx-font-size: 14px;
                -fx-cursor: hand;
            }
            .sidebar .toggle-button:hover {
                -fx-background-color: #2c3e50;
            }
            .sidebar .toggle-button:selected {
                -fx-background-color: #2563eb;
                -fx-text-fill: white;
            }
            .screen {
                -fx-padding: 16 18 12 18;
                -fx-spacing: 10;
            }
            .screen-title {
                -fx-font-size: 21px;
                -fx-font-weight: bold;
                -fx-text-fill: #1f2d3d;
            }
            .screen-subtitle {
                -fx-text-fill: #5b6b7c;
            }
            .section {
                -fx-background-color: white;
                -fx-background-radius: 8;
                -fx-border-color: #dde3ea;
                -fx-border-radius: 8;
                -fx-padding: 10 12 12 12;
                -fx-spacing: 8;
            }
            .section-title {
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #1f2d3d;
            }
            .toolbar {
                -fx-spacing: 6;
                -fx-alignment: center-left;
            }
            .info-bar {
                -fx-background-color: #eaf2ff;
                -fx-background-radius: 8;
                -fx-border-color: #bfd5fb;
                -fx-border-radius: 8;
                -fx-padding: 10 12 10 12;
                -fx-spacing: 8;
            }
            .info-bar-title {
                -fx-font-weight: bold;
                -fx-text-fill: #1e3a8a;
            }
            .highlight {
                -fx-font-weight: bold;
                -fx-text-fill: #1d4ed8;
            }
            .muted {
                -fx-text-fill: #5b6b7c;
            }
            .status-bar {
                -fx-background-color: #e7f6ee;
                -fx-text-fill: #14532d;
                -fx-background-radius: 6;
                -fx-padding: 6 10 6 10;
            }
            .feed-box {
                -fx-background-color: #e9edf2;
                -fx-border-color: #d3dae3 transparent transparent transparent;
                -fx-padding: 6 12 10 12;
                -fx-spacing: 4;
            }
            .feed-title {
                -fx-font-weight: bold;
                -fx-text-fill: #334155;
            }
            .feed .list-cell, .mono {
                -fx-font-family: "Monospaced";
                -fx-font-size: 12px;
            }
            .split-pane {
                -fx-background-color: transparent;
                -fx-padding: 0;
            }
            .split-pane > .split-pane-divider {
                -fx-background-color: transparent;
                -fx-padding: 0 0 8 0;
            }
            .placeholder-title {
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: #1f2d3d;
            }
            .placeholder-text {
                -fx-font-size: 14px;
                -fx-text-fill: #5b6b7c;
            }
            .form-label {
                -fx-font-weight: bold;
            }
            .form-grid {
                -fx-padding: 6 4 6 4;
            }
            """;

    /** The stylesheet as a URL JavaFX can load. */
    public static final String URL = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private Theme() {
    }

    public static void apply(Scene scene) {
        if (!scene.getStylesheets().contains(URL)) {
            scene.getStylesheets().add(URL);
        }
    }

    /** For dialogs, which have their own scene. */
    public static void apply(Parent parent) {
        if (!parent.getStylesheets().contains(URL)) {
            parent.getStylesheets().add(URL);
        }
    }
}
