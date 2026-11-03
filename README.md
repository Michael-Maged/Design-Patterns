# Design-Patterns: Non-Profit Management System

A Java console system for a non-profit organization. It covers donations, volunteers, events, beneficiaries and communication, and is built to demonstrate the design patterns from the course: **Factory, Observer, Strategy, Template Method, Facade, Decorator, Singleton**.

The full design is in [`docs/superpowers/specs/2026-10-24-nonprofit-system-design.md`](docs/superpowers/specs/2026-10-24-nonprofit-system-design.md).

## Team and ownership

| Student | Name | Package | Responsibility | Patterns | Diagram | Status |
|---|---|---|---|---|---|---|
| 1 | _TBD_ | `org.nonprofit.donation` | Donors, online/check/in-kind donations, receipts and tax documents | Factory Method, Template Method | [`output/donation.puml`](output/donation.puml) | **Done** |
| 2 | _TBD_ | `org.nonprofit.volunteer` | Volunteers, skills, availability, assignment, hours, certificates | Strategy, Template Method | [`output/volunteer.puml`](output/volunteer.puml) | **Done** |
| 3 | _TBD_ | `org.nonprofit.event` | Events, registration, ticketing, waitlist, attendance, reminders | Factory, Decorator, Observer | [`output/event.puml`](output/event.puml) | **Done** |
| 4 | _TBD_ | `org.nonprofit.beneficiary` | Beneficiaries, needs, resource distribution, impact reports | Strategy, Template Method | [`output/beneficiary.puml`](output/beneficiary.puml) | **Done** |
| 5 | _TBD_ | `org.nonprofit.communication` | Email / SMS / social media channels, message composition | Strategy, Decorator, Observer | [`output/communication.puml`](output/communication.puml) | **Done** |
| 6 | _TBD_ | `org.nonprofit.core`, `org.nonprofit.app` + `App` | Data store, event bus, system facade, console menu, overall diagram, requirements doc | Singleton, Observer, Facade, MVC | [`output/core.puml`](output/core.puml), [`output/overall.puml`](output/overall.puml) | **Done** |

\* Student 3 added the minimal shared core that Events needs: `Person`, `ContactPreference`, `EventBus` (Singleton + Observer), `SystemEvent`, `SystemEventType` and `SystemObserver`. Student 6 owns these files and builds on them. Other modules publish on `EventBus` and should add their own topics to `SystemEventType`.

## Events module (Student 3)

| Pattern | Where | What it does |
|---|---|---|
| **Factory** | `EventFactory`, `EventType` | Creates a `Fundraiser`, `Workshop` or `OutreachProgram` from an enum or from user input (`"workshop"`, `"outreach program"`). |
| **Decorator** | `Ticket`, `BasicTicket`, `TicketDecorator`, `VipTicket`, `MealTicket`, `ParkingTicket`, `TicketAddOn` | Ticket add-ons stack on a basic ticket, and each one adds to the price and the description. |
| **Observer** | `Event` (subject), `EventObserver`, `Attendee`, `EventBusNotifier` | Registered attendees are notified when an event is rescheduled, relocated or cancelled, or when a reminder goes out. `EventBusNotifier` forwards these notifications to the system `EventBus` for the Communication module. |

Other features: capacity limits, a **waitlist with automatic promotion** when someone cancels, check-in and attendance reports, ticket revenue, fundraiser pledge progress, and reminders for events starting within a time window. `EventService` is the module's entry point and is what the Facade will call.

## Donations module (Student 1)

| Pattern | Where | What it does |
|---|---|---|
| **Factory Method** | `DonationProcessor` (creator), `OnlineDonationProcessor`, `CheckDonationProcessor`, `InKindDonationProcessor` (concrete creators), `Donation` and its three subclasses (products) | `DonationProcessor.receive()` is the same for every donation: create it, give it the next number, add it to the donor's history. The create step is the factory method `createDonation()`, and each concrete creator decides which donation class to build, like the textbook's `PizzaStore`. `DonationService` only depends on the abstract creator and the abstract `Donation`. |
| **Template Method** | `DonationDocument`, `DonationReceipt`, `TaxDeductionStatement` | `DonationDocument.generate()` is final and always prints header, body, totals and footer. A receipt covers one donation; a tax statement lists a donor's donations for one year with money and goods totals. A document with nothing to show (a tax year with no donations) is refused with an `IllegalStateException` instead of being printed. |

Other features: recurring monthly online gifts shown on the receipt (extended requirement 1), **campaign goals with progress** (extended requirement 2), totals by donation method, top donors, receipt numbers with no gaps when a bad donation is rejected, and donor records kept while they have donations. Each donation is published on the `EventBus` as `DONATION_RECEIVED` so the Communication module can thank the donor. `DonationService` is the module's entry point and is what the Facade will call.

## Volunteers module (Student 2)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `AssignmentStrategy`, `SkillBasedAssignment`, `AvailabilityBasedAssignment`, `PreferenceBasedAssignment`, `AssignmentMode` | `VolunteerService` holds one strategy that picks who gets a task. It can be swapped at runtime (or chosen from user input such as `"preference"`), so the same task can go to a different volunteer. |
| **Template Method** | `Certificate`, `HoursCertificate`, `EventParticipationCertificate` | `Certificate.issue()` is final and always prints title, recipient, achievement and signature. Subclasses supply only the title and achievement, and can override the `isEligible()` hook. |
| **MVC** | `VolunteerView` (view), `VolunteerController` (controller), `VolunteerService` and the classes it manages (model), `VolunteerApp` (starts it) | A console screen for the module. The view only prints menus and reads input; the controller turns each menu choice into calls on the model and tells the view what to show. The view also observes the `EventBus`, so it shows a notification when the model assigns a task. |

