package se.lab.shop.service;

import se.lab.shop.model.*;
import se.lab.shop.persistence.*;
import se.lab.shop.security.Passwords;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Business layer: validation, authorization and JDBC transaction boundaries. */
public final class ShopService {
    private final Database database;
    private final UserDao userDao = new UserDao();
    private final CatalogDao catalogDao = new CatalogDao();
    private final OrderDao orderDao = new OrderDao();
    public ShopService(Database database) { this.database = database; }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null || password.length() > 128) return Optional.empty();
        return read(c -> userDao.findCredentials(c, username.trim())
            .filter(credentials -> credentials.user().active() && Passwords.verify(password, credentials.passwordHash()))
            .map(UserDao.Credentials::user));
    }
    public Optional<User> findActiveUser(int id) {
        return read(c -> userDao.findById(c, id).filter(User::active));
    }
    public List<Product> products() {
        return read(c -> {
            List<Product> copies = new ArrayList<>();
            for(Product product : catalogDao.products(c)) {
                copies.add(new Product(product));
            }
            return List.copyOf(copies);
        });
    }
    public List<Category> categories() { return read(catalogDao::categories); }

    public CartView viewCart(Cart cart) {
        Map<Integer,Integer> snapshot = cart.snapshot();
        return read(c -> {
            List<CartLine> lines = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (var entry : snapshot.entrySet()) {
                Product product = catalogDao.findProduct(c, entry.getKey())
                    .orElseThrow(() -> new ShopException("Varan finns inte längre."));
                CartLine line = new CartLine(new Product(product), entry.getValue());
                lines.add(line);
                total = total.add(line.getTotal());
            }
            return new CartView(List.copyOf(lines), total);
        });
    }
    public void addToCart(Cart cart, int productId, int quantity) {
        if (quantity < 1 || quantity > 999) throw new ShopException("Antalet måste vara 1–999.");
        synchronized (cart) {
            checkCartQuantity(productId, cart.snapshot().getOrDefault(productId, 0) + quantity);
            try { cart.add(productId, quantity); }
            catch (IllegalArgumentException e) { throw new ShopException(e.getMessage()); }
        }
    }
    public void updateCart(Cart cart, int productId, int quantity) {
        if (quantity < 0 || quantity > 999) throw new ShopException("Antalet måste vara 0–999.");
        synchronized (cart) {
            if (!cart.snapshot().containsKey(productId)) throw new ShopException("Varan saknas i korgen.");
            if (quantity > 0) checkCartQuantity(productId, quantity);
            cart.set(productId, quantity);
        }
    }
    private void checkCartQuantity(int productId, int quantity) {
        if (quantity > 999) throw new ShopException("Högst 999 stycken av samma vara.");
        Product product = read(c -> catalogDao.findProduct(c, productId)
            .orElseThrow(() -> new ShopException("Varan finns inte.")));
        if (product.stock() < quantity) throw new ShopException("Det finns inte tillräckligt i lager.");
    }

    /** Stock, order and lines commit together; failure leaves the cart intact. */
    public int checkout(User actor, Cart cart) {
        synchronized (cart) {
            Map<Integer,Integer> items = cart.snapshot();
            if (items.isEmpty()) throw new ShopException("Korgen är tom.");
            int orderId = transaction(c -> {
                User current = requireUser(c, actor);
                int id = orderDao.insert(c, current.id());
                // Lock in the same order for every checkout to avoid reverse-lock deadlocks.
                for (var item : new TreeMap<>(items).entrySet()) {
                    int quantity = item.getValue();
                    if (quantity < 1 || quantity > 999) throw new ShopException("Ogiltigt antal.");
                    Product product = catalogDao.lockProduct(c, item.getKey())
                        .orElseThrow(() -> new ShopException("Varan finns inte längre."));
                    if (!catalogDao.decrementStock(c, product.id(), quantity)) {
                        throw new ShopException("Otillräckligt lager för " + product.name() + ".");
                    }
                    orderDao.insertLine(c, id, product, quantity);
                }
                return id;
            });
            cart.clear();
            return orderId;
        }
    }
    public List<Order> orders(User actor) {
        return read(c -> { requireRole(c, actor, Role.ADMIN, Role.WAREHOUSE); return orderDao.list(c); });
    }
    public void packOrder(User actor, int orderId) {
        transaction(c -> {
            requireRole(c, actor, Role.ADMIN, Role.WAREHOUSE);
            if (!orderDao.pack(c, orderId)) throw new ShopException("Ordern saknas eller är redan packad.");
            return null;
        });
    }
    public List<User> users(User actor) {
        return read(c -> { requireRole(c, actor, Role.ADMIN); return userDao.list(c); });
    }
    /** Blank password preserves the current hash when editing an existing user. */
    public void saveUser(User actor, Integer id, String username, String password, Role role, boolean active) {
        String name = text(username, "Användarnamn", 50);
        if (!name.matches("[A-Za-z0-9_.-]{3,50}")) throw new ShopException("Användarnamn: 3–50 bokstäver (a–z), siffror, punkt, bindestreck eller understreck.");
        if (role == null) throw new ShopException("Välj en behörighet.");
        String hash = null;
        if (password != null && !password.isEmpty()) {
            if (password.length() < 8 || password.length() > 128) throw new ShopException("Lösenordet måste vara 8–128 tecken.");
            hash = Passwords.hash(password);
        } else if (id == null) throw new ShopException("En ny användare måste ha ett lösenord.");
        final String passwordHash = hash;
        transaction(c -> {
            // Serialize admin changes to protect the last active administrator.
            userDao.lockActiveAdmins(c);
            requireRole(c, actor, Role.ADMIN);
            if (id == null) userDao.insert(c, name, passwordHash, role, active);
            else {
                User old = userDao.findById(c, id).orElseThrow(() -> new ShopException("Användaren finns inte."));
                if (old.active() && old.role() == Role.ADMIN && (!active || role != Role.ADMIN) && userDao.countActiveAdmins(c) <= 1)
                    throw new ShopException("Den sista aktiva administratören måste finnas kvar.");
                userDao.update(c, id, name, passwordHash, role, active);
            }
            return null;
        });
    }
    public void saveProduct(User actor, Integer id, int categoryId, String name, BigDecimal price, int stock) {
        String productName = text(name, "Varunamn", 100);
        if (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new ShopException("Ange ett giltigt pris som är minst 0.");
        final BigDecimal amount;
        try { amount = price.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new ShopException("Priset får ha högst två decimaler."); }
        if (stock < 0 || stock > 1_000_000) throw new ShopException("Lagerantal måste vara 0–1 000 000.");
        transaction(c -> {
            requireRole(c, actor, Role.ADMIN);
            if (catalogDao.categories(c).stream().noneMatch(category -> category.id() == categoryId)) throw new ShopException("Kategorin finns inte.");
            if (id != null && catalogDao.findProduct(c, id).isEmpty()) throw new ShopException("Varan finns inte.");
            catalogDao.saveProduct(c, id, categoryId, productName, amount, stock);
            return null;
        });
    }
    public void saveCategory(User actor, Integer id, String name) {
        String categoryName = text(name, "Kategorinamn", 100);
        transaction(c -> {
            requireRole(c, actor, Role.ADMIN);
            if (id != null && catalogDao.categories(c).stream().noneMatch(category -> category.id() == id)) throw new ShopException("Kategorin finns inte.");
            catalogDao.saveCategory(c, id, categoryName);
            return null;
        });
    }
    private User requireUser(Connection c, User actor) throws SQLException {
        if (actor == null) throw new AccessDeniedException();
        // A caller or session cannot grant itself a role: read the current database value.
        return userDao.findById(c, actor.id()).filter(User::active).orElseThrow(AccessDeniedException::new);
    }
    private void requireRole(Connection c, User actor, Role... roles) throws SQLException {
        Role actual = requireUser(c, actor).role();
        for (Role allowed : roles) if (actual == allowed) return;
        throw new AccessDeniedException();
    }
    private static String text(String value, String label, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) throw new ShopException(label + " måste innehålla 1–" + maxLength + " tecken.");
        return value.trim();
    }
    private <T> T read(SqlWork<T> work) {
        try (Connection c = database.open()) { return work.run(c); }
        catch (SQLException e) { throw databaseError(e); }
    }
    private <T> T transaction(SqlWork<T> work) {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql) throw databaseError(sql);
                throw (RuntimeException)e;
            }
        } catch (SQLException e) { throw databaseError(e); }
    }
    private ShopException databaseError(SQLException e) {
        if ("23000".equals(e.getSQLState()) && e.getErrorCode() == 1062) return new ShopException("Namnet är redan upptaget.", e);
        return new ShopException("Databasen kunde inte slutföra åtgärden. Försök igen.", e);
    }
    @FunctionalInterface private interface SqlWork<T> { T run(Connection c) throws SQLException; }
}
