package org.nonprofit.gui.communication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.nonprofit.communication.strategy.ChannelSelector;
import org.nonprofit.communication.strategy.CommunicationChannel;
import org.nonprofit.communication.model.CommunicationService;
import org.nonprofit.communication.model.Message;
import org.nonprofit.communication.decorator.MessageContent;
import org.nonprofit.communication.decorator.PlainContent;
import org.nonprofit.communication.model.SentMessage;
import org.nonprofit.communication.decorator.UrgentTagDecorator;
import org.nonprofit.core.model.Audience;
import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.core.model.Person;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.Feed;
import org.nonprofit.gui.common.ScreenActions;

/**
 * MVC pattern (controller) of the Communication screen in the GUI: turns the
 * compose area of the {@link CommunicationPanel} into calls on the model
 * ({@link CommunicationService}, unchanged from the console version) and
 * tells the panel what to show. It offers everything the console
 * {@code CommunicationController} does: a group broadcast, one person, the
 * attendees of one event, and the message log.
 *
 * The status line after a send shows the STRATEGY at work (how many people
 * each channel reached); Preview shows the DECORATOR at work (the same text
 * dressed by each channel).
 *
 * The people live in the other modules, which this module does not import:
 * whoever builds the screen passes the same three look-ups as the console
 * screen (group members, a person by ID, an event by ID).
 */
public class CommunicationGuiController {
    /** The groups of {@code groupMembers}; the look-up accepts them in any case. */
    private static final List<String> GROUPS = List.of("Donors", "Volunteers", "Beneficiaries", "Attendees");
    private static final int PREVIEW_PEOPLE = 12;

    private final CommunicationService service;
    private final CommunicationPanel panel;
    private final ScreenActions actions;
    private final Function<String, List<? extends Person>> groupMembers;
    private final Function<String, Person> findPerson;
    private final Function<String, Audience> eventAudience;
    private final String organisation;
    /** Only used to show, in the preview, the channel each person would get. */
    private final ChannelSelector selector = new ChannelSelector();

    /**
     * @param deliveries    the messages Communication sent since the last call ({@code CommunicationService::takeOutput})
     * @param groupMembers  the people in a group, by name ("donors"); refuses an unknown group
     * @param findPerson    any person by ID (DNR-, VOL-, ATT-, BEN-), or null if there is none
     * @param eventAudience an event by ID (EVT-) as its registered attendees, or null if there is none
     * @param organisation  the name signed under messages, for the preview (the service keeps its own copy private)
     */
    public CommunicationGuiController(CommunicationService service, Dialogs dialogs, Feed feed,
                                      Supplier<List<String>> deliveries,
                                      Function<String, List<? extends Person>> groupMembers,
                                      Function<String, Person> findPerson,
                                      Function<String, Audience> eventAudience,
                                      String organisation) {
        this.service = service;
        this.groupMembers = groupMembers;
        this.findPerson = findPerson;
        this.eventAudience = eventAudience;
        this.organisation = organisation;
        this.panel = new CommunicationPanel(GROUPS, List.of(ContactPreference.values()));
        this.actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);

