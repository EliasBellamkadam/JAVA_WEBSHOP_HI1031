package se.lab.shop.model;

import java.time.LocalDateTime;
import java.util.List;

public record Order(int id, int userId, String username, String status,
                    LocalDateTime createdAt, List<OrderLine> lines) {
    public Order {
        lines = List.copyOf(lines);
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return lines; }
}
