package se.lab.shop.persistence;

import se.lab.shop.model.Order;
import se.lab.shop.model.OrderLine;
import se.lab.shop.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC för ordrar. Orderhuvud, rader och lagerändring måste ingå i samma affärstransaktion. */
public final class OrderDao {
    public int insert(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO orders (user_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Orderns id kunde inte hämtas.");
                }
                return result.getInt(1);
            }
        }
    }

    /** Sparar varans namn/pris vid beställningen, så senare katalogändringar inte ändrar ordern. */
    public void insertLine(Connection connection, int orderId, Product product, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO order_lines (order_id, product_id, product_name, unit_price, quantity) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, orderId);
            statement.setInt(2, product.id());
            statement.setString(3, product.name());
            statement.setBigDecimal(4, product.price());
            statement.setInt(5, quantity);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Orderraden kunde inte sparas.");
            }
        }
    }

    public List<Order> list(Connection connection) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.id, o.user_id, u.username, o.status, o.created_at, "
                + "l.product_id, l.product_name, l.unit_price, l.quantity "
                + "FROM orders o JOIN users u ON u.id = o.user_id "
                + "LEFT JOIN order_lines l ON l.order_id = o.id ORDER BY o.id DESC, l.product_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            PendingOrder current = null;
            while (result.next()) {
                int id = result.getInt("id");
                if (current == null || current.id != id) {
                    if (current != null) {
                        orders.add(current.toOrder());
                    }
                    current = new PendingOrder(id, result.getInt("user_id"), result.getString("username"),
                            result.getString("status"), result.getTimestamp("created_at").toLocalDateTime());
                }
                int productId = result.getInt("product_id");
                if (!result.wasNull()) {
                    current.lines.add(new OrderLine(productId, result.getString("product_name"),
                            result.getBigDecimal("unit_price"), result.getInt("quantity")));
                }
            }
            if (current != null) {
                orders.add(current.toOrder());
            }
        }
        return List.copyOf(orders);
    }

    /** Den villkorliga ändringen förhindrar att samma order packas två gånger. */
    public boolean pack(Connection connection, int orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET status = 'PACKED' WHERE id = ? AND status = 'NEW'")) {
            statement.setInt(1, orderId);
            return statement.executeUpdate() == 1;
        }
    }

    private static final class PendingOrder {
        private final int id;
        private final int userId;
        private final String username;
        private final String status;
        private final LocalDateTime createdAt;
        private final List<OrderLine> lines = new ArrayList<>();

        private PendingOrder(int id, int userId, String username, String status, LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.createdAt = createdAt;
        }

        private Order toOrder() {
            return new Order(id, userId, username, status, createdAt, lines);
        }
    }
}
