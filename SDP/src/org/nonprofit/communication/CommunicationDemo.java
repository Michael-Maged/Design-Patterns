package org.nonprofit.communication;

import java.time.LocalDateTime;
import java.util.List;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.Person;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.EventService;
import org.nonprofit.event.EventType;

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

        heading("5. OBSERVER - an event update reaches every attendee (audience)");
        EventService events = new EventService();
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 18, 0);
        Event workshop = events.createEvent(EventType.WORKSHOP, "First Aid Basics", start, "Room 4B", 10);
        Attendee mona = new Attendee("Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Attendee omar = new Attendee("Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        events.register(workshop.getId(), mona);
        events.register(workshop.getId(), omar);
        workshop.relocate("Room 7A");

        heading("6. Communication log");
        comms.getLog().forEach(entry -> System.out.println("  " + entry));
    }

    private static void heading(String text) {
        System.out.println("\n== " + text);
    }
}
