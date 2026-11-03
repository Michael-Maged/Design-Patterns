package org.nonprofit.gui;

import org.nonprofit.app.DataStore;
import org.nonprofit.app.SampleData;
import org.nonprofit.event.Attendee;
import org.nonprofit.gui.common.FxDialogs;
import org.nonprofit.gui.common.Theme;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Starts the JavaFX version of the system: loads the sample data into the
 * {@link DataStore} Singleton (the same model the console {@code App} uses)
 * and opens the {@link MainWindow}.
 *
 * <pre>
 * javac --release 17 --module-path lib/javafx-sdk-21/lib --add-modules javafx.controls -d bin $(find src -name "*.java")
 * java --module-path lib/javafx-sdk-21/lib --add-modules javafx.controls -cp bin org.nonprofit.gui.GuiApp
 * </pre>
 */
public class GuiApp extends Application {

    @Override
    public void start(Stage stage) {
        DataStore store = DataStore.getInstance();
        // Like App: the sample data sends its own thank-yous and notices; drop them
        // so the feed starts with what happens in this session.
        SampleData.load(store);
        store.getCommunicationService().takeOutput();
        for (Attendee attendee : store.getEventService().listAttendees()) {
            attendee.takeNewNotes();
        }

        MainWindow window = new MainWindow(store, new FxDialogs(stage));
        window.getFeed().post("Sample data loaded: " + store.getDonationService().getDonors().size() + " donors, "
                + store.getVolunteerService().listVolunteers().size() + " volunteers, "
                + store.getEventService().listEvents().size() + " events, "
                + store.getBeneficiaryService().listBeneficiaries().size() + " beneficiaries.");

        Scene scene = new Scene(window.getRoot(), 1280, 820);
        Theme.apply(scene);
        stage.setTitle(DataStore.ORGANISATION);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
