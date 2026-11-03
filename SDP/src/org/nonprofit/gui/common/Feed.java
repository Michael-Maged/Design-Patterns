package org.nonprofit.gui.common;

import java.util.List;

/**
 * Where a screen's controller puts the lines for the notification feed at
 * the bottom of the window: what the Communication module delivered
 * (e-mails, SMS, posts) and, for Events, the attendees' new notes.
 * {@link NotificationFeed} shows them; a test can pass a plain list instead.
 */
public interface Feed {

    /** Adds one line (blank lines are ignored). */
    void post(String line);

    default void postAll(List<String> lines) {
        for (String line : lines) {
            post(line);
        }
    }
}
