package se.lab.shop.model;

/** Inloggad användare; lösenord och lösenordshash lagras aldrig i sessionen. */
public record User(int id, String username, Role role, boolean active) {
    public int getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean getActive() { return active; }
}
