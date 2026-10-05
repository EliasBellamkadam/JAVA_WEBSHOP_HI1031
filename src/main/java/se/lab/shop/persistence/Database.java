package se.lab.shop.persistence;

import se.lab.shop.model.Role;
import se.lab.shop.security.Passwords;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Öppnar JDBC-anslutningar. Affärslagret äger transaktioner och stänger anslutningarna. */
public final class Database {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public Database(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "JDBC-adressen saknas.");
        this.username = Objects.requireNonNull(username, "Databasanvändaren saknas.");
        this.password = Objects.requireNonNull(password, "Databaslösenordet saknas.");
        if (!jdbcUrl.startsWith("jdbc:mysql:") || username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Ange en MySQL JDBC-adress, användare och lösenord.");
        }
        try {
            // DriverManager har redan startat i Tomcat. Ladda webbappens JDBC-driver explicit.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL Connector/J saknas.", exception);
        }
    }

    /** MySQL Server och databaskontot skapas separat, till exempel genom Workbench. */
    public static Database fromEnvironment() {
        String password = System.getenv("SHOP_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("SHOP_DB_PASSWORD saknas. Följ körguiden och ange "
                    + "databaskontots lösenord i Tomcats miljövariabler.");
        }
        return new Database(environmentOrDefault("SHOP_DB_URL",
                "jdbc:mysql://localhost:3306/webshop?connectionTimeZone=%2B00:00&forceConnectionTimeZoneToSession=true"),
                environmentOrDefault("SHOP_DB_USER", "webshop"), password);
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        try {
            // Läs aktuell data efter väntan på radlås, även om transaktionen redan läst andra rader.
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    /** Skapar schema utan att återställa data. Första startens exempeldata sparas i en transaktion. */
    public void initialize() throws SQLException {
        String schema = readSchema();
        try (Connection connection = open()) {
            try (Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
            connection.setAutoCommit(false);
            try {
                if (hasNoUsers(connection)) {
                    seed(connection);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream input = Database.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new SQLException("Databasens schema.sql saknas.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SQLException("Kunde inte läsa databasens schema.", exception);
        }
    }

    private static boolean hasNoUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            result.next();
            return result.getInt(1) == 0;
        }
    }

    private static void seed(Connection connection) throws SQLException {
        UserDao users = new UserDao();
        users.insert(connection, "kund", Passwords.hash("kund12345"), Role.CUSTOMER, true);
        users.insert(connection, "admin", Passwords.hash("admin12345"), Role.ADMIN, true);
        users.insert(connection, "lager", Passwords.hash("lager12345"), Role.WAREHOUSE, true);

        CatalogDao catalog = new CatalogDao();
        int accessories = catalog.saveCategory(connection, null, "Tillbehör");
        int office = catalog.saveCategory(connection, null, "Kontor");
        catalog.saveProduct(connection, null, accessories, "Java-mugg", new BigDecimal("129.00"), 10);
        catalog.saveProduct(connection, null, office, "Anteckningsbok", new BigDecimal("49.00"), 20);
        catalog.saveProduct(connection, null, accessories, "USB-kabel", new BigDecimal("79.00"), 0);
    }
}
