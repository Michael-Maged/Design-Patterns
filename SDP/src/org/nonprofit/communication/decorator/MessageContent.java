package org.nonprofit.communication.decorator;

/**
 * DECORATOR pattern (component): text that can be rendered. Decorators wrap
 * a content object and add a tag, a signature or a footer around it.
 */
public interface MessageContent {
    String render();
}
