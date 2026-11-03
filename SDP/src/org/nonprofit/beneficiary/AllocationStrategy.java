package org.nonprofit.beneficiary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STRATEGY pattern (strategy interface): one rule for sharing a limited
 * stock between the open needs of one category. {@link BeneficiaryService}
 * holds one strategy and it can be swapped at runtime, so the same stock
 * may go to different people.
 */
public interface AllocationStrategy {

    /**
     * @param needs     open needs of one category, in the order they were recorded
     * @param available units in stock (at least 1)
     * @return units per need, in the order they are handed out; needs that get nothing are left out
     */
    Map<Need, Integer> allocate(List<Need> needs, int available);

    /** Short label shown in menus and demo output. */
    String name();

    /** Shared by strategies that only differ in order: fill each need fully before moving on. */
    static Map<Need, Integer> fillInOrder(List<Need> ordered, int available) {
        Map<Need, Integer> shares = new LinkedHashMap<>();
        int left = available;
        for (Need need : ordered) {
            if (left == 0) {
                break;
            }
            int units = Math.min(need.remaining(), left);
            shares.put(need, units);
            left -= units;
        }
        return shares;
    }
}
