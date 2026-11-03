import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.nonprofit.app.DataStore;
import org.nonprofit.app.MainMenuController;
import org.nonprofit.app.MainMenuView;
import org.nonprofit.app.NonProfitFacade;
import org.nonprofit.app.SampleData;
import org.nonprofit.app.SystemDemo;
import org.nonprofit.volunteer.VolunteerController;
import org.nonprofit.volunteer.VolunteerView;

/**
 * Starts the non-profit management system.
 * <ul>
 *   <li>{@code java -cp bin App} loads sample data and opens the console menu (MVC over the FACADE).</li>
 *   <li>{@code java -cp bin App --demo} runs the scripted walkthrough of every module and pattern.</li>
 * </ul>
 */
public class App {

    public static void main(String[] args) {
        NonProfitFacade facade = new NonProfitFacade();
        if (args.length > 0 && args[0].equals("--demo")) {
            SystemDemo.run(facade);
            return;
        }

        // The sample data sends its own thank-yous and notices; keep them off the screen.
        PrintStream console = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            SampleData.load(facade);
        } finally {
            System.setOut(console);
        }
        console.println(">> Sample data loaded: " + facade.listDonors().size() + " donors, "
                + facade.listVolunteers().size() + " volunteers, " + facade.listEvents().size() + " events, "
                + facade.listBeneficiaries().size() + " beneficiaries.");

        // One Scanner for both screens, so neither swallows the other's input.
        Scanner in = new Scanner(System.in);
        VolunteerController volunteerScreen = new VolunteerController(
                DataStore.getInstance().getVolunteerService(), new VolunteerView(in, console));
        new MainMenuController(facade, new MainMenuView(in, console), volunteerScreen).run();
    }
}
