package org.nonprofit.communication.decorator;

/**
 * DECORATOR pattern (abstract decorator): wraps another {@link MessageContent}
 * and forwards to it. Decorators stack, e.g. Footer(Signature(Urgent(Plain))).
 */
public abstract class ContentDecorator implements MessageContent {
    protected final MessageContent wrapped;

    protected ContentDecorator(MessageContent wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String render() {
        return wrapped.render();
    }
}
