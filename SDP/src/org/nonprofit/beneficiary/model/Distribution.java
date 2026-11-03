package org.nonprofit.beneficiary.model;

import java.time.LocalDate;

/** A record of units handed to a beneficiary against one need, under a named program. */
public class Distribution {
    private static int nextId = 1;

    private final String id;
    private final Need need;
    private final int quantity;
    private final String program;
    private final LocalDate date;

    Distribution(Need need, int quantity, String program, LocalDate date) {
        this.id = "DST-" + nextId++;
        this.need = need;
        this.quantity = quantity;
        this.program = program;
        this.date = date;
    }

    public String getId() { return id; }
    public Need getNeed() { return need; }
    public Beneficiary getBeneficiary() { return need.getBeneficiary(); }
    public String getCategory() { return need.getCategory(); }
    public int getQuantity() { return quantity; }
    public String getProgram() { return program; }
    public LocalDate getDate() { return date; }

    public String summary() {
        return String.format("%s %s: %d x %s to %s (%s)",
                id, date, quantity, getCategory(), getBeneficiary().getName(), program);
    }
}
