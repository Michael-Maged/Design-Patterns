package org.nonprofit.volunteer;

import java.time.LocalDate;

/** TEMPLATE METHOD pattern (concrete class): thanks a volunteer for their total hours. */
public class HoursCertificate extends Certificate {

    public HoursCertificate(Volunteer volunteer, LocalDate issueDate) {
        super(volunteer, issueDate);
    }

    @Override
    protected String title() {
        return "Certificate of Volunteer Service";
    }

    @Override
    protected String achievement() {
        long tasksDone = volunteer.getTasks().stream().filter(Task::isCompleted).count();
        return String.format("for contributing %.1f hours of service%nacross %d completed task(s).",
                volunteer.getTotalHours(), tasksDone);
    }

    /** Only volunteers who have logged time get one. */
    @Override
    protected boolean isEligible() {
        return volunteer.getTotalHours() > 0;
    }
}
