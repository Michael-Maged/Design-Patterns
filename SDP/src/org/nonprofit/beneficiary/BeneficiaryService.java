package org.nonprofit.beneficiary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nonprofit.core.Audience;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Entry point of the Beneficiaries module (the Facade in core calls this).
 * Keeps beneficiaries and their needs, shares out limited stock using the
 * current {@link AllocationStrategy}, records every distribution and
 * produces {@link ImpactReport}s.
 */
public class BeneficiaryService {
    private final Map<String, Beneficiary> beneficiaries = new LinkedHashMap<>();
    private final Map<String, Need> needs = new LinkedHashMap<>();
    private final List<Distribution> distributions = new ArrayList<>();
    private final EventBus bus = EventBus.getInstance();
    private AllocationStrategy strategy = new UrgencyFirstAllocation();
    private Audience urgentResponders;

    // ---- Beneficiaries ----

    public Beneficiary registerBeneficiary(Beneficiary beneficiary) {
        beneficiaries.put(beneficiary.getId(), beneficiary);
        return beneficiary;
    }

    /** @return the beneficiary, or null if there is none with that id */
    public Beneficiary findBeneficiary(String beneficiaryId) {
        return beneficiaries.get(beneficiaryId);
    }

    public List<Beneficiary> listBeneficiaries() {
        return new ArrayList<>(beneficiaries.values());
    }

