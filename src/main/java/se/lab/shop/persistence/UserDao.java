package se.lab.shop.persistence;

import se.lab.shop.model.Role;
import se.lab.shop.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC för användare. Metoderna varken stänger anslutningen eller avslutar dess transaktion. */
public final class UserDao {
    public record Credentials(User user, String passwordHash) { }

    public List<User> list(Connection connection) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, role, active FROM users ORDER BY username, id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(readUser(result));
            }
        }
        return List.copyOf(users);
    }

    public Optional<User> findById(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, role, active FROM users WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readUser(result)) : Optional.empty();
            }
        }
    }

    public Optional<Credentials> findCredentials(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, role, active, password_hash FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new Credentials(readUser(result), result.getString("password_hash")))
                        : Optional.empty();
            }
        }
    }

    public int insert(Connection connection, String username, String passwordHash, Role role, boolean active)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password_hash, role, active) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.setBoolean(4, active);
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Användarens id kunde inte hämtas.");
                }
                return result.getInt(1);
            }
        }
    }

    /** En null-hash behåller tidigare lösenord. */
    public void update(Connection connection, int id, String username, String passwordHash, Role role,
                       boolean active) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET username = ?, password_hash = COALESCE(?, password_hash), "
                        + "role = ?, active = ? WHERE id = ?")) {
            statement.setString(1, username);
            if (passwordHash == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, passwordHash);
            }
            statement.setString(3, role.name());
            statement.setBoolean(4, active);
            statement.setInt(5, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Användaren finns inte.");
            }
        }
    }

    public int countActiveAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = TRUE");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    /** Kräver en öppen transaktion; låsen behålls tills affärslagret commit/rollback görs. */
    public void lockActiveAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id FOR UPDATE");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                result.getInt(1);
            }
        }
    }

    private static User readUser(ResultSet result) throws SQLException {
        return new User(result.getInt("id"), result.getString("username"),
                Role.valueOf(result.getString("role")), result.getBoolean("active"));
    }
}
