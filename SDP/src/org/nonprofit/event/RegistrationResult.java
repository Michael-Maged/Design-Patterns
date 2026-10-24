package org.nonprofit.event;

/** Outcome of {@link EventService#register}. {@code ticket} is null unless REGISTERED. */
public record RegistrationResult(Status status, Ticket ticket, int waitlistPosition) {

    public enum Status {
        REGISTERED,
        WAITLISTED,
        ALREADY_REGISTERED,
        ALREADY_WAITLISTED
    }

    static RegistrationResult registered(Ticket ticket) {
        return new RegistrationResult(Status.REGISTERED, ticket, 0);
    }

    static RegistrationResult waitlisted(int position) {
        return new RegistrationResult(Status.WAITLISTED, null, position);
    }

    static RegistrationResult of(Status status) {
        return new RegistrationResult(status, null, 0);
    }
}
