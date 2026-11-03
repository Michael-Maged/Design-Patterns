package org.nonprofit.communication;

/** STRATEGY pattern (concrete strategy): a simulated public post that mentions the recipient. */
public class SocialMediaChannel implements CommunicationChannel {
    private final String platform;

    public SocialMediaChannel(String platform) {
        this.platform = platform;
    }

    @Override
    public void send(Message message) {
        String handle = "@" + message.getRecipient().getName().replace(" ", "");
        System.out.println("    [" + platform.toUpperCase() + " post] " + handle + " "
                + message.getBody().replace("\n", " "));
    }

    @Override
    public String name() {
        return platform;
    }
}
