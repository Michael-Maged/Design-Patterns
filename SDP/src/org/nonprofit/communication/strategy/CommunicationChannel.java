package org.nonprofit.communication.strategy;

import org.nonprofit.communication.model.CommunicationService;
import org.nonprofit.communication.model.Message;
import org.nonprofit.communication.decorator.MessageContent;

/**
 * STRATEGY pattern (strategy interface): one way of delivering a message.
 * {@link CommunicationService} picks a channel per recipient at runtime, so
 * the same message can go out by e-mail, SMS or social media.
 */
public interface CommunicationChannel {

    /**
     * Delivers the message (simulated). Nothing is printed here; the text that
     * would appear on the recipient's device is returned for a view to show.
     *
     * @return the delivery as text (may span several lines)
     * @throws IllegalArgumentException if the recipient has no address for this channel
     */
    String send(Message message);

    /** Short label shown in the communication log. */
    String name();

    /** How this channel dresses a message (DECORATOR). By default it adds nothing. */
    default MessageContent decorate(MessageContent content, String organisation) {
        return content;
    }
}
