package org.nonprofit.communication;

import org.nonprofit.core.Person;

/** One message addressed to one person. The body is already fully rendered. */
public class Message {
    private final Person recipient;
    private final String subject;
    private final String body;

    public Message(Person recipient, String subject, String body) {
        if (recipient == null) {
            throw new IllegalArgumentException("A message needs a recipient");
        }
        this.recipient = recipient;
        this.subject = subject == null ? "" : subject;
        this.body = body == null ? "" : body;
    }

    public Person getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
