package org.nonprofit.beneficiary.templatemethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nonprofit.beneficiary.model.Beneficiary;
import org.nonprofit.beneficiary.model.Distribution;
import org.nonprofit.beneficiary.model.Need;

/**
 * TEMPLATE METHOD pattern (concrete class): what one program handed out,
 * how many people it reached and how many of the needs it served are now
 * fully met.
 */
public class ProgramImpactReport extends ImpactReport {
    private final String program;
    private final List<Distribution> allDistributions;

    private final List<Distribution> distributions = new ArrayList<>();
    private final Set<Beneficiary> reached = new LinkedHashSet<>();
    private final Set<Need> needsServed = new LinkedHashSet<>();

    public ProgramImpactReport(String program, List<Distribution> allDistributions, LocalDate reportDate) {
        super(reportDate);
        this.program = program;
        this.allDistributions = allDistributions;
    }

    @Override
    protected String title() {
        return "Program Impact Report";
    }

    @Override
    protected String scope() {
        return "Program: " + program;
    }

    @Override
    protected void collectData() {
        // Start empty so generating the same report twice does not count anything twice.
        distributions.clear();
        reached.clear();
        needsServed.clear();
        for (Distribution d : allDistributions) {
            if (d.getProgram().equalsIgnoreCase(program)) {
                distributions.add(d);
                reached.add(d.getBeneficiary());
                needsServed.add(d.getNeed());
            }
        }
    }

    @Override
    protected boolean hasData() {
        return !distributions.isEmpty();
    }

    @Override
    protected Map<String, String> computeMetrics() {
        int units = 0;
        for (Distribution d : distributions) {
            units += d.getQuantity();
        }
        int met = 0;
        double fulfilment = 0;
        for (Need need : needsServed) {
            if (need.isMet()) {
                met++;
            }
            fulfilment += need.percentMet();
        }
        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Beneficiaries reached", String.valueOf(reached.size()));
        metrics.put("Distributions", String.valueOf(distributions.size()));
        metrics.put("Units handed out", String.valueOf(units));
        metrics.put("Needs served, now fully met", met + " of " + needsServed.size());
        metrics.put("Average fulfilment", String.format("%.0f%%", fulfilment / needsServed.size()));
        return metrics;
    }

    /** One row per category: units given and how many people got them. */
    @Override
    protected List<String> formatRows() {
        Map<String, Integer> units = new LinkedHashMap<>();
        Map<String, Set<Beneficiary>> people = new LinkedHashMap<>();
        for (Distribution d : distributions) {
            units.merge(d.getCategory(), d.getQuantity(), Integer::sum);
            people.computeIfAbsent(d.getCategory(), c -> new LinkedHashSet<>()).add(d.getBeneficiary());
        }
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : units.entrySet()) {
            rows.add(String.format("%-20s %4d unit(s) to %d beneficiary(ies)",
                    entry.getKey(), entry.getValue(), people.get(entry.getKey()).size()));
        }
        return rows;
    }

    @Override
    protected String summary() {
        return String.format("%s reached %d beneficiary(ies) across %d household member(s).",
                program, reached.size(), householdMembers());
    }

    private int householdMembers() {
        int total = 0;
        for (Beneficiary b : reached) {
            total += b.getHouseholdSize();
        }
        return total;
    }
}
