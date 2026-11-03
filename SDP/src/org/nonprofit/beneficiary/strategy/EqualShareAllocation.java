package org.nonprofit.beneficiary.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.nonprofit.beneficiary.model.Need;

/**
 * STRATEGY pattern (concrete strategy): the stock is shared out one unit at
 * a time in turn, so everyone gets something before anyone gets a second
 * unit. Nobody gets more than they still need.
 */
public class EqualShareAllocation implements AllocationStrategy {

    @Override
    public Map<Need, Integer> allocate(List<Need> needs, int available) {
        Map<Need, Integer> shares = new LinkedHashMap<>();
        int left = available;
        boolean gaveAny = true;
        while (left > 0 && gaveAny) {
            gaveAny = false;
            for (Need need : needs) {
                int given = shares.getOrDefault(need, 0);
                if (left > 0 && given < need.remaining()) {
                    shares.put(need, given + 1);
                    left--;
                    gaveAny = true;
                }
            }
        }
        return shares;
    }

    @Override
    public String name() {
        return "Equal share";
    }
}
