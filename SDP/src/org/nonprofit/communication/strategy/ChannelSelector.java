package org.nonprofit.communication.strategy;

import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.core.model.Person;

/**
 * Chooses the channel (STRATEGY) for a recipient from their preferred
 * contact method. If that method has no address on file it falls back to
 * whichever one does, e-mail first.
 */
public class ChannelSelector {
    private final CommunicationChannel email = new EmailChannel();
    private final CommunicationChannel sms = new SmsChannel();
    private final CommunicationChannel social = new SocialMediaChannel("Facebook");

    /** The channel for a menu choice or a recipient preference. */
    public CommunicationChannel channelFor(ContactPreference preference) {
        switch (preference) {
            case SMS:
                return sms;
            case SOCIAL_MEDIA:
                return social;
            default:
                return email;
        }
    }

    /** @return the channel to use for this person, or null if they cannot be reached */
    public CommunicationChannel channelFor(Person person) {
        ContactPreference preference = person.getPreferredContact() == null
                ? ContactPreference.EMAIL : person.getPreferredContact();
        if (canUse(person, preference)) {
            return channelFor(preference);
        }
        for (ContactPreference other : ContactPreference.values()) {
            if (canUse(person, other)) {
                return channelFor(other);
            }
        }
        return null;
    }

    private static boolean canUse(Person person, ContactPreference preference) {
        switch (preference) {
            case EMAIL:
                return person.getEmail() != null && !person.getEmail().isBlank();
            case SMS:
                return person.getPhone() != null && !person.getPhone().isBlank();
            default:
                return true;
        }
    }
}
