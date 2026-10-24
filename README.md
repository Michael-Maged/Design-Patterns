# Design-Patterns: Non-Profit Management System

A Java console system for a non-profit organization. It covers donations, volunteers, events, beneficiaries and communication, and is built to demonstrate the design patterns from the course: **Factory, Observer, Strategy, Template Method, Facade, Decorator, Singleton**.

The full design is in [`docs/superpowers/specs/2026-10-24-nonprofit-system-design.md`](docs/superpowers/specs/2026-10-24-nonprofit-system-design.md).

## Team and ownership

| Student | Name | Package | Responsibility | Patterns | Diagram | Status |
|---|---|---|---|---|---|---|
| 1 | _TBD_ | `org.nonprofit.donation` | Donors, online/check/in-kind donations, receipts and tax documents | Factory, Template Method | `output/donation.puml` | Not started |
| 2 | _TBD_ | `org.nonprofit.volunteer` | Volunteers, skills, availability, assignment, hours, certificates | Strategy, Template Method | `output/volunteer.puml` | Not started |
| 3 | _TBD_ | `org.nonprofit.event` | Events, registration, ticketing, waitlist, attendance, reminders | Factory, Decorator, Observer | [`output/event.puml`](output/event.puml) | **Done** |
| 4 | _TBD_ | `org.nonprofit.beneficiary` | Beneficiaries, needs, resource distribution, impact reports | Strategy, Template Method | `output/beneficiary.puml` | Not started |
| 5 | _TBD_ | `org.nonprofit.communication` | Email / SMS / social media channels, message composition | Strategy, Decorator | `output/communication.puml` | Not started |
| 6 | _TBD_ | `org.nonprofit.core` + `App` | Data store, event bus, system facade, console menu, overall diagram, requirements doc | Singleton, Observer, Facade | [`output/core.puml`](output/core.puml), `output/overall.puml` | Foundation in place* |

\* Student 3 added the minimal shared core that Events needs: `Person`, `ContactPreference`, `EventBus` (Singleton + Observer), `SystemEvent`, `SystemEventType` and `SystemObserver`. Student 6 owns these files and builds on them. Other modules publish on `EventBus` and should add their own topics to `SystemEventType`.

## Events module (Student 3)

| Pattern | Where | What it does |
|---|---|---|
| **Factory** | `EventFactory`, `EventType` | Creates a `Fundraiser`, `Workshop` or `OutreachProgram` from an enum or from user input (`"workshop"`, `"outreach program"`). |
| **Decorator** | `Ticket`, `BasicTicket`, `TicketDecorator`, `VipTicket`, `MealTicket`, `ParkingTicket`, `TicketAddOn` | Ticket add-ons stack on a basic ticket, and each one adds to the price and the description. |
| **Observer** | `Event` (subject), `EventObserver`, `Attendee`, `EventBusNotifier` | Registered attendees are notified when an event is rescheduled, relocated or cancelled, or when a reminder goes out. `EventBusNotifier` forwards these notifications to the system `EventBus` for the Communication module. |

Other features: capacity limits, a **waitlist with automatic promotion** when someone cancels, check-in and attendance reports, ticket revenue, fundraiser pledge progress, and reminders for events starting within a time window. `EventService` is the module's entry point and is what the Facade will call.

## Running

From the `SDP` folder:

```sh
javac --release 17 -d bin $(find src -name "*.java")
java -cp bin org.nonprofit.event.EventDemo
```

In VS Code, open `SDP/src/org/nonprofit/event/EventDemo.java` and click **Run**.

## Diagrams

The team uses a **UML generator** to produce the class diagrams from the Java source. Diagrams are not drawn by hand.

- The generator writes one PlantUML file per package into the top-level **`output/`** folder (for example `output/event.puml` and `output/core.puml`).
- Each file starts with a header comment that includes a `URL:` line. Open that link to see the rendered diagram on plantuml.com. You can also view the file with the VS Code *PlantUML* extension.
- **After changing your package, run the generator again** so your diagram matches the code. Don't edit files in `output/` by hand, because the next run overwrites them.
- Each student generates the diagram for their own package. Student 6 also generates `output/overall.puml` covering the whole system.
