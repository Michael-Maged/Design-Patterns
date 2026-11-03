package org.nonprofit.beneficiary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * STRATEGY pattern (concrete strategy): needs are filled in the order they
 * were requested, whatever their urgency.
 */
public class FirstComeFirstServedAllocation implements AllocationStrategy {

    @Override
    public Map<Need, Integer> allocate(List<Need> needs, int available) {
        List<Need> ordered = new ArrayList<>(needs);
        ordered.sort((a, b) -> a.getRequestedOn().compareTo(b.getRequestedOn()));
        return AllocationStrategy.fillInOrder(ordered, available);
    }

    @Override
    public String name() {
        return "First come, first served";
    }
}
