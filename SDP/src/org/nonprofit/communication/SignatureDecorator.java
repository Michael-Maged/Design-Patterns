package org.nonprofit.communication;

/** DECORATOR pattern (concrete decorator): ends the text with the organisation's signature. */
public class SignatureDecorator extends ContentDecorator {
    private final String organisation;

    public SignatureDecorator(MessageContent wrapped, String organisation) {
        super(wrapped);
        this.organisation = organisation;
    }

    @Override
    public String render() {
        return wrapped.render() + "\n- " + organisation;
    }
}