Other features: volunteer profiles (skills, available days, preferred task categories), one task per volunteer per day, hour logging, a **leaderboard by hours** (extended requirement 4), suggestions without assigning, and freeing a removed volunteer's open tasks. Assignments are published on the `EventBus` as `VOLUNTEER_ASSIGNED` for the Communication module. `VolunteerService` is the module's entry point and is what the Facade will call.

## Beneficiaries module (Student 4)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `AllocationStrategy`, `UrgencyFirstAllocation`, `FirstComeFirstServedAllocation`, `EqualShareAllocation`, `AllocationMode` | When stock is limited, `BeneficiaryService` asks its current strategy how to share it between the open needs of one category. It can be swapped at runtime (or chosen from user input such as `"equal"`), so the same stock can go to different people. `preview(...)` shows the result without handing anything out. |
| **Template Method** | `ImpactReport`, `ProgramImpactReport`, `BeneficiaryProgressReport` | `ImpactReport.generate()` is final and always runs collect data, compute metrics, format rows, summary, footer. A program report shows what one program handed out and how many needs it fully met; a progress report shows one beneficiary's needs, met and open. A report with no data is refused with an `IllegalStateException`. |

Other features: needs counted in units, so they cover both resources (food parcels) and services (counselling sessions); distributions can never exceed what is still needed; services given directly to one person (`giveDirectly`); and records with history cannot be removed. A HIGH-urgency need is published on the `EventBus` as `URGENT_NEED` (extended requirement 5) to the urgent responders set with `setUrgentResponders(Audience)`. `DataStore` passes the volunteers there when it starts up, so this module never imports the Volunteers package. Each distribution is published as `RESOURCE_DISTRIBUTED` to the beneficiary. `BeneficiaryService` is the module's entry point and is what the Facade will call.

## Communication module (Student 5)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `CommunicationChannel`, `EmailChannel`, `SmsChannel`, `SocialMediaChannel`, `ChannelSelector` | Each message is sent through a channel picked per recipient from their preferred contact (falling back to one they have details for). A caller can force one channel for a whole broadcast. |
| **Decorator** | `MessageContent`, `PlainContent`, `ContentDecorator`, `UrgentTagDecorator`, `SignatureDecorator`, `FooterDecorator` | The text is built by stacking an urgent tag, a signature and an unsubscribe footer (e-mail only) around the plain text. |
| **Observer** | `CommunicationService` | Subscribes to every `EventBus` topic and turns donations, assignments, registrations and event changes into messages. A system event with no recipient is sent to every person in its source's `Audience` (`Event` implements it), so Communication never depends on the Events package. |

Other features: SMS cut to 160 characters, a communication log (`getLog()`), and `broadcast(...)` for manual sends.

## Core and main menu (Student 6)

The new classes live in `org.nonprofit.app`, not in `core`. They have to import every module, and every module imports `core`, so putting them in `core` would make the packages depend on each other. Dependencies only point down: `app` → modules → `core`.

| Pattern | Where | What it does |
|---|---|---|
| **Singleton** | `DataStore` | Lazily created, thread-safe single instance that owns the one copy of each module service. `CommunicationService` is created only here, so no message is sent twice. It also passes the volunteers to `BeneficiaryService` as the urgent responders. A small attendee registry lives here too, so the menu can pick people for events. |
| **Facade** | `NonProfitFacade` | The single entry point the main menu calls. Simple methods for every module (record a donation, assign a task, register an attendee with add-ons, distribute stock, broadcast to a group, ...) that delegate to the services from `DataStore`. |
| **MVC** | `MainMenuView` (view), `MainMenuController` (controller), the module services (model) | Main menu with 1 Donations, 2 Volunteers, 3 Events, 4 Beneficiaries, 5 Communication. Volunteers opens the existing `VolunteerController` screen on the shared service. Results start with `>>`, errors with `!!`, and bad input never ends the program. |

`App` loads sample data (`SampleData`) and opens the main menu. `App --demo` runs `SystemDemo`, a fixed walkthrough through the facade that shows every pattern in the system, with the same output on every run.

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
java -cp bin org.nonprofit.communication.CommunicationDemo
java -cp bin org.nonprofit.beneficiary.BeneficiaryDemo
```

In VS Code, open `SDP/src/App.java` and click **Run** for the main menu, or open `SDP/src/org/nonprofit/event/EventDemo.java`, `SDP/src/org/nonprofit/volunteer/VolunteerDemo.java` or `SDP/src/org/nonprofit/donation/DonationDemo.java` and click **Run**. `VolunteerApp` opens the interactive Volunteers screen; the `...Demo` classes run a fixed walkthrough.

## Diagrams

The team uses a **UML generator** to produce the class diagrams from the Java source. Diagrams are not drawn by hand.

- The generator writes one PlantUML file per package into the top-level **`output/`** folder (for example `output/event.puml` and `output/core.puml`).
- Each file starts with a header comment that includes a `URL:` line. Open that link to see the rendered diagram on plantuml.com. You can also view the file with the VS Code *PlantUML* extension.
- **After changing your package, run the generator again** so your diagram matches the code. Don't edit files in `output/` by hand, because the next run overwrites them.
- Each student generates the diagram for their own package. Student 6 also generates `output/overall.puml` covering the whole system.
