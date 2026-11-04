# Design-Patterns: Non-Profit Management System

A Java system for a non-profit organization, with a console menu and a JavaFX window over the same model. It covers donations, volunteers, events, beneficiaries and communication, and is built to demonstrate the design patterns from the course: **Simple Factory, Factory Method, Observer, Strategy, Template Method, Decorator, Singleton, MVC**.

The original design plan is in [`docs/superpowers/specs/2026-10-24-nonprofit-system-design.md`](docs/superpowers/specs/2026-10-24-nonprofit-system-design.md). It planned a `NonProfitFacade`; that was dropped, and there is **no Facade** in the code: the main menu opens each module's own MVC screen, and each screen's controller calls its module service (from `DataStore`) directly. This README and the diagrams in `output/` describe the code as it is.

## Team and ownership

| Student | Name | Package | Responsibility | Patterns | Diagram | Status |
|---|---|---|---|---|---|---|
| 1 | _TBD_ | `org.nonprofit.donation` | Donors, online/check/in-kind donations, campaigns, receipts and tax documents | Factory Method, Template Method, Observer, MVC | [`output/donation/donation.puml`](output/donation/donation.puml) | **Done** |
| 2 | _TBD_ | `org.nonprofit.volunteer` | Volunteers, skills, availability, assignment, hours, certificates | Strategy, Template Method, MVC | [`output/volunteer/volunteer.puml`](output/volunteer/volunteer.puml) | **Done** |
| 3 | _TBD_ | `org.nonprofit.event` | Events, registration, ticketing, waitlist, attendance, reminders | Simple Factory, Decorator, Observer, MVC | [`output/event/event.puml`](output/event/event.puml) | **Done** |
| 4 | _TBD_ | `org.nonprofit.beneficiary` | Beneficiaries, needs, resource distribution, impact reports | Strategy, Template Method, MVC | [`output/beneficiary/beneficiary.puml`](output/beneficiary/beneficiary.puml) | **Done** |
| 5 | _TBD_ | `org.nonprofit.communication` | Email / SMS / social media channels, message composition | Strategy, Decorator, Observer, MVC | [`output/communication/communication.puml`](output/communication/communication.puml) | **Done** |
| 6 | _TBD_ | `org.nonprofit.core`, `org.nonprofit.app` + `App`, `org.nonprofit.gui` (`GuiApp`, `MainWindow`, `gui.common`) | Data store, event bus, console menu, GUI window and shared GUI helpers, overall diagram, requirements doc | Singleton, Observer, MVC | [`output/core/core.puml`](output/core/core.puml), [`output/overall/overall.puml`](output/overall/overall.puml) | **Done** |

