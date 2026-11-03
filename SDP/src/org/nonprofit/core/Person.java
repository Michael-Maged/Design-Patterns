package org.nonprofit.core;

/**
 * Shared base type for every human in the system (donors, volunteers,
 * attendees, beneficiaries).
 */
public abstract class Person {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private final ContactPreference preferredContact;

    protected Person(String id, String name, String email, String phone, ContactPreference preferredContact) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferredContact = preferredContact;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public ContactPreference getPreferredContact() { return preferredContact; }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
