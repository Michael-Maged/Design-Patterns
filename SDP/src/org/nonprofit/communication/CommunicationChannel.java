package org.nonprofit.communication;

/**
 * STRATEGY pattern (strategy interface): one way of delivering a message.
 * {@link CommunicationService} picks a channel per recipient at runtime, so
 * the same message can go out by e-mail, SMS or social media.
 */
public interface CommunicationChannel {

    /** @throws IllegalArgumentException if the recipient has no address for this channel */
    void send(Message message);

    /** Short label shown in the communication log. */
    String name();
}
