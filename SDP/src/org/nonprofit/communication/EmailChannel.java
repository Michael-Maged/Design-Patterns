package org.nonprofit.communication;

/** STRATEGY pattern (concrete strategy): simulated e-mail, printed to the console. */
public class EmailChannel implements CommunicationChannel {

    @Override
    public void send(Message message) {
        String address = message.getRecipient().getEmail();
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(message.getRecipient().getName() + " has no e-mail address");
        }
        System.out.println("    [EMAIL to " + address + "] Subject: " + message.getSubject());
        for (String line : message.getBody().split("\n")) {
            System.out.println("        " + line);
        }
    }

    @Override
    public String name() {
        return "Email";
    }
}
