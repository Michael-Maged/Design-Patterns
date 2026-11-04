package org.nonprofit.communication.strategy;

import org.nonprofit.communication.model.Message;

/** STRATEGY pattern (concrete strategy): simulated SMS, cut to one 160-character text. */
public class SmsChannel implements CommunicationChannel {
    static final int MAX_LENGTH = 160;

    @Override
    public String send(Message message) {
        String number = message.getRecipient().getPhone();
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException(message.getRecipient().getName() + " has no phone number");
        }
        String text = message.getBody().replace("\n", " ");
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH - 3) + "...";
        }
        return "    [SMS to " + number + "] " + text;
    }

    @Override
    public String name() {
        return "SMS";
    }
}
