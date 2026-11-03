package org.nonprofit.gui.common;

import java.util.List;
import java.util.function.Supplier;

/**
 * What every button of a screen does around its own work, so each
 * controller writes it once: run the action, show a broken rule as an error
 * dialog, refresh the screen, and move what the Communication module sent
 * (e-mails, SMS, posts) to the notification feed.
 *
 * <pre>
 * actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);
 * panel.onAssign(() -&gt; actions.run(this::assign));
 * </pre>
 */
public final class ScreenActions {
    private final Dialogs dialogs;
    private final Feed feed;
    private final Supplier<List<String>> deliveries;
    private final Runnable refresh;

    /**
     * @param deliveries the lines sent since the last call, e.g. {@code CommunicationService::takeOutput}
     * @param refresh    reloads the screen's tables from the service
     */
    public ScreenActions(Dialogs dialogs, Feed feed, Supplier<List<String>> deliveries, Runnable refresh) {
        this.dialogs = dialogs;
        this.feed = feed;
        this.deliveries = deliveries;
        this.refresh = refresh;
    }

    public void run(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // A rule broken in the model (or bad input) is shown, not thrown.
            dialogs.error(e.getMessage());
        }
        refresh.run();
        feed.postAll(deliveries.get());
    }
}
