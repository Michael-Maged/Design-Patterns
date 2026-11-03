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
import org.nonprofit.beneficiary.model.Urgency;

/**
 * TEMPLATE METHOD pattern (concrete class): one beneficiary's needs, how
 * much of each has been received, and which are still open.
 */
public class BeneficiaryProgressReport extends ImpactReport {
    private final Beneficiary beneficiary;

    private List<Need> needs = new ArrayList<>();

    public BeneficiaryProgressReport(Beneficiary beneficiary, LocalDate reportDate) {
        super(reportDate);
        this.beneficiary = beneficiary;
    }

    @Override
    protected String title() {
        return "Beneficiary Progress Report";
    }

    @Override
    protected String scope() {
        return beneficiary.getName() + " (" + beneficiary.getId() + "), " + beneficiary.getArea();
    }

    @Override
    protected void collectData() {
        needs = beneficiary.getNeeds();
    }

    @Override
    protected boolean hasData() {
        return !needs.isEmpty();
    }

    @Override
    protected Map<String, String> computeMetrics() {
        int met = 0;
        Set<String> programs = new LinkedHashSet<>();
        for (Need need : needs) {
            if (need.isMet()) {
                met++;
            }
        }
        for (Distribution d : beneficiary.getDistributions()) {
            programs.add(d.getProgram());
        }
        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Needs recorded", String.valueOf(needs.size()));
        metrics.put("Needs met", String.valueOf(met));
        metrics.put("Needs still open", String.valueOf(needs.size() - met));
        metrics.put("Units received", String.valueOf(beneficiary.unitsReceived()));
        metrics.put("Helped by", programs.isEmpty() ? "-" : String.join(", ", programs));
        return metrics;
    }

    @Override
    protected List<String> formatRows() {
        List<String> rows = new ArrayList<>();
        for (Need need : needs) {
            rows.add(String.format("%-7s %-20s %-6s %3d/%-3d %4.0f%%  %s",
                    need.getId(), need.getCategory(), need.getUrgency(),
                    need.getReceived(), need.getQuantity(), need.percentMet(),
                    need.isMet() ? "MET" : "OPEN"));
        }
        return rows;
    }

    @Override
    protected String summary() {
        int urgentOpen = 0;
        for (Need need : needs) {
            if (!need.isMet() && need.getUrgency() == Urgency.HIGH) {
                urgentOpen++;
            }
        }
        String text = (needs.size() - beneficiary.openNeeds().size()) + " of " + needs.size() + " need(s) met.";
        return urgentOpen == 0 ? text : text + " " + urgentOpen + " urgent need(s) still open.";
    }
}