**GUI screens.** Each module's GUI screen (`org.nonprofit.gui.<module>`: `<Module>Panel` + `<Module>GuiController`) belongs with that module's student; `GuiApp`, `MainWindow` and `gui.common` belong to Student 6. See [GUI (JavaFX)](#gui-javafx).

\* Student 3 added the minimal shared core that Events needs: `Person`, `ContactPreference`, `EventBus` (Singleton + Observer), `SystemEvent`, `SystemEventType` and `SystemObserver`. Student 6 owns these files and builds on them. Other modules publish on `EventBus` and should add their own topics to `SystemEventType`. `Person.updateContact(...)` lets every module edit a person's name and contact details (the ID never changes, and a blank name is refused).

**Source layout: one folder per pattern.** In every module, each design pattern's classes sit together in their own sub-package; the screen is in `mvc`, the service and its data in `model`, and the module's demo / runner stays at the module root:

| Module | `mvc` | `model` | Pattern folders |
|---|---|---|---|
| `donation` | Controller, View | Service, Donor, Campaign, DonationForm, DonationType | `factorymethod` (creators + Donation products), `templatemethod` (DonationDocument, DonationReceipt, TaxStatement) |
| `event` | Controller, View | Service, RegistrationResult, WaitlistEntry | `simplefactory` (EventFactory, EventType, Fundraiser, Workshop, OutreachProgram), `observer` (EventSubject, Event, EventObserver, EventChange, Attendee, EventBusNotifier), `decorator` (Ticket … TicketAddOn) |
| `beneficiary` | Controller, View | Service, Beneficiary, Need, Urgency, Distribution | `strategy` (AllocationStrategy + 3 rules, AllocationMode), `templatemethod` (ImpactReport + 2) |
| `volunteer` | Controller, View | Service, Volunteer, Task, HourEntry | `strategy` (AssignmentStrategy + 3 rules, AssignmentMode), `templatemethod` (Certificate, HoursCertificate, EventParticipationCertificate) |
| `communication` | Controller, View | Service, Message, SentMessage | `strategy` (CommunicationChannel + 3 channels, ChannelSelector), `decorator` (MessageContent, PlainContent, ContentDecorator + 3) |
| `app` | MainMenuController, MainMenuView | — | `singleton` (DataStore, BroadcastGroup); SampleData and SystemDemo stay at the root |
| `core` | — | Person, ContactPreference, Audience | `observer` (EventBus, SystemObserver, SystemEvent, SystemEventType); EventBus is also the Singleton |

Every folder has its own class diagram with an SVG next to it in `output/<module>/<folder>.puml` (e.g. `output/event/observer.puml`); `output/<module>/<module>.puml` shows all of a module's folders together, and `output/overall/overall.puml` shows every module, its folders and which folders depend on which. The diagrams are generated from the Java source.

**Observer names.** The Observer code uses the lecture's names. In the Events module, `EventSubject` is the Subject interface (`registerObserver`, `removeObserver`), `Event` implements it and calls its protected `notifyObservers(...)`, and `EventObserver.update(...)` is the Observer. The system-wide `EventBus` keeps the shorter names `subscribe` / `unsubscribe` / `publish`, which are the lecture's `registerObserver` / `removeObserver` / `notifyObservers`; `SystemObserver.onEvent(...)` is `update()`.

**Model classes don't print (MVC).** Services and model classes return text instead of printing it. Channels return what they delivered, `CommunicationService` keeps it in an outbox until a view calls `takeOutput()`, and an `Attendee` stores its notes until a view calls `takeNewNotes()`. Only the views (`MainMenuView` and the five module views), `App` and the demo classes print.

## Events module (Student 3)

| Pattern | Where | What it does |
|---|---|---|
| **Simple Factory** | `EventFactory`, `EventType` | The static `EventFactory.create(...)` is the only place that creates a `Fundraiser`, `Workshop` or `OutreachProgram` (their constructors are package-private), from an enum or from user input (`"workshop"`, `"outreach program"`). Adding an event kind means one subclass and one case. |
| **Decorator** | `Ticket`, `BasicTicket`, `TicketDecorator`, `VipTicket`, `MealTicket`, `ParkingTicket`, `TicketAddOn` | Ticket add-ons stack on a basic ticket, and each one adds to the price and the description. `BasicTicket` copies the event's base price when it is sold, so changing the price later does not change tickets already issued. |
| **Observer** | `EventSubject` (subject interface), `Event` (concrete subject), `EventObserver`, `Attendee`, `EventBusNotifier` | Registering for an event calls `registerObserver(attendee)`; cancelling calls `removeObserver`. Registered attendees are notified when an event is rescheduled, relocated or cancelled, or when a reminder goes out. `EventBusNotifier` forwards these notifications to the system `EventBus` for the Communication module. People on the waitlist are not observers, so when an event is cancelled each of them is told directly (and a message is published for them on the bus). |
| **MVC** | `EventView` (view), `EventController` (controller), `EventService` and the events, tickets and attendees it manages (model) | The Events screen the main menu opens (option 0 says "Back" there). The view only prints menus and reads input; the controller turns each choice into calls on `EventService`. After each action the view shows the messages sent (the controller is given `CommunicationService::takeOutput` as a `Supplier`, so this module does not import Communication) and every attendee's new inbox notes. |

Other features: capacity limits, a **waitlist with automatic promotion** when someone cancels, check-in and attendance reports, ticket revenue, fundraiser pledge progress, reminders for events starting within a time window, and editing an event (rename, capacity, base ticket price, type details). A cancelled event refuses edits, reminders, check-in and new registrations; an event that has already started refuses registration and reminders when the current time is passed in; an event can only be deleted while nobody is registered or waitlisted (otherwise cancel it so people are told); raising the capacity promotes people from the waitlist. `EventService` is the module's entry point and also keeps the attendees on file (`addAttendee`, `findAttendee`, `listAttendees`), so the Events screen can pick an existing person by `ATT-` id.

## Donations module (Student 1)

| Pattern | Where | What it does |
|---|---|---|
| **Factory Method** | `DonationCreator` (creator), `OnlineDonationCreator`, `CheckDonationCreator`, `InKindDonationCreator` (concrete creators), `Donation` and its subclasses `OnlineDonation`, `CheckDonation`, `InKindDonation` (products) | `DonationCreator.receive()` is final and is the same for every donation: call the factory method `createDonation()`, then add the donation to the donor's history. Each concrete creator decides which donation class to build. `DonationService` only knows the abstract creator and the abstract `Donation`. A new donation method means one new product class plus one new creator, plugged in with `addCreator()`. |
| **Template Method** | `DonationDocument`, `DonationReceipt`, `TaxStatement` | `DonationDocument.generate()` is final and always prints header, body, totals, footer. A receipt covers one donation. A tax statement lists a donor's donations for one year, with money and goods totalled separately. The `canBeIssued()` hook stops an empty statement from being printed. |
| **Observer** | `DonationService` (publisher), `DonationView` and `CommunicationService` (observers) | Every new donation is published on the `EventBus` as `DONATION_RECEIVED`. The Communication module sends the thank-you message and the screen shows a notification. `DonationService` never calls either of them directly. |
| **MVC** | `DonationView`, `DonationController`, `DonationService` (model), `DonationApp` | An interactive console screen. The view only prints and reads input, and the controller turns menu choices into calls on the service. |

Client requirements collected for this module: **monthly online gifts** (shown on the receipt) and **campaigns with a money goal and progress percentage**. Other features: totals by method, top donors, input checks (missing check number, negative amount, unknown campaign...), donation numbers with no gaps when a bad donation is rejected, and donors who have already given can't be deleted. `DonationService` is the module's entry point; the main menu opens this module's own screen (`DonationController` + `DonationView`) on the shared `DonationService` from `DataStore`.

Fixes made while integrating it into the main menu: donor contact details can be edited (`updateDonor`, menu option 11); the screen asks for the date received (empty = today) so a donation can be backdated into a tax year, and `donate(..., today)` refuses a future date; an `Infinity` amount or campaign goal is refused like `NaN`; donor and donation ids can be typed in any case (`dnr-1`, `don-1`).

## Volunteers module (Student 2)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `AssignmentStrategy`, `SkillBasedAssignment`, `AvailabilityBasedAssignment`, `PreferenceBasedAssignment`, `AssignmentMode` | `VolunteerService` holds one strategy that picks who gets a task. It can be swapped at runtime (or chosen from user input such as `"preference"`), so the same task can go to a different volunteer. |
| **Template Method** | `Certificate`, `HoursCertificate`, `EventParticipationCertificate` | `Certificate.issue()` is final and always builds title, recipient, achievement and signature. Subclasses supply only the title and achievement, and can override the `isEligible()` hook. |
| **MVC** | `VolunteerView` (view), `VolunteerController` (controller), `VolunteerService` and the classes it manages (model), `VolunteerApp` (starts it) | A console screen for the module. The view only prints menus and reads input; the controller turns each menu choice into calls on the model and tells the view what to show. The view also observes the `EventBus`, so it shows a notification when the model assigns a task. When opened from the main menu, the controller is also given `CommunicationService::takeOutput` (as a `Supplier`), so the view shows the messages sent after each action without this module importing Communication. |

Other features: volunteer profiles (skills, available days, preferred task categories), one task per volunteer per day, hour logging, a **leaderboard by hours** (extended requirement 4), suggestions without assigning, freeing a removed volunteer's open tasks, editing contact details (`updateVolunteerContact`) and tasks (`updateTask`; a completed task cannot change), and adding/removing skills, days and preferences through `VolunteerService` (a skill or day that one of the volunteer's unfinished tasks needs cannot be removed). Assignments are published on the `EventBus` as `VOLUNTEER_ASSIGNED` for the Communication module. `VolunteerService` is the module's entry point and is what the Volunteers screen calls.

