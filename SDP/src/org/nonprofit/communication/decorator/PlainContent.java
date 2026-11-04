package org.nonprofit.communication.decorator;

/** DECORATOR pattern (concrete component): the bare text. */
public class PlainContent implements MessageContent {
    private final String text;

    public PlainContent(String text) {
        this.text = text;
    }

    @Override
    public String render() {
        return text;
    }
}
