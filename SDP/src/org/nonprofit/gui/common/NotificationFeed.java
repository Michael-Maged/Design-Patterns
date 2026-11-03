package org.nonprofit.gui.common;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;

/** The notification feed: a list of recent lines, newest last, that scrolls to the newest one. */
public final class NotificationFeed implements Feed {
    private static final int MAX_LINES = 500;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ListView<String> view = new ListView<>();

    public NotificationFeed() {
        view.setId("notification-feed");
        view.getStyleClass().add("feed");
        view.setPlaceholder(new Label("No notifications yet. E-mails, SMS and posts sent by the system appear here."));
        view.setFocusTraversable(false);
        // Fixed rows, and a height of whole rows, so the newest line is never cut off.
        view.setFixedCellSize(22);
        view.setPrefHeight(6 * 22 + 2);
    }

    @Override
    public void post(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        view.getItems().add(LocalTime.now().format(TIME) + "  " + line.strip());
        if (view.getItems().size() > MAX_LINES) {
            view.getItems().remove(0);
        }
        // After the next layout, so the new row exists and ends up fully visible at the bottom.
        Platform.runLater(this::scrollToNewest);
    }

    private void scrollToNewest() {
        int last = view.getItems().size() - 1;
        if (last < 0) {
            return;
        }
        // VirtualFlow.scrollTo(int) scrolls just enough to show the whole row;
        // ListView.scrollTo would put it at the top and can leave it half cut off.
        if (view.lookup(".virtual-flow") instanceof VirtualFlow<?> flow) {
            flow.scrollTo(last);
        } else {
            view.scrollTo(last);
        }
    }

    /** The lines shown, oldest first. */
    public List<String> lines() {
        return List.copyOf(view.getItems());
    }

    public ListView<String> getView() {
        return view;
    }
}