## Beneficiaries module (Student 4)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `AllocationStrategy`, `UrgencyFirstAllocation`, `FirstComeFirstServedAllocation`, `EqualShareAllocation`, `AllocationMode` | When stock is limited, `BeneficiaryService` asks its current strategy how to share it between the open needs of one category. It can be swapped at runtime (or chosen from user input such as `"equal"`), so the same stock can go to different people. `preview(...)` shows the result without handing anything out. |
| **Template Method** | `ImpactReport`, `ProgramImpactReport`, `BeneficiaryProgressReport` | `ImpactReport.generate()` is final and always runs collect data, compute metrics, format rows, summary, footer. A program report shows what one program handed out and how many needs it fully met; a progress report shows one beneficiary's needs, met and open. A report with no data is refused with an `IllegalStateException`. |
| **MVC** | `BeneficiaryView` (view), `BeneficiaryController` (controller), `BeneficiaryService` and the beneficiaries, needs and distributions it manages (model) | The Beneficiaries screen the main menu opens (option 0 says "Back" there). The view only prints menus and reads input; the controller turns each choice into calls on `BeneficiaryService`, and the view shows the messages sent after each action (given as a `Supplier`, so this module does not import Communication). |

Other features: needs counted in units, so they cover both resources (food parcels) and services (counselling sessions); distributions can never exceed what is still needed; services given directly to one person (`giveDirectly`); beneficiaries and needs can be updated (`updateBeneficiary`, `updateNeed`); and records with history cannot be removed. A HIGH-urgency need is published on the `EventBus` as `URGENT_NEED` (extended requirement 5) to the urgent responders set with `setUrgentResponders(Audience)`. `DataStore` passes the volunteers there when it starts up, so this module never imports the Volunteers package. Each distribution is published as `RESOURCE_DISTRIBUTED` to the beneficiary. `BeneficiaryService` is the module's entry point and is what the Beneficiaries screen calls.

