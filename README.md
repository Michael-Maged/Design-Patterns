# Design-Patterns: Non-Profit Management System

A Java console system for a non-profit organization. It covers donations, volunteers, events, beneficiaries and communication, and is built to demonstrate the design patterns from the course: **Factory, Observer, Strategy, Template Method, Facade, Decorator, Singleton**.

The full design is in [`docs/superpowers/specs/2026-10-24-nonprofit-system-design.md`](docs/superpowers/specs/2026-10-24-nonprofit-system-design.md).

## Team and ownership

| Student | Name | Package | Responsibility | Patterns | Diagram | Status |
|---|---|---|---|---|---|---|
| 1 | _TBD_ | `org.nonprofit.donation` | Donors, online/check/in-kind donations, campaigns, receipts and tax documents | Factory Method, Template Method, Observer, MVC | [`output/donation.puml`](output/donation.puml) | **Done** |
| 2 | _TBD_ | `org.nonprofit.volunteer` | Volunteers, skills, availability, assignment, hours, certificates | Strategy, Template Method | [`output/volunteer.puml`](output/volunteer.puml) | **Done** |
| 3 | _TBD_ | `org.nonprofit.event` | Events, registration, ticketing, waitlist, attendance, reminders | Factory, Decorator, Observer | [`output/event.puml`](output/event.puml) | **Done** |
| 4 | _TBD_ | `org.nonprofit.beneficiary` | Beneficiaries, needs, resource distribution, impact reports | Strategy, Template Method | `output/beneficiary.puml` (to generate) | **Done** |
| 5 | _TBD_ | `org.nonprofit.communication` | Email / SMS / social media channels, message composition | Strategy, Decorator, Observer | [`output/communication.puml`](output/communication.puml) | **Done** |
| 6 | _TBD_ | `org.nonprofit.core` + `App` | Data store, event bus, system facade, console menu, overall diagram, requirements doc | Singleton, Observer, Facade | [`output/core.puml`](output/core.puml), `output/overall.puml` | Foundation in place* |

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
| **Factory Method** | `DonationCreator` (creator), `OnlineDonationCreator`, `CheckDonationCreator`, `InKindDonationCreator` (concrete creators), `Donation` and its subclasses `OnlineDonation`, `CheckDonation`, `InKindDonation` (products) | `DonationCreator.receive()` is final and is the same for every donation: call the factory method `createDonation()`, then add the donation to the donor's history. Each concrete creator decides which donation class to build. `DonationService` only knows the abstract creator and the abstract `Donation`. A new donation method means one new product class plus one new creator, plugged in with `addCreator()`. |
| **Template Method** | `DonationDocument`, `DonationReceipt`, `TaxStatement` | `DonationDocument.generate()` is final and always prints header, body, totals, footer. A receipt covers one donation. A tax statement lists a donor's donations for one year, with money and goods totalled separately. The `canBeIssued()` hook stops an empty statement from being printed. |
| **Observer** | `DonationService` (publisher), `DonationView` and `CommunicationService` (observers) | Every new donation is published on the `EventBus` as `DONATION_RECEIVED`. The Communication module sends the thank-you message and the screen shows a notification. `DonationService` never calls either of them directly. |
| **MVC** | `DonationView`, `DonationController`, `DonationService` (model), `DonationApp` | An interactive console screen. The view only prints and reads input, and the controller turns menu choices into calls on the service. |

Client requirements collected for this module: **monthly online gifts** (shown on the receipt) and **campaigns with a money goal and progress percentage**. Other features: totals by method, top donors, input checks (missing check number, negative amount, unknown campaign...), donation numbers with no gaps when a bad donation is rejected, and donors who have already given can't be deleted. `DonationService` is the module's entry point and is what the Facade will call.

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

Other features: needs counted in units, so they cover both resources (food parcels) and services (counselling sessions); distributions can never exceed what is still needed; services given directly to one person (`giveDirectly`); and records with history cannot be removed. A HIGH-urgency need is published on the `EventBus` as `URGENT_NEED` (extended requirement 5) to the urgent responders set with `setUrgentResponders(Audience)`. The Facade passes the volunteers there, so this module never imports the Volunteers package. Each distribution is published as `RESOURCE_DISTRIBUTED` to the beneficiary. `BeneficiaryService` is the module's entry point and is what the Facade will call.

## Communication module (Student 5)

| Pattern | Where | What it does |
|---|---|---|
| **Strategy** | `CommunicationChannel`, `EmailChannel`, `SmsChannel`, `SocialMediaChannel`, `ChannelSelector` | Each message is sent through a channel picked per recipient from their preferred contact (falling back to one they have details for). A caller can force one channel for a whole broadcast. |
| **Decorator** | `MessageContent`, `PlainContent`, `ContentDecorator`, `UrgentTagDecorator`, `SignatureDecorator`, `FooterDecorator` | The text is built by stacking an urgent tag, a signature and an unsubscribe footer (e-mail only) around the plain text. |
| **Observer** | `CommunicationService` | Subscribes to every `EventBus` topic and turns donations, assignments, registrations and event changes into messages. A system event with no recipient is sent to every person in its source's `Audience` (`Event` implements it), so Communication never depends on the Events package. |

Other features: SMS cut to 160 characters, a communication log (`getLog()`), and `broadcast(...)` for manual sends.

## Running

From the `SDP` folder:

```sh
javac --release 17 -d bin $(find src -name "*.java")
java -cp bin org.nonprofit.event.EventDemo
java -cp bin org.nonprofit.volunteer.VolunteerDemo
java -cp bin org.nonprofit.volunteer.VolunteerApp
java -cp bin org.nonprofit.donation.DonationDemo
java -cp bin org.nonprofit.donation.DonationApp
java -cp bin org.nonprofit.communication.CommunicationDemo
java -cp bin org.nonprofit.beneficiary.BeneficiaryDemo
```

In VS Code, open `SDP/src/org/nonprofit/event/EventDemo.java`, `SDP/src/org/nonprofit/volunteer/VolunteerDemo.java` or `SDP/src/org/nonprofit/donation/DonationDemo.java` and click **Run**. `VolunteerApp` and `DonationApp` open the interactive Volunteers and Donations screens; the `...Demo` classes run a fixed walkthrough.

## Diagrams

The team uses a **UML generator** to produce the class diagrams from the Java source. Diagrams are not drawn by hand.

- The generator writes one PlantUML file per package into the top-level **`output/`** folder (for example `output/event.puml` and `output/core.puml`).
- Each file starts with a header comment that includes a `URL:` line. Open that link to see the rendered diagram on plantuml.com. You can also view the file with the VS Code *PlantUML* extension.
- **After changing your package, run the generator again** so your diagram matches the code. Don't edit files in `output/` by hand, because the next run overwrites them.
- Each student generates the diagram for their own package. Student 6 also generates `output/overall.puml` covering the whole system.
