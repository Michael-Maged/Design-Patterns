package org.nonprofit.beneficiary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * STRATEGY pattern (concrete strategy): the most urgent needs are filled
 * first; needs of equal urgency are served in the order they were requested.
 */
public class UrgencyFirstAllocation implements AllocationStrategy {

    @Override
    public Map<Need, Integer> allocate(List<Need> needs, int available) {
        List<Need> ordered = new ArrayList<>(needs);
        // The sort is stable, so needs that tie keep the order they were recorded in.
        ordered.sort((a, b) -> {
            int byUrgency = b.getUrgency().compareTo(a.getUrgency());
            return byUrgency != 0 ? byUrgency : a.getRequestedOn().compareTo(b.getRequestedOn());
        });
        return AllocationStrategy.fillInOrder(ordered, available);
    }

    @Override
    public String name() {
        return "Urgency first";
    }
}
