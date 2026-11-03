package org.nonprofit.beneficiary;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.nonprofit.core.Audience;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.Person;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;
import org.nonprofit.core.SystemObserver;
import org.nonprofit.beneficiary.strategy.AllocationMode;
import org.nonprofit.beneficiary.strategy.AllocationStrategy;
import org.nonprofit.beneficiary.model.Beneficiary;
import org.nonprofit.beneficiary.model.BeneficiaryService;
import org.nonprofit.beneficiary.model.Distribution;
import org.nonprofit.beneficiary.strategy.EqualShareAllocation;
import org.nonprofit.beneficiary.strategy.FirstComeFirstServedAllocation;
import org.nonprofit.beneficiary.model.Need;
import org.nonprofit.beneficiary.model.Urgency;
import org.nonprofit.beneficiary.strategy.UrgencyFirstAllocation;

/**
 * Stand-alone walkthrough of the Beneficiaries module, showing each pattern.
 * Run: java -cp bin org.nonprofit.beneficiary.BeneficiaryDemo
 */
public class BeneficiaryDemo {

    public static void main(String[] args) {
        // A simple subscriber stands in for the Communication module and
        // prints what it would send, including who a group message reaches.
        SystemObserver printer = new SystemObserver() {
            @Override
            public void onEvent(SystemEvent event) {
                System.out.println("    [EventBus] " + event);
                if (event.getSource() instanceof Audience) {
                    for (Person p : ((Audience) event.getSource()).getAudience()) {
                        System.out.println("        -> " + p.getName());
                    }
                }
            }
        };
        EventBus.getInstance().subscribe(SystemEventType.RESOURCE_DISTRIBUTED, printer);
        EventBus.getInstance().subscribe(SystemEventType.URGENT_NEED, printer);

        BeneficiaryService service = new BeneficiaryService();
        LocalDate today = LocalDate.of(2026, 11, 1);

        // In the full system DataStore passes VolunteerService's volunteers here.
        List<Person> onCall = List.of(new Responder("Sara Mostafa"), new Responder("Karim Fathy"));
        service.setUrgentResponders(() -> onCall);

        heading("1. Beneficiaries and their needs (a HIGH need alerts volunteers)");
        Beneficiary amal = service.registerBeneficiary(
                new Beneficiary("Amal Saeed", "+20120000001", "Ard El Lewa", 5));
        Beneficiary hassan = service.registerBeneficiary(
                new Beneficiary("Hassan Ali", "hassan@example.org", "+20120000002", ContactPreference.EMAIL,
                        "Imbaba", 2));
        Beneficiary rania = service.registerBeneficiary(
                new Beneficiary("Rania Youssef", "+20120000003", "Ard El Lewa", 4));
        service.addNeed(amal.getId(), "Food Parcel", 4, Urgency.MEDIUM, today.minusDays(9));
        service.addNeed(hassan.getId(), "food parcel", 3, Urgency.LOW, today.minusDays(12));
        service.addNeed(rania.getId(), "food parcel", 5, Urgency.fromString("high"), today.minusDays(2));
        Need counselling = service.addNeed(amal.getId(), "Counselling session", 6, Urgency.LOW, today.minusDays(5));
        for (Beneficiary b : service.listBeneficiaries()) {
            System.out.println("  " + b.summary());
        }

        heading("2. STRATEGY - the same 6 food parcels, three strategies, three answers");
        AllocationStrategy[] strategies = {
                new UrgencyFirstAllocation(), new FirstComeFirstServedAllocation(), new EqualShareAllocation() };
        for (AllocationStrategy strategy : strategies) {
            service.setStrategy(strategy);
            System.out.printf("  %-26s -> %s%n", strategy.name(), describe(service.preview("food parcel", 6)));
        }

        heading("3. Strategy chosen at runtime from user input (\"equal\"), then distributed");
        service.setStrategy(AllocationMode.fromString("equal"));
        service.distribute("Winter Food Program", "food parcel", 6, today);
        System.out.println("  Second delivery of 5, urgency first:");
        service.setStrategy(AllocationMode.URGENCY);
        service.distribute("Winter Food Program", "food parcel", 5, today.plusDays(7));
        System.out.println("  Still open: " + service.openNeeds("food parcel"));

        heading("4. A service given to one person directly");
        service.giveDirectly(counselling.getId(), 2, "Family Support", today.plusDays(1));
        try {
            service.giveDirectly(counselling.getId(), 10, "Family Support", today.plusDays(2));
        } catch (IllegalArgumentException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
        for (Distribution d : service.listDistributions()) {
            System.out.println("  " + d.summary());
        }

        heading("5. TEMPLATE METHOD - two impact reports, one layout");
        System.out.println(service.programImpactReport("Winter Food Program", today.plusDays(7)));
        System.out.println(service.progressReport(amal.getId(), today.plusDays(7)));
        try {
            service.programImpactReport("Summer Camp", today.plusDays(7));
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("6. Removing records keeps history safe");
        Beneficiary newcomer = service.registerBeneficiary(new Beneficiary("Mahmoud Nabil", "+20120000004", "Giza", 1));
        System.out.println("  Removed newcomer with no history? " + service.removeBeneficiary(newcomer.getId()));
        try {
            service.removeBeneficiary(amal.getId());
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    private static String describe(Map<Need, Integer> shares) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(share.getKey().getBeneficiary().getName()).append(' ').append(share.getValue());
        }
        return sb.toString();
    }

    /** Stands in for a volunteer so the demo does not depend on the Volunteers package. */
    private static class Responder extends Person {
        Responder(String name) {
            super("VOL-demo", name, null, "+20110000000", ContactPreference.SMS);
        }
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