    /** Beneficiaries who have received anything cannot be removed: impact reports depend on them. */
    public boolean removeBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        if (!beneficiary.getDistributions().isEmpty()) {
            throw new IllegalStateException(beneficiary.getName() + " has received resources and cannot be removed");
        }
        for (Need need : beneficiary.getNeeds()) {
            needs.remove(need.getId());
        }
        return beneficiaries.remove(beneficiaryId) != null;
    }

    // ---- Needs ----

    /**
     * Records a need. A HIGH-urgency need is published straight away as
     * URGENT_NEED so the urgent responders (volunteers) hear about it.
     */
    public Need addNeed(String beneficiaryId, String category, int quantity, Urgency urgency, LocalDate requestedOn) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        Need need = new Need(beneficiary, category, quantity, urgency, requestedOn);
        beneficiary.addNeed(need);
        needs.put(need.getId(), need);
        if (urgency == Urgency.HIGH) {
            alertUrgentNeed(need);
        }
        return need;
    }

    /** Only needs nothing has been given against can be removed; anything else is history. */
    public boolean removeNeed(String needId) {
        Need need = requireNeed(needId);
        if (need.getReceived() > 0) {
            throw new IllegalStateException(needId + " has already been partly met and cannot be removed");
        }
        need.getBeneficiary().removeNeed(need);
        return needs.remove(needId) != null;
    }

    /** @return the need, or null if there is none with that id */
    public Need findNeed(String needId) {
        return needs.get(needId);
    }

    public List<Need> openNeeds() {
        List<Need> open = new ArrayList<>();
        for (Need need : needs.values()) {
            if (!need.isMet()) {
                open.add(need);
            }
        }
        return open;
    }

    public List<Need> openNeeds(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        String wanted = category.trim().toLowerCase(Locale.ROOT);
        List<Need> open = new ArrayList<>();
        for (Need need : openNeeds()) {
            if (need.getCategory().equals(wanted)) {
                open.add(need);
            }
        }
        return open;
    }

    /**
     * Who should hear about urgent needs. The Facade passes the volunteers
     * here as an {@link Audience}, so this module never imports the
     * Volunteers package. With nobody set, the alert still goes on the bus.
     */
    public void setUrgentResponders(Audience responders) {
        this.urgentResponders = responders;
    }

    private void alertUrgentNeed(Need need) {
        Beneficiary b = need.getBeneficiary();
        // The message names the area, not the person, because it goes to every volunteer.
        String message = String.format("Urgent: %d x %s needed for a household of %d in %s (%s)",
                need.getQuantity(), need.getCategory(), b.getHouseholdSize(), b.getArea(), need.getId());
        Object source = urgentResponders != null ? urgentResponders : need;
        bus.publish(new SystemEvent(SystemEventType.URGENT_NEED, message, null, source));
    }

    // ---- Strategy ----

    public void setStrategy(AllocationStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy is required");
        }
        this.strategy = strategy;
    }

    public void setStrategy(AllocationMode mode) {
        setStrategy(mode.create());
    }

    public AllocationStrategy getStrategy() {
        return strategy;
    }

    // ---- Distribution ----

    /** How the current strategy would share {@code available} units of a category, without handing anything out. */
    public Map<Need, Integer> preview(String category, int available) {
        if (available <= 0) {
            throw new IllegalArgumentException("Available stock must be at least 1");
        }
        return strategy.allocate(openNeeds(category), available);
    }

    /**
     * Shares out limited stock of one category between everyone who needs
     * it, using the current strategy, and tells each person what they got.
     * Units nobody needs are left over.
     */
    public List<Distribution> distribute(String program, String category, int available, LocalDate date) {
        String programName = requireProgram(program);
        requireDate(date);
        Map<Need, Integer> shares = preview(category, available);
        checkShares(shares, available);
        List<Distribution> made = new ArrayList<>();
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            made.add(record(share.getKey(), share.getValue(), programName, date));
        }
        return made;
    }

    /** Hands units straight to one need, e.g. a service such as counselling sessions. */
    public Distribution giveDirectly(String needId, int units, String program, LocalDate date) {
        return record(requireNeed(needId), units, requireProgram(program), requireDate(date));
    }

    /**
     * Checks a strategy's answer before anything is handed out, so a faulty
     * strategy cannot leave a distribution half done.
     */
    private static void checkShares(Map<Need, Integer> shares, int available) {
        int total = 0;
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            Need need = share.getKey();
            int units = share.getValue();
            if (units <= 0 || units > need.remaining()) {
                throw new IllegalStateException("Strategy gave " + units + " unit(s) to " + need.getId()
                        + ", which needs " + need.remaining());
            }
            total += units;
        }
        if (total > available) {
            throw new IllegalStateException("Strategy handed out " + total + " unit(s) but only "
                    + available + " are available");
        }
    }

    private Distribution record(Need need, int units, String program, LocalDate date) {
        need.receive(units);
        Distribution distribution = new Distribution(need, units, program, date);
        need.getBeneficiary().addDistribution(distribution);
        distributions.add(distribution);
        String status = need.isMet() ? "this need is now fully met"
                : need.remaining() + " more to come";
        bus.publish(new SystemEvent(SystemEventType.RESOURCE_DISTRIBUTED,
                String.format("You received %d x %s from %s (%s)", units, need.getCategory(), program, status),
                need.getBeneficiary(), distribution));
        return distribution;
    }

    public List<Distribution> listDistributions() {
        return new ArrayList<>(distributions);
    }

    /** Every program name used so far, in the order first seen. */
    public List<String> listPrograms() {
        List<String> programs = new ArrayList<>();
        for (Distribution d : distributions) {
            boolean seen = false;
            for (String p : programs) {
                if (p.equalsIgnoreCase(d.getProgram())) {
                    seen = true;
                }
            }
            if (!seen) {
                programs.add(d.getProgram());
            }
        }
        return programs;
    }

    // ---- Impact reports (Template Method) ----

    public String programImpactReport(String program, LocalDate date) {
        return new ProgramImpactReport(requireProgram(program), listDistributions(), date).generate();
    }

    public String progressReport(String beneficiaryId, LocalDate date) {
        return new BeneficiaryProgressReport(requireBeneficiary(beneficiaryId), date).generate();
    }

    private Beneficiary requireBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary = beneficiaries.get(beneficiaryId);
        if (beneficiary == null) {
            throw new IllegalArgumentException("No beneficiary with id " + beneficiaryId);
        }
        return beneficiary;
    }

    private Need requireNeed(String needId) {
        Need need = needs.get(needId);
        if (need == null) {
            throw new IllegalArgumentException("No need with id " + needId);
        }
        return need;
    }

    private static LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Distribution date is required");
        }
        return date;
    }

    private static String requireProgram(String program) {
        if (program == null || program.isBlank()) {
            throw new IllegalArgumentException("Program name is required");
        }
        return program.trim();
    }
}
