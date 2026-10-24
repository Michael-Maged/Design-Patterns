package org.nonprofit.volunteer;

import java.time.LocalDate;

/** One line of a volunteer's hour log: the task worked, for how long, and when. */
public record HourEntry(Task task, double hours, LocalDate date) {
}