        panel.onSend(() -> actions.run(this::send));
        panel.onPreview(() -> actions.run(this::preview));
        panel.onClear(() -> actions.run(this::clear));
        refresh();
    }

    public CommunicationPanel getPanel() {
        return panel;
    }

    /** Reloads the message log (it also grows when other screens trigger messages). */
    public void refresh() {
        panel.showLog(service.getLog());
    }

    // ---- Send ----

    private void send() {
        CommunicationPanel.Recipients kind = panel.recipients();
        Person person = null;
        List<? extends Person> people;
        String who;
        switch (kind) {
            case PERSON:
                person = requirePerson();
                people = List.of(person);
                who = person.toString();
                break;
            case EVENT:
                String eventId = normalizeId(panel.eventId(), "Event id");
                people = requireEvent(eventId).getAudience();
                if (people.isEmpty()) {
                    panel.showStatus("Nobody is registered for " + eventId + "; no message sent.");
                    return;
                }
                who = "the " + people.size() + " attendee(s) of " + eventId;
                break;
            default:
                String group = requireGroup();
                people = groupMembers.apply(group);
                if (people.isEmpty()) {
                    panel.showStatus("There are no " + group.toLowerCase(Locale.ROOT) + " yet; no message sent.");
                    return;
                }
                who = people.size() + " " + group.toLowerCase(Locale.ROOT);
        }
        String subject = requireText(panel.subject(), "Subject");
        String text = requireText(panel.text(), "Message text");
        boolean urgent = panel.urgent();
        ContactPreference forced = panel.forcedChannel();

        int logBefore = service.getLog().size();
        if (person != null) {
            if (!service.send(person, subject, text, urgent, forced)) {
                panel.showStatus("Nothing was sent to " + who + ".");
                throw new IllegalStateException("Nothing was sent to " + who
                        + ". The reason is in the notifications.");
            }
            List<SentMessage> log = service.getLog();
            panel.showStatus("Sent to " + who + " by " + log.get(log.size() - 1).getChannel()
                    + (forced == null ? " (their preferred channel)." : " (the channel you chose)."));
            return;
        }
        int reached = service.broadcast(people, subject, text, urgent, forced);
        StringBuilder status = new StringBuilder("Sent to " + reached + " of " + who);
        if (reached > 0) {
            status.append(": ").append(channelsUsed(logBefore));
        }
        status.append(rule(forced)).append('.');
        int skipped = people.size() - reached;
        if (skipped > 0) {
            status.append(' ').append(skipped).append(" could not be reached (see the notifications).");
        }
        panel.showStatus(status.toString());
    }

    /** "2 by Email, 1 by SMS": how the messages logged since {@code logBefore} went out (STRATEGY). */
    private String channelsUsed(int logBefore) {
        List<SentMessage> log = service.getLog();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SentMessage message : log.subList(logBefore, log.size())) {
            counts.merge(message.getChannel(), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((channel, count) -> parts.add(count + " by " + channel));
        return String.join(", ", parts);
    }

    private static String rule(ContactPreference forced) {
        return forced == null ? " (each person's preferred channel)" : " (one channel for everyone)";
    }

    // ---- Preview (DECORATOR) ----

    /**
     * The text as each channel would deliver it, built with the model's own
     * pieces: plain text, the urgent tag if ticked, then the channel's
     * decorators; the channel's send shows the final form (an SMS is cut to one text).
     */
    private void preview() {
        String subject = panel.subject().trim();
        String text = requireText(panel.text(), "Message text");
        MessageContent content = new PlainContent(text);
        if (panel.urgent()) {
            content = new UrgentTagDecorator(content);
        }
        Person sample = new PreviewRecipient();
        StringBuilder out = new StringBuilder("How a recipient would get it on each channel"
                + (panel.urgent() ? " (urgent)" : "") + ":");
        for (ContactPreference preference : ContactPreference.values()) {
            CommunicationChannel channel = selector.channelFor(preference);
            String body = channel.decorate(content, organisation).render();
            String delivered = channel.send(new Message(sample, subject, body));
            out.append("\n\n== ").append(channel.name()).append(" ==\n").append(delivered.stripIndent());
        }
        out.append("\n\n").append(whoGetsWhat());
        panel.showPreview(out.toString());
        panel.showStatus("Preview ready: the same text, dressed differently by each channel.");
    }

    /** The chosen recipients and the channel each would get (STRATEGY), or why they are not known yet. */
    private String whoGetsWhat() {
        List<? extends Person> people;
        try {
            people = chosenPeople();
        } catch (IllegalArgumentException e) {
            return "Recipients: " + e.getMessage() + ".";
        }
        if (people.isEmpty()) {
            return "Recipients: nobody (no message would be sent).";
        }
        ContactPreference forced = panel.forcedChannel();
        StringBuilder out = new StringBuilder("Recipients and their channel" + rule(forced) + ":");
        for (Person person : people.subList(0, Math.min(PREVIEW_PEOPLE, people.size()))) {
            CommunicationChannel channel = forced != null ? selector.channelFor(forced) : selector.channelFor(person);
            out.append("\n  ").append(person).append(" -> ").append(channel == null ? "nobody" : channel.name());
        }
        if (people.size() > PREVIEW_PEOPLE) {
            out.append("\n  ... and ").append(people.size() - PREVIEW_PEOPLE).append(" more");
        }
        return out.toString();
    }

    private List<? extends Person> chosenPeople() {
        switch (panel.recipients()) {
            case PERSON:
                return List.of(requirePerson());
            case EVENT:
                return requireEvent(normalizeId(panel.eventId(), "Event id")).getAudience();
            default:
                return groupMembers.apply(requireGroup());
        }
    }

    /** Stands in for a recipient in the preview, with every address filled in. */
    private static final class PreviewRecipient extends Person {
        PreviewRecipient() {
            super("PREVIEW", "Sample Recipient", "recipient@example.org", "+20100000000", ContactPreference.EMAIL);
        }
    }

    private void clear() {
        panel.clearMessage();
        panel.showStatus("Ready for a new message.");
    }

    // ---- Turning what the user entered into values ----

    private String requireGroup() {
        String group = panel.group();
        if (group == null) {
            throw new IllegalArgumentException("Choose a group");
        }
        return group;
    }

    private Person requirePerson() {
        String id = normalizeId(panel.personId(), "Person id");
        Person person = findPerson.apply(id);
        if (person == null) {
            throw new IllegalArgumentException("No person with id " + id);
        }
        return person;
    }

    private Audience requireEvent(String id) {
        Audience event = eventAudience.apply(id);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + id);
        }
        return event;
    }

    /** "att-1" becomes ATT-1, like the console screen. */
    private static String normalizeId(String text, String what) {
        return requireText(text, what).trim().toUpperCase(Locale.ROOT);
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return text;
    }
}
