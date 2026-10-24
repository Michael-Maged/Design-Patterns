# Non-Profit Management System — Design

## Intent

A university Software Design Patterns course project (Part 1, "Major Assessment Task") for a team of **six students**. The system manages a non-profit's donations, volunteers, events, beneficiaries and communications. The real point is to **demonstrate design patterns cleanly** and to divide the work fairly among six students so each one can present their own part.

## Goals

- Cover the five modules in the brief: Donations, Volunteers, Events, Beneficiaries, Communication.
- Use every pattern taught in lecture: **Factory, Observer, Strategy, Template Method, Facade, Decorator, Singleton**. Each pattern must be applied where it naturally fits, not bolted on.
- Each student owns one package and at least two patterns.
- Deliverables:
  - Java implementation code, well structured and commented.
  - A class diagram per package plus one overall diagram.
  - An "extended client requirements" document.
- Run it as a console menu demo that exercises every module and pattern.

## Non-goals

- No real database, real email/SMS gateways or real social media APIs. Persistence is in memory and channels are simulated with console output.
- No GUI, no web layer, no frameworks, no build tool beyond what VS Code's Java extension already uses.
- No authentication or user accounts.

## Anti-goals (failure even if it works)

- Patterns forced in where they don't belong, or one pattern used in a way a grader would call wrong (e.g. a "Factory" that is just a constructor call).
- An uneven split where one student has most of the code or the patterns.
- Code a student can't explain. Keep each class small and readable.

## Constraints (decided by the user)

- Language: **Java**.
- Team of **6**; the code is split among the students.
- Ownership is recorded **only in the top-level `README.md`** (no `@author` tags, no per-student branches).
- Patterns covered in lecture: the five in the brief plus Decorator, Singleton (and Strategy).

## Builder's calls (Claude's, not confirmed by the user)

- Plain Java 17, console app, in-memory storage. Code lives in the existing VS Code project at `SDP/src` (source path `src`, output `bin`). Entry point is the existing `SDP/src/App.java`, rewritten.
- Class diagrams are PlantUML (`.puml`) text files under `docs/diagrams/`.
- Extended requirements go in `docs/requirements.md`.
- No unit-test framework (the project has no `lib` jars). Verification is a scripted `--demo` mode of `App` that runs every scenario non-interactively, plus a successful `javac` compile.
- Students are labelled "Student 1" … "Student 6" in the README. The team fills in their real names.

## Package layout and ownership

Root package: `org.nonprofit` under `SDP/src/`.

| Student | Package | Responsibility | Patterns |
|---|---|---|---|
| 1 | `org.nonprofit.donation` | Donors, donations (amount, type, date), online / check / in-kind methods, receipts and tax documents | Factory, Template Method |
| 2 | `org.nonprofit.volunteer` | Volunteers (skills, availability, preferences), tasks, assignment, hour logging, certificates | Strategy, Template Method |
| 3 | `org.nonprofit.event` | Events (fundraiser, workshop, outreach), registration, ticketing, attendance, reminders | Factory, Decorator, Observer |
| 4 | `org.nonprofit.beneficiary` | Beneficiaries and needs, resource/service distribution, impact evaluation | Strategy, Template Method |
| 5 | `org.nonprofit.communication` | Email, SMS, social-media channels, message composition, per-recipient channel choice | Strategy, Decorator |
| 6 | `org.nonprofit.core` + `App` | Shared data store, notification hub, system facade, console menu, overall diagram, requirements doc | Singleton, Observer, Facade |

Each student also writes their package's `.puml` diagram. Student 6 writes `overall.puml`.

## Pattern designs per module

### Core (Student 6)
- **Singleton**: `DataStore`, a single in-memory registry holding lists of donors, donations, volunteers, events and beneficiaries. `EventBus` is the system-wide notification hub. Both use a lazily initialised, thread-safe `getInstance()`.
- **Observer**: `EventBus` holds `SystemObserver` subscribers keyed by `SystemEventType` (`DONATION_RECEIVED`, `EVENT_UPDATED`, `VOLUNTEER_ASSIGNED`, `RESOURCE_DISTRIBUTED`, …). Modules publish; other modules (chiefly Communication) subscribe. This keeps modules decoupled.
- **Facade**: `NonProfitFacade` is the single entry point the console menu calls (`recordDonation(...)`, `registerVolunteer(...)`, `createEvent(...)`, `registerAttendee(...)`, `distributeResource(...)`, `broadcast(...)`, etc.). It hides the subsystems.
- `App` runs an interactive console menu over the facade. `App --demo` runs a scripted walkthrough of every feature.
- Shared base type: `Person` (id, name, email, phone, preferred contact). `Donor`, `Volunteer`, `Attendee` and `Beneficiary` extend it.

