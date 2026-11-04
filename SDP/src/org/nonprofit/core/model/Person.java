package org.nonprofit.core.model;

/**
 * Shared base type for every human in the system (donors, volunteers,
 * attendees, beneficiaries).
 */
public abstract class Person {
    private final String id;
    private String name;
    private String email;
    private String phone;
    private ContactPreference preferredContact;

    protected Person(String id, String name, String email, String phone, ContactPreference preferredContact) {
        this.id = id;
        this.name = requireName(name);
        this.email = email;
        this.phone = phone;
        this.preferredContact = preferredContact;
    }

    /**
     * Checks a name before it is used. Subclasses call this before taking
     * the next ID number, so a rejected person does not use one up.
     */
    protected static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name;
    }

    /**
     * Changes the person's contact details. The name is checked first, so a
     * rejected update leaves the person unchanged. The ID never changes.
     */
    public void updateContact(String name, String email, String phone, ContactPreference preferredContact) {
        this.name = requireName(name);
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
