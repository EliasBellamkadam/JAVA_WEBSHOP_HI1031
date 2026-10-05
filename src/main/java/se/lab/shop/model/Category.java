package se.lab.shop.model;

public record Category(int id, String name) {
    public int getId() { return id; }
    public String getName() { return name; }
}
