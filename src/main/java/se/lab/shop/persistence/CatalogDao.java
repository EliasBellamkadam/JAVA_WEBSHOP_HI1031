package se.lab.shop.persistence;

import se.lab.shop.model.Category;
import se.lab.shop.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC för katalog och lager. Anslutning och transaktionsgränser ägs av affärslagret. */
public final class CatalogDao {
    private static final String PRODUCT_QUERY =
            "SELECT p.id, p.category_id, c.name AS category_name, p.name, p.price, p.stock "
                    + "FROM products p JOIN categories c ON c.id = p.category_id";

    public List<Product> products(Connection connection) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                PRODUCT_QUERY + " ORDER BY c.name, p.name, p.id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                products.add(readProduct(result));
            }
        }
        return List.copyOf(products);
    }

    public List<Category> categories(Connection connection) throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name FROM categories ORDER BY name, id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                categories.add(new Category(result.getInt("id"), result.getString("name")));
            }
        }
        return List.copyOf(categories);
    }

    public Optional<Product> findProduct(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(PRODUCT_QUERY + " WHERE p.id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readProduct(result)) : Optional.empty();
            }
        }
    }

    /** Kräver en transaktion. Flera varor ska låsas i stigande id-ordning för att undvika deadlock. */
    public Optional<Product> lockProduct(Connection connection, int id) throws SQLException {
        // Lås bara varuraden. Ett lås på katalogens JOIN skulle även låsa kategorin
        // och i onödan blockera köp av andra varor i samma kategori.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM products WHERE id = ? FOR UPDATE")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
            }
        }
        return findProduct(connection, id);
    }

    /** Null-id skapar en vara; annars uppdateras den befintliga. */
    public int saveProduct(Connection connection, Integer id, int categoryId, String name,
                           BigDecimal price, int stock) throws SQLException {
        String sql = id == null
                ? "INSERT INTO products (category_id, name, price, stock) VALUES (?, ?, ?, ?)"
                : "UPDATE products SET category_id = ?, name = ?, price = ?, stock = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, categoryId);
            statement.setString(2, name);
            statement.setBigDecimal(3, price);
            statement.setInt(4, stock);
            if (id != null) {
                statement.setInt(5, id);
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Varan finns inte.");
            }
            if (id != null) {
                return id;
            }
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Varans id kunde inte hämtas.");
                }
                return result.getInt(1);
            }
        }
    }

    /** Null-id skapar en kategori; annars uppdateras den befintliga. */
    public int saveCategory(Connection connection, Integer id, String name) throws SQLException {
        String sql = id == null
                ? "INSERT INTO categories (name) VALUES (?)"
                : "UPDATE categories SET name = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            if (id != null) {
                statement.setInt(2, id);
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Kategorin finns inte.");
            }
            if (id != null) {
                return id;
            }
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Kategorins id kunde inte hämtas.");
                }
                return result.getInt(1);
            }
        }
    }

    /** Villkoret skyddar även på SQL-nivå mot negativt saldo vid samtidiga köp. */
    public boolean decrementStock(Connection connection, int productId, int quantity) throws SQLException {
        if (quantity <= 0 || quantity > 999) {
            throw new IllegalArgumentException("Antalet måste vara mellan 1 och 999.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
            statement.setInt(1, quantity);
            statement.setInt(2, productId);
            statement.setInt(3, quantity);
            return statement.executeUpdate() == 1;
        }
    }

    private static Product readProduct(ResultSet result) throws SQLException {
        return new Product(result.getInt("id"), result.getInt("category_id"),
                result.getString("category_name"), result.getString("name"),
                result.getBigDecimal("price"), result.getInt("stock"));
    }
}
