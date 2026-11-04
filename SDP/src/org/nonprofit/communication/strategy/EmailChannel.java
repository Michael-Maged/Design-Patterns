package org.nonprofit.communication.strategy;

import org.nonprofit.communication.decorator.FooterDecorator;
import org.nonprofit.communication.model.Message;
import org.nonprofit.communication.decorator.MessageContent;
import org.nonprofit.communication.decorator.SignatureDecorator;

/** STRATEGY pattern (concrete strategy): simulated e-mail, returned as text for a view to show. */
public class EmailChannel implements CommunicationChannel {

    @Override
    public String send(Message message) {
        String address = message.getRecipient().getEmail();
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(message.getRecipient().getName() + " has no e-mail address");
        }
        StringBuilder text = new StringBuilder("    [EMAIL to " + address + "] Subject: " + message.getSubject());
        for (String line : message.getBody().split("\n")) {
            text.append("\n        ").append(line);
        }
        return text.toString();
    }

    @Override
    public String name() {
        return "Email";
    }

    /** E-mail carries a signature and the legal footer. */
    @Override
    public MessageContent decorate(MessageContent content, String organisation) {
        return new FooterDecorator(new SignatureDecorator(content, organisation));
    }
}
