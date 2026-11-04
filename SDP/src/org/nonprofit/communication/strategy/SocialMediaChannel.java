package org.nonprofit.communication.strategy;

import org.nonprofit.communication.model.Message;
import org.nonprofit.communication.decorator.MessageContent;
import org.nonprofit.communication.decorator.SignatureDecorator;

/** STRATEGY pattern (concrete strategy): a simulated public post that mentions the recipient. */
public class SocialMediaChannel implements CommunicationChannel {
    private final String platform;

    public SocialMediaChannel(String platform) {
        this.platform = platform;
    }

    @Override
    public String send(Message message) {
        String handle = "@" + message.getRecipient().getName().replace(" ", "");
        return "    [" + platform.toUpperCase() + " post] " + handle + " "
                + message.getBody().replace("\n", " ");
    }

    @Override
    public String name() {
        return platform;
    }

    /** A public post carries the signature only. */
    @Override
    public MessageContent decorate(MessageContent content, String organisation) {
        return new SignatureDecorator(content, organisation);
    }
}
