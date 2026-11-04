package org.nonprofit.communication.decorator;

/** DECORATOR pattern (concrete decorator): puts an [URGENT] tag in front. */
public class UrgentTagDecorator extends ContentDecorator {

    public UrgentTagDecorator(MessageContent wrapped) {
        super(wrapped);
    }

    @Override
    public String render() {
        return "[URGENT] " + wrapped.render();
    }
}
