package org.nonprofit.volunteer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TEMPLATE METHOD pattern (concrete class): thanks a volunteer for helping
 * at one event, listing the tasks they completed there.
 */
public class EventParticipationCertificate extends Certificate {
    private final String eventName;

    public EventParticipationCertificate(Volunteer volunteer, String eventName, LocalDate issueDate) {
        super(volunteer, issueDate);
        this.eventName = eventName;
    }

    @Override
    protected String title() {
        return "Certificate of Participation";
    }

    @Override
    protected String achievement() {
        StringBuilder sb = new StringBuilder("for volunteering at " + eventName + ":");
        for (HourEntry entry : entriesForEvent()) {
            sb.append(String.format("%n  - %s (%.1f h)", entry.getTask().getName(), entry.getHours()));
        }
        return sb.toString();
    }

    /** Only volunteers who logged time on a task of this event get one. */
    @Override
    protected boolean isEligible() {
        return !entriesForEvent().isEmpty();
    }

    private List<HourEntry> entriesForEvent() {
        List<HourEntry> result = new ArrayList<>();
        for (HourEntry entry : volunteer.getHourLog()) {
            if (eventName.equalsIgnoreCase(entry.getTask().getEventName())) {
                result.add(entry);
            }
        }
        return result;
    }
}
