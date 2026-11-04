import java.io.PrintStream;
import java.util.Scanner;

import org.nonprofit.app.singleton.DataStore;
import org.nonprofit.app.mvc.MainMenuController;
import org.nonprofit.app.mvc.MainMenuView;
import org.nonprofit.app.SampleData;
import org.nonprofit.app.SystemDemo;
import org.nonprofit.beneficiary.mvc.BeneficiaryController;
import org.nonprofit.beneficiary.mvc.BeneficiaryView;
import org.nonprofit.communication.mvc.CommunicationController;
import org.nonprofit.communication.model.CommunicationService;
import org.nonprofit.communication.mvc.CommunicationView;
import org.nonprofit.donation.mvc.DonationController;
import org.nonprofit.donation.mvc.DonationView;
import org.nonprofit.event.observer.Attendee;
import org.nonprofit.event.mvc.EventController;
import org.nonprofit.event.mvc.EventView;
import org.nonprofit.volunteer.mvc.VolunteerController;
import org.nonprofit.volunteer.mvc.VolunteerView;

/**
 * Starts the non-profit management system.
 * <ul>
 *   <li>{@code java -cp bin App} loads sample data and opens the console menu
 *       (MVC: MainMenuView and MainMenuController, which open the five module screens, each its own
 *       View and Controller over a module service in the DataStore Singleton).</li>
 *   <li>{@code java -cp bin App --demo} runs the scripted walkthrough of every module and pattern.</li>
 * </ul>
 */
public class App {

    public static void main(String[] args) {
        DataStore store = DataStore.getInstance();
        if (args.length > 0 && args[0].equals("--demo")) {
            SystemDemo.run(store);
            return;
        }

        // The sample data sends its own thank-yous and notices; keep them off the screen
        // by taking and dropping the messages it sent and the attendees' notes.
        PrintStream console = System.out;
        SampleData.load(store);
        store.getCommunicationService().takeOutput();
        for (Attendee attendee : store.getEventService().listAttendees()) {
            attendee.takeNewNotes();
        }
        console.println(">> Sample data loaded: " + store.getDonationService().getDonors().size() + " donors, "
                + store.getVolunteerService().listVolunteers().size() + " volunteers, "
                + store.getEventService().listEvents().size() + " events, "
                + store.getBeneficiaryService().listBeneficiaries().size() + " beneficiaries.");

        // One Scanner for all screens, so none swallows another's input.
        // The module screens are made after the sample data, so their notifications start now.
        // Each screen shows what Communication sent after every action; the Communication
        // screen reaches the other modules' people only through the DataStore look-ups.
        Scanner in = new Scanner(System.in);
        CommunicationService communication = store.getCommunicationService();
        DonationController donationScreen = new DonationController(
                store.getDonationService(), new DonationView(in), true, communication::takeOutput);
        VolunteerController volunteerScreen = new VolunteerController(
                store.getVolunteerService(), new VolunteerView(in, console), true, communication::takeOutput);
        EventController eventScreen = new EventController(
                store.getEventService(), new EventView(in, console), true, communication::takeOutput);
        BeneficiaryController beneficiaryScreen = new BeneficiaryController(
                store.getBeneficiaryService(), new BeneficiaryView(in, console), true, communication::takeOutput);
        CommunicationController communicationScreen = new CommunicationController(
                communication, new CommunicationView(in, console), true,
                store::membersOf, store::findPerson, store::eventAudience);
        new MainMenuController(new MainMenuView(in, console), donationScreen, volunteerScreen,
                eventScreen, beneficiaryScreen, communicationScreen).run();
    }
}
