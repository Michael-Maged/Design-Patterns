package org.nonprofit.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.nonprofit.app.DataStore;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.NotificationFeed;
import org.nonprofit.gui.common.Ui;
import org.nonprofit.gui.beneficiary.BeneficiaryGuiController;
import org.nonprofit.gui.communication.CommunicationGuiController;
import org.nonprofit.gui.donation.DonationGuiController;
import org.nonprofit.gui.event.EventGuiController;
import org.nonprofit.gui.volunteer.VolunteerGuiController;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * The main window (MVC view of the whole GUI, like the console main menu):
 * a sidebar with the five modules, the selected module's screen in the
 * middle and the notification feed at the bottom.
 *
 * Each module screen is its own View + Controller over its module service
 * from the {@link DataStore}; this class only puts them in the window.
 */
public class MainWindow {
    private final BorderPane root = new BorderPane();
    private final VBox sidebar = new VBox();
    private final ToggleGroup sidebarGroup = new ToggleGroup();
    private final NotificationFeed feed = new NotificationFeed();
    private final Map<String, Node> screens = new LinkedHashMap<>();
    private final Map<String, Runnable> onShow = new LinkedHashMap<>();
    private final Map<String, ToggleButton> buttons = new LinkedHashMap<>();
    private final Supplier<List<String>> deliveries;

    public MainWindow(DataStore store, Dialogs dialogs) {
        // What Communication sent (e-mails, SMS, posts) since the last action; each screen moves it to the feed.
        deliveries = store.getCommunicationService()::takeOutput;
        buildSidebar();

        // ===================== MODULE SCREENS =====================
        // One entry per module, in sidebar order. To plug a screen in, replace its
        // placeholder line with:  addModule("<Name>", controller.getPanel(), controller::refresh);
        DonationGuiController donations = new DonationGuiController(
                store.getDonationService(), dialogs, feed, deliveries);
        addModule("Donations", donations.getPanel(), donations::refresh);
        VolunteerGuiController volunteers = new VolunteerGuiController(
                store.getVolunteerService(), dialogs, feed, deliveries);
        addModule("Volunteers", volunteers.getPanel(), volunteers::refresh);
        EventGuiController events = new EventGuiController(
                store.getEventService(), dialogs, feed, deliveries);
        addModule("Events", events.getPanel(), events::refresh);
        BeneficiaryGuiController beneficiaries = new BeneficiaryGuiController(
                store.getBeneficiaryService(), dialogs, feed, deliveries);
        addModule("Beneficiaries", beneficiaries.getPanel(), beneficiaries::refresh);
        CommunicationGuiController communication = new CommunicationGuiController(
                store.getCommunicationService(), dialogs, feed, deliveries,
                store::membersOf, store::findPerson, store::eventAudience, DataStore.ORGANISATION);
        addModule("Communication", communication.getPanel(), communication::refresh);
        // ==========================================================

        VBox feedBox = new VBox(new Label("Notifications (e-mails, SMS, posts, attendee notes)"), feed.getView());
        feedBox.getChildren().get(0).getStyleClass().add("feed-title");
        feedBox.getStyleClass().add("feed-box");

        root.setLeft(sidebar);
        root.setBottom(feedBox);
        select(screens.keySet().iterator().next());
    }

    private void buildSidebar() {
        Label name = new Label(DataStore.ORGANISATION);
        name.getStyleClass().add("app-name");
        Label tagline = new Label("Non-profit management");
        tagline.getStyleClass().add("app-tagline");
        sidebar.getChildren().addAll(name, tagline);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(190);
        sidebar.setMinWidth(190);
    }

    private void addModule(String name, Node screen) {
        addModule(name, screen, () -> { });
    }

    /** @param refresh run every time the screen is opened, so it shows changes made on other screens */
    private void addModule(String name, Node screen, Runnable refresh) {
        ToggleButton button = new ToggleButton(name);
        button.setId("nav-" + name.toLowerCase(Locale.ROOT));
        button.setToggleGroup(sidebarGroup);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> select(name));
        sidebar.getChildren().add(button);
        screens.put(name, screen);
        onShow.put(name, refresh);
        buttons.put(name, button);
    }

    /** Opens a module's screen by its sidebar name, e.g. "Volunteers". */
    public void select(String name) {
        Node screen = screens.get(name);
        if (screen == null) {
            throw new IllegalArgumentException("No module " + name);
        }
        // A toggle button can be clicked off; keep the open screen's button pressed.
        buttons.get(name).setSelected(true);
        onShow.get(name).run();
        root.setCenter(screen);
        feed.postAll(deliveries.get());
    }

    public Parent getRoot() {
        return root;
    }

    public NotificationFeed getFeed() {
        return feed;
    }
}
