package org.nonprofit.communication;

import java.time.LocalDateTime;

/** One line of the communication log: what was sent, to whom, and how. */
public class SentMessage {
    private final LocalDateTime time;
    private final String channel;
    private final String recipient;
    private final String subject;

    public SentMessage(LocalDateTime time, String channel, String recipient, String subject) {
        this.time = time;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
    }

    public LocalDateTime getTime() { return time; }
    public String getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }

    @Override
    public String toString() {
        return String.format("%tF %<tR  %-9s -> %-14s %s", time, channel, recipient, subject);
    }
}
