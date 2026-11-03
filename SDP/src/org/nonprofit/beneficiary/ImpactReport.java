package org.nonprofit.beneficiary;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TEMPLATE METHOD pattern (abstract class): {@link #generate()} fixes the
 * steps of every impact report - collect data, compute metrics, format the
 * rows, write the summary - and subclasses fill in what each step does.
 */
public abstract class ImpactReport {
    static final String ORGANIZATION = "Community Hands Foundation";
    private static final String RULE = "=".repeat(64);
    private static final String THIN_RULE = "-".repeat(64);

    protected final LocalDate reportDate;

    protected ImpactReport(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    /** The template method. Final so subclasses cannot change the order of the steps. */
    public final String generate() {
        collectData();
        if (!hasData()) {
            throw new IllegalStateException("Nothing to report for " + scope());
        }
        Map<String, String> metrics = computeMetrics();
        StringBuilder sb = new StringBuilder();
        sb.append(RULE).append('\n');
        sb.append(ORGANIZATION).append('\n');
        sb.append(title().toUpperCase(Locale.ROOT)).append('\n');
        sb.append(scope()).append(" | as of ").append(reportDate).append('\n');
        sb.append(THIN_RULE).append('\n');
        for (String row : formatRows()) {
            sb.append("  ").append(row).append('\n');
        }
        sb.append(THIN_RULE).append('\n');
        for (Map.Entry<String, String> metric : metrics.entrySet()) {
            sb.append(String.format("  %-30s %s%n", metric.getKey() + ":", metric.getValue()));
        }
        sb.append(THIN_RULE).append('\n');
        sb.append(summary()).append('\n');
        sb.append(footer()).append('\n');
        sb.append(RULE);
        return sb.toString();
    }

    // ---- Steps that every subclass must provide ----

    protected abstract String title();

    /** What the report is about, e.g. a program name or a beneficiary. */
    protected abstract String scope();

    protected abstract void collectData();

    protected abstract boolean hasData();

    /** Metric name to value, in the order they are printed. */
    protected abstract Map<String, String> computeMetrics();

    protected abstract List<String> formatRows();

    protected abstract String summary();

    // ---- Hook with a default that subclasses may override ----

    protected String footer() {
        return "Prepared by the Programs Office.";
    }
}
