package org.nonprofit.core.model;

import java.util.List;
import org.nonprofit.core.observer.SystemEvent;

/**
 * Anything that addresses a group of people (an event's attendees, for
 * example). A {@link SystemEvent} with no recipient carries one of these as
 * its source, so Communication can reach the group without depending on the
 * module that owns it.
 */
public interface Audience {
    List<? extends Person> getAudience();
}
