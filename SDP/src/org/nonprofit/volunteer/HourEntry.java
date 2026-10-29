package org.nonprofit.volunteer;

import java.time.LocalDate;

/** One line of a volunteer's hour log: the task worked, for how long, and when. */
public class HourEntry {
    private final Task task;
    private final double hours;
    private final LocalDate date;

    public HourEntry(Task task, double hours, LocalDate date) {
        this.task = task;
        this.hours = hours;
        this.date = date;
    }

    public Task getTask() { return task; }
    public double getHours() { return hours; }
    public LocalDate getDate() { return date; }
}
