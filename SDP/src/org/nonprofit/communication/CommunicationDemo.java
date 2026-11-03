package org.nonprofit.communication;

import java.util.List;

import org.nonprofit.core.Audience;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.Person;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Stand-alone walkthrough of the Communication module, showing each pattern.
 * Run: java -cp bin org.nonprofit.communication.CommunicationDemo
 */
public class CommunicationDemo {

    /** A recipient for messages that are not about events. */
    private static class Contact extends Person {
        Contact(String id, String name, String email, String phone, ContactPreference preference) {
            super(id, name, email, phone, preference);
        }
    }

    public static void main(String[] args) {
        CommunicationService comms = new CommunicationService("Hope Foundation");

        Person nadia = new Contact("P-1", "Nadia Farouk", "nadia@example.org", "+20100000010", ContactPreference.EMAIL);
        Person karim = new Contact("P-2", "Karim Adel", "karim@example.org", "+20100000011", ContactPreference.SMS);
        Person hana = new Contact("P-3", "Hana Mostafa", "hana@example.org", "+20100000012", ContactPreference.SOCIAL_MEDIA);
        Person ghost = new Contact("P-4", "Sami Nour", null, null, ContactPreference.SMS);

        heading("1. STRATEGY - same message, channel chosen per recipient");
        for (Person p : List.of(nadia, karim, hana)) {
            comms.send(p, "Hello", "Thanks for supporting our winter drive.", false, null);
        }
        System.out.println("  Prefers SMS but has no phone or e-mail, so the selector falls back to social media:");
        comms.send(ghost, "Hello", "Thanks for supporting our winter drive.", false, null);

        heading("2. STRATEGY - switching channel at runtime (everyone by SMS)");
        comms.broadcast(List.of(nadia, karim, hana), "Road closed", "Meet at the side gate today.", false,
                ContactPreference.SMS);

        heading("3. DECORATOR - urgent tag, signature and footer stacked on the text");
        comms.send(nadia, "Urgent need", "We need blankets by Friday.", true, null);
        comms.send(hana, "Urgent need", "We need blankets by Friday.", true, null);
        comms.send(karim, "Urgent need", "We need blankets by Friday. Please bring any you can spare to the main hall tonight.", true, null);

        heading("4. OBSERVER - system events on the EventBus become messages");
        EventBus bus = EventBus.getInstance();
        bus.publish(new SystemEvent(SystemEventType.DONATION_RECEIVED,
                "Thank you for your ONLINE donation of $250.00", nadia, null));
        bus.publish(new SystemEvent(SystemEventType.VOLUNTEER_ASSIGNED,
                "You are assigned to Pack food boxes on SATURDAY", karim, null));

        heading("5. OBSERVER - a message with no recipient reaches its source's whole audience");
        // Any source that implements core.Audience works (in the real system it is an Event).
        // A stand-in is used here so this module never depends on the Events package.
        Person mona = new Contact("P-5", "Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Person omar = new Contact("P-6", "Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        Audience workshopAttendees = () -> List.of(mona, omar);
        bus.publish(new SystemEvent(SystemEventType.EVENT_UPDATED,
                "First Aid Basics moved from Room 4B to Room 7A", null, workshopAttendees));

        heading("6. Communication log");
        comms.getLog().forEach(entry -> System.out.println("  " + entry));
    }

    private static void heading(String text) {
        System.out.println("\n== " + text);
    }
}
