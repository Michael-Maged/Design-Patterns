package org.nonprofit.communication.decorator;

/** DECORATOR pattern (concrete decorator): adds the unsubscribe / legal footer. */
public class FooterDecorator extends ContentDecorator {
    static final String FOOTER = "You receive this because you are registered with us. Reply STOP to unsubscribe.";

    public FooterDecorator(MessageContent wrapped) {
        super(wrapped);
    }

    @Override
    public String render() {
        return wrapped.render() + "\n" + FOOTER;
    }
}
