package org.nonprofit.gui.common;

import java.util.Optional;

/**
 * Every pop-up a screen's controller needs. {@link FxDialogs} shows real
 * JavaFX dialogs; a test passes its own implementation that answers
 * without blocking.
 */
public interface Dialogs {

    /** A rule was broken (usually the message of an IllegalArgumentException / IllegalStateException). */
    void error(String message);

    void info(String message);

    /** Multi-line text in a scrollable monospace box: receipts, certificates, reports. */
    void text(String title, String text);

    /** @return true if the user chose OK */
    boolean confirm(String question);

    /** Shows the form; empty if the user cancelled. */
    Optional<FormValues> form(Form form);
}
