package org.nonprofit.gui.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns what the user typed into values, for the controllers. Bad input
 * throws {@link IllegalArgumentException}, which {@link ScreenActions} shows
 * as an error dialog, the same as a rule broken in the model.
 */
public final class Inputs {

    private Inputs() {
    }

    /** "first aid, driving" becomes ["first aid", "driving"]; empty parts are skipped. */
    public static List<String> list(String text) {
        List<String> parts = new ArrayList<>();
        if (text == null) {
            return parts;
        }
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }

    /** A decimal number such as "2.5"; NaN and Infinity are refused. */
    public static double number(String text, String what) {
        try {
            double value = Double.parseDouble(text.trim());
            if (Double.isFinite(value)) {
                return value;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // falls through to the message below
        }
        throw new IllegalArgumentException(what + " must be a number, e.g. 2.5");
    }

    /** A whole number such as "3". */
    public static int whole(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(what + " must be a whole number, e.g. 3");
        }
    }

    /** Empty text means "none" (null), e.g. an optional e-mail. */
    public static String orNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