## Communication module (Student 5)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `CommunicationChannel`, `EmailChannel`, `SmsChannel`, `SocialMediaChannel`, `ChannelSelector` | Each message is sent through a channel picked per recipient from their preferred contact (falling back to one they have details for). A caller can force one channel for a whole broadcast. `send(...)` returns the delivered text instead of printing it. |
| **Decorator** | `MessageContent`, `PlainContent`, `ContentDecorator`, `UrgentTagDecorator`, `SignatureDecorator`, `FooterDecorator` | The text is built by stacking an urgent tag, a signature and an unsubscribe footer around the plain text. Each channel decides its own decorations through `decorate(content, organisation)`: e-mail adds the signature and footer, social media the signature only, SMS nothing (the interface's default). `CommunicationService` only adds the urgent tag and then calls `channel.decorate(...)`, so it never checks which channel it has (no `instanceof`; it programs to the interface). |
| **Observer** | `CommunicationService` | Subscribes to every `EventBus` topic and turns donations, assignments, registrations and event changes into messages. A system event with no recipient is sent to every person in its source's `Audience` (`Event` implements it), so Communication never depends on the Events package. |
| **MVC** | `CommunicationView` (view), `CommunicationController` (controller), `CommunicationService` (model) | The Communication screen the main menu opens: 1 broadcast to a group, 2 send a message to one person, 3 message the attendees of one event, 4 show the message log. The people live in other modules, so `App` gives the controller three look-ups that return only core types: group name → people (`DataStore.membersOf`), any person id → `Person` (`DataStore.findPerson`) and event id → `Audience` (`DataStore.eventAudience`). This module imports no other module. |

Other features: SMS cut to 160 characters, a communication log (`getLog()`), `broadcast(...)` and `send(...)` for manual sends (to a group, one person or one event's attendees), and an outbox of delivered text that the views show after each action (`takeOutput()`).

## Core and main menu (Student 6)

The new classes live in `org.nonprofit.app`, not in `core`. They have to import every module, and every module imports `core`, so putting them in `core` would make the packages depend on each other. Dependencies only point down: `app` → modules → `core`.

| Pattern | Where | What it does |
|---|---|---|
| **Singleton** | `DataStore` | Lazily created, thread-safe single instance that owns the one copy of each module service. `CommunicationService` is created only here, so no message is sent twice. It also passes the volunteers to `BeneficiaryService` as the urgent responders. For the Communication screen, `membersOf(...)` builds the recipient list for a group broadcast, `findPerson(id)` finds a donor, volunteer, attendee or beneficiary by id, and `eventAudience(id)` returns an event as a core `Audience`. |
| **MVC** | `MainMenuView` (view), `MainMenuController` (controller), the five module screens over the module services (model) | Main menu with 1 Donations, 2 Volunteers, 3 Events, 4 Beneficiaries, 5 Communication, 0 Exit. Each option opens that module's own MVC screen (`DonationController`, `VolunteerController`, `EventController`, `BeneficiaryController`, `CommunicationController`, each with its view) on the shared service from `DataStore` (their option 0 says "Back" there). `App` builds the five screens with one shared `Scanner`, so piped input is not lost. In every screen results start with `>>`, errors with `!!`, IDs can be typed in any case (`ben-1`), bad input never ends the program, and the messages Communication sent are shown after each action. |

What the five module screens offer:

- **Donations**: the module's own screen (list, add, remove and update donors; add and list campaigns; make a donation with a typed date, default today, so it can be backdated into a tax year while a future date is refused; list donations; receipts and tax statements; summary and top donors). The thank-you messages Communication sends are shown after each action.
- **Events**: list all events or the upcoming ones by date; create; edit (rename, reschedule, relocate, capacity, base ticket price, fundraiser goal and pledges, workshop topic and instructor, outreach target community); register an attendee with add-ons (a new person is checked first and only stored when the registration goes through, and their `ATT-` id is printed); cancel a registration or leave the waitlist; check in; update an attendee's contact details; a reminder for one event or for the next 24 hours; attendance report; cancel or delete an event (only an event nobody has signed up for can be deleted). Registration and reminders use the current time, so an event that has started is refused, and a cancelled event refuses every edit.
- **Beneficiaries**: list, register, update and remove beneficiaries; add, update and remove needs; list open needs; change the allocation rule; preview a distribution before handing anything out; distribute stock; give units directly to one need; list distributions; impact reports.
- **Volunteers**: the module's own screen (profiles, contact details, tasks including updating one, assignment rule, hours, leaderboard, certificates).
- **Communication**: broadcast to a group; send a message to one person by any id (`DNR-`, `VOL-`, `ATT-`, `BEN-`; their preferred channel unless one is forced, optionally urgent); message the attendees registered for one event (`EVT-` id); show the message log.

`App` loads sample data (`SampleData`) and opens the main menu. `App --demo` runs `SystemDemo`, a fixed walkthrough that calls the module services and shows every pattern in the system (Singleton, Simple Factory (events) / Factory Method (donations), Strategy, Observer, Decorator, Template Method, and finally MVC by driving the main menu with scripted input), with the same output on every run.

## Running

From the `SDP` folder:

```sh
javac --release 17 -d bin $(find src -name "*.java")
java -cp bin App            # main menu (whole system)
java -cp bin App --demo     # scripted walkthrough of every module and pattern
java -cp bin org.nonprofit.event.EventDemo
java -cp bin org.nonprofit.volunteer.VolunteerDemo
java -cp bin org.nonprofit.volunteer.VolunteerApp
java -cp bin org.nonprofit.donation.DonationDemo
java -cp bin org.nonprofit.donation.DonationApp
java -cp bin org.nonprofit.communication.CommunicationDemo
java -cp bin org.nonprofit.beneficiary.BeneficiaryDemo
```

In VS Code, open `SDP/src/App.java` and click **Run** for the main menu, or open `SDP/src/org/nonprofit/event/EventDemo.java`, `SDP/src/org/nonprofit/volunteer/VolunteerDemo.java` or `SDP/src/org/nonprofit/donation/DonationDemo.java` and click **Run**. `VolunteerApp` and `DonationApp` open the interactive Volunteers and Donations screens; the `...Demo` classes run a fixed walkthrough. The JavaFX window needs the JavaFX SDK and its own commands: see [GUI (JavaFX)](#gui-javafx) below. If the SDK is missing, the first `javac` line fails on the `gui` files; build with `-not -path "*/gui/*"` instead.

## GUI (JavaFX)

A JavaFX window with a sidebar (**Donations, Volunteers, Events, Beneficiaries, Communication**), the selected module's screen in the middle and a live **notification feed** at the bottom (the e-mails, SMS and posts Communication sent, and the Events attendees' new notes). It is a second **View + Controller** layer on the **same model** as the console menu: MVC with two views and one model. The module services in `DataStore` were not changed for it, and what you do in the GUI is the same data the console would show. All GUI code is under `org.nonprofit.gui`; each `gui.<module>` package imports only its own module, `core` and `gui.common`, so modules still never import each other.

| Class | Role |
|---|---|
| `gui.GuiApp` | Loads the sample data into the `DataStore` and opens the window |
| `gui.MainWindow` | Builds the five module controllers on their services from `DataStore`, the sidebar and the notification feed |
| `gui.<module>.<Module>Panel` | **View**: JavaFX controls only (tables, buttons, drop-downs); never calls a service |
| `gui.<module>.<Module>GuiController` | **Controller**: turns each click into calls on the module service and tells the panel what to show |
| `gui.common` | Shared helpers: `Dialogs` / `FxDialogs` (error, info, confirm, monospace text for receipts, certificates and reports, forms), `Form` / `FormValues` (a form described without JavaFX), `Feed` / `NotificationFeed`, `ScreenActions` (runs an action, shows a broken rule as an error dialog, refreshes, moves Communication's output to the feed), `Ui` (titles, toolbars, sections, tables), `Inputs` (number parsing), `Theme` (the stylesheet) |

Where each pattern can be seen in the window:

| Screen | Pattern | What you see |
|---|---|---|
| Donations | **Factory Method** | A bar lists the creator for each donation type and names the creator that built the last donation (e.g. `OnlineDonationCreator built OnlineDonation DON-5 ...`) |
| | **Template Method** | Receipt and tax statement open as text documents |
| Volunteers | **Strategy** | The assignment rule drop-down swaps the strategy live; **Suggest** (and selecting a task) shows who the current rule picks |
| | **Template Method** | Hours and event participation certificates |
| Events | **Simple Factory** | **Create...** picks the event type (fundraiser, workshop, outreach program) |
| | **Decorator** | Registering offers the VIP / Meal / Parking add-ons with their prices; the ticket's description and total price come from the stacked decorators |
| | **Observer** | After each action the attendees' new notes (reschedule, cancel, reminder) appear in the feed as `[inbox of ...]` |
| Beneficiaries | **Strategy** | The allocation rule drop-down plus **Preview split...** shows how the same stock would be shared under the current rule |
| | **Template Method** | Progress report (one beneficiary) and program impact report |
| | | HIGH-urgency needs are shown with a red badge |
| Communication | **Strategy** | Channel per person from their preferred contact, or one channel forced for the whole send; the status line counts who each channel reached |
| | **Decorator** | **Preview** shows the same text as each channel would deliver it (e-mail with signature and footer, social media with signature, SMS plain) |
| Feed | **Observer** | Messages Communication sent in reaction to `EventBus` events (thank-yous, assignments, registrations, urgent needs) |
| All | **Singleton** | `DataStore` is the one model shared by the GUI and the console |

### Setup (every teammate, once)

JavaFX is not part of the JDK.

1. Download the **JavaFX 21 SDK** (a newer SDK such as 22 also works; keep the folder name below) (the *SDK*, not *jmods*) for your OS from <https://gluonhq.com/products/javafx/>: macOS Apple Silicon = aarch64, Intel Mac = x64, Windows x64, Linux x64.
2. Unzip it and rename/move the folder so the jars are at **`SDP/lib/javafx-sdk-21/lib/`** (e.g. `SDP/lib/javafx-sdk-21/lib/javafx.controls.jar`).
3. The folder is in `.gitignore` because it is different for each OS: never commit it.
4. macOS: if Gatekeeper blocks the native libraries when the window opens, run `xattr -dr com.apple.quarantine SDP/lib/javafx-sdk-21`.

### Run

In VS Code: **Run and Debug** → **GUI (JavaFX)** (`SDP/.vscode/launch.json` passes the module path). The same list has **Console menu** and **Console demo (App --demo)**.

From the `SDP` folder, macOS / Linux:

```sh
javac --release 17 --module-path lib/javafx-sdk-21/lib --add-modules javafx.controls -d bin $(find src -name "*.java")
java --module-path lib/javafx-sdk-21/lib --add-modules javafx.controls --enable-native-access=javafx.graphics -cp bin org.nonprofit.gui.GuiApp
```

Windows PowerShell (paths use `\`; a classpath with several entries would use `;`):

```powershell
javac --release 17 --module-path lib\javafx-sdk-21\lib --add-modules javafx.controls -d bin (Get-ChildItem -Recurse -Filter *.java src | % FullName)
java --module-path lib\javafx-sdk-21\lib --add-modules javafx.controls --enable-native-access=javafx.graphics -cp bin org.nonprofit.gui.GuiApp
```

### Without JavaFX

The console app still builds if you skip the GUI package:

```sh
javac --release 17 -d bin $(find src -name "*.java" -not -path "*/gui/*")
java -cp bin App
```

(PowerShell: `javac --release 17 -d bin (Get-ChildItem -Recurse -Filter *.java src | ? FullName -notmatch '\\gui\\' | % FullName)`.) Without the SDK, VS Code shows errors in the `gui/` files; the **Console menu** and **Console demo** run configurations still work.

## Diagrams

The class diagrams are PlantUML files in the top-level **`output/`** folder: one per package plus `output/overall/overall.puml` for the whole system. Each file's header comment says where it came from:

- `output/core/core.puml` and `output/event/event.puml` started as output of the team's UML generator and were regrouped by pattern; they are now kept up to date by hand (last updated from the source on 2026-11-03). Regenerating them would lose the grouping.
- `output/donation/donation.puml`, `output/volunteer/volunteer.puml`, `output/beneficiary/beneficiary.puml`, `output/communication/communication.puml` and `output/overall/overall.puml` are written by hand from the source.
- When the code changes, update the matching diagram by hand.
- All of them show only the important members. You can view any of them with the VS Code *PlantUML* extension, or check them with `plantuml -checkonly output/*.puml`.
