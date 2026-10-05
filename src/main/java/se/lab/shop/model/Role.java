package se.lab.shop.model;

/** De tre behörighetsklasserna i laborationen. */
public enum Role {
    CUSTOMER("Kund"),
    ADMIN("Administratör"),
    WAREHOUSE("Lagerpersonal");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