### Donations (Student 1)
- `Donation` is abstract, with subclasses `OnlineDonation` (payment reference), `CheckDonation` (check number, bank) and `InKindDonation` (item description, estimated value).
- **Factory**: `DonationFactory.create(DonationType, details)` returns the right subclass. Adding a method means adding one subclass and one factory case.
- **Template Method**: `DonationDocument` is abstract, with a final `generate()` = header → body → totals → footer. Subclasses are `DonationReceipt` and `TaxDeductionStatement` (annual summary per donor). Only the body differs.
- `DonationService` records a donation in `DataStore` and publishes `DONATION_RECEIVED` on `EventBus`.

### Volunteers (Student 2)
- `Volunteer` has skills, availability (days), preferred task categories and logged hours. `Task` has required skill, day and category.
- **Strategy**: `AssignmentStrategy` interface `pick(Task, List<Volunteer>)`. Implementations: `SkillBasedAssignment`, `AvailabilityBasedAssignment`, `PreferenceBasedAssignment`. Swappable at runtime from the menu.
- **Template Method**: `Certificate` is abstract, with a final `issue()` = title → recipient → achievement → signature. Subclasses: `HoursCertificate` (total hours), `EventParticipationCertificate`.
- `VolunteerService` assigns, logs hours, and publishes `VOLUNTEER_ASSIGNED`.

### Events (Student 3)
- `Event` is abstract (name, date, location, capacity, attendees), with subclasses `Fundraiser`, `Workshop`, `OutreachProgram`.
- **Factory**: `EventFactory.create(EventType, ...)`.
- **Decorator**: `Ticket` interface (`price()`, `description()`). `BasicTicket` is the concrete component. `TicketDecorator` is abstract, with `VipTicket`, `MealTicket`, `ParkingTicket`. Add-ons stack.
- **Observer**: `Event` keeps its registered attendees as observers. Changing date/location calls `notifyAttendees()`. Updates and reminders are also published on `EventBus` as `EVENT_UPDATED` / `EVENT_REMINDER` so Communication sends them.
- `EventService` covers registration (capacity check, issues ticket), check-in/attendance, and `sendReminders(event)`.

### Beneficiaries (Student 4)
- `Beneficiary` has a list of `Need`s (category, quantity, urgency). `Distribution` records what was given, how much and when.
- **Strategy**: `AllocationStrategy` interface decides order when resources are limited. Implementations: `UrgencyFirstAllocation`, `FirstComeFirstServedAllocation`, `EqualShareAllocation`.
- **Template Method**: `ImpactReport` is abstract, with a final `generate()` = collect data → compute metrics → format → summary. Subclasses: `ProgramImpactReport` (per program/category), `BeneficiaryProgressReport` (per person: needs met vs open).
- `BeneficiaryService` distributes and publishes `RESOURCE_DISTRIBUTED`.

### Communication (Student 5)
- `Message` (subject, body, recipient).
- **Strategy**: `CommunicationChannel` interface `send(Message)`. Implementations: `EmailChannel`, `SmsChannel`, `SocialMediaChannel` (posts to simulated Facebook/X/Instagram). A `ChannelSelector` picks a strategy from the recipient's preferred contact (donors default to email, volunteers to SMS, public outreach to social media).
- **Decorator**: `MessageContent` interface `render()`. `PlainContent` is the concrete component, with decorators `SignatureDecorator`, `UrgentTagDecorator` and `FooterDecorator` (unsubscribe/legal footer). Logging is done by `LoggingChannel`, which wraps a channel.
- `CommunicationService` implements `SystemObserver`. It subscribes to the `EventBus` events and turns them into messages (thank-you for donations, event updates/reminders, assignment notices). It also exposes `broadcast(...)` for manual sends and keeps a sent-message log.

## Repository output

```
README.md                      ← project overview, how to run, team/ownership table (Student → package → patterns → diagram)
docs/requirements.md           ← brief requirements + extended client requirements (Student 6)
docs/diagrams/*.puml           ← donation, volunteer, event, beneficiary, communication, core, overall
SDP/src/App.java
SDP/src/org/nonprofit/{core,donation,volunteer,event,beneficiary,communication}/...
```

`SDP/README.md` (the VS Code boilerplate) is left alone.

## Extended client requirements (content for docs/requirements.md)

The brief asks the team to collect extra requirements from users. These are Claude's proposed additions, presented as if gathered from staff, to be adjusted by the team, and implemented:
1. Recurring monthly donations (a flag on online donations, shown on receipts).
2. Donation campaign goals with a progress percentage.
3. Event waitlist when capacity is full; auto-promote on cancellation and notify.
4. Volunteer leaderboard by hours.
5. Urgent-need alerts: a beneficiary need with high urgency triggers a broadcast to volunteers.
6. Communication log viewable from the menu.

## Verification

- `javac` the whole `SDP/src` tree into `SDP/bin` with zero errors.
- `java -cp SDP/bin App --demo` runs every module scenario and prints visible evidence of each pattern (factory-created types, strategy swap changing the result, decorated tickets/messages, observer notifications, template documents, the same singleton instance).

## Left to the builder

Exact field names, menu wording, sample seed data, and ASCII formatting of documents/reports.
