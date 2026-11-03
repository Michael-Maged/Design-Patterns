package org.nonprofit.communication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nonprofit.core.Audience;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.Person;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;
import org.nonprofit.core.SystemObserver;

/**
 * Entry point of the Communication module (what the Facade will call).
 *
 * OBSERVER pattern (concrete observer): subscribes to every topic on the
 * {@link EventBus} and turns each system event into messages.
 * STRATEGY pattern (client): sends each message through the channel picked
 * by {@link ChannelSelector}, or through one channel forced by the caller.
 * DECORATOR pattern (client): builds the message text from stacked
 * {@link MessageContent} decorators.
 */
public class CommunicationService implements SystemObserver {
    private final String organisation;
    private final ChannelSelector selector = new ChannelSelector();
    private final List<SentMessage> log = new ArrayList<>();

    public CommunicationService(String organisation) {
        this.organisation = organisation;
        EventBus bus = EventBus.getInstance();
        for (SystemEventType type : SystemEventType.values()) {
            bus.subscribe(type, this);
        }
    }

    /** Called by the {@link EventBus}. A null recipient means "everyone in the source's audience". */
    @Override
    public void onEvent(SystemEvent event) {
        boolean urgent = event.getType() == SystemEventType.URGENT_NEED
                || event.getType() == SystemEventType.EVENT_CANCELLED;
        String subject = subjectFor(event.getType());
        if (event.getRecipient() != null) {
            send(event.getRecipient(), subject, event.getMessage(), urgent, null);
        } else if (event.getSource() instanceof Audience) {
            broadcast(((Audience) event.getSource()).getAudience(), subject, event.getMessage(), urgent);
        }
    }

    /** Sends one text to many people, each on their preferred channel. @return how many were reached */
    public int broadcast(Collection<? extends Person> recipients, String subject, String text, boolean urgent) {
        return broadcast(recipients, subject, text, urgent, null);
    }

    /** As above, but every message goes through {@code forced} (null = each person's preference). */
    public int broadcast(Collection<? extends Person> recipients, String subject, String text, boolean urgent,
            ContactPreference forced) {
        int reached = 0;
        for (Person person : recipients) {
            if (send(person, subject, text, urgent, forced)) {
                reached++;
            }
        }
        return reached;
    }

    /** @return true if the message was sent, false if the person has no usable contact details */
    public boolean send(Person recipient, String subject, String text, boolean urgent, ContactPreference forced) {
        CommunicationChannel channel = forced != null ? selector.channelFor(forced) : selector.channelFor(recipient);
        if (channel == null) {
            System.out.println("    [skipped] " + recipient.getName() + " has no contact details");
            return false;
        }
        try {
            channel.send(new Message(recipient, subject, compose(text, urgent, channel)));
        } catch (IllegalArgumentException e) {
            System.out.println("    [skipped] " + e.getMessage());
            return false;
        }
        log.add(new SentMessage(LocalDateTime.now(), channel.name(), recipient.getName(), subject));
        return true;
    }

    /** DECORATOR in use: tag if urgent, a signature, and a legal footer on e-mail only. */
    String compose(String text, boolean urgent, CommunicationChannel channel) {
        MessageContent content = new PlainContent(text);
        if (urgent) {
            content = new UrgentTagDecorator(content);
        }
        if (channel instanceof EmailChannel) {
            content = new FooterDecorator(new SignatureDecorator(content, organisation));
        } else if (channel instanceof SocialMediaChannel) {
            content = new SignatureDecorator(content, organisation);
        }
        return content.render();
    }

    public List<SentMessage> getLog() {
        return List.copyOf(log);
    }

    private static String subjectFor(SystemEventType type) {
        switch (type) {
            case DONATION_RECEIVED:
                return "Thank you for your donation";
            case VOLUNTEER_ASSIGNED:
                return "New volunteer assignment";
            case RESOURCE_DISTRIBUTED:
                return "Resources distributed";
            case URGENT_NEED:
                return "Urgent need";
            case EVENT_REGISTERED:
                return "Event registration";
            case EVENT_UPDATED:
                return "Event updated";
            case EVENT_REMINDER:
                return "Event reminder";
            case EVENT_CANCELLED:
                return "Event cancelled";
            default:
                return "Waitlist update";
        }
    }
}
