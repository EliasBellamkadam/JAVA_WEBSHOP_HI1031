package se.lab.shop.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Sessionskorg med produkt-id och antal. Priser och lagersaldo hämtas från databasen. */
public final class Cart {
    private static final int MAX_QUANTITY = 999;
    private static final int MAX_LINES = 100;
    private final Map<Integer, Integer> quantities = new TreeMap<>();

    public synchronized void add(int productId, int quantity) {
        if (quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Antalet måste vara mellan 1 och 999.");
        }
        set(productId, quantities.getOrDefault(productId, 0) + quantity);
    }

    /** Antalet noll tar bort raden. Synkronisering skyddar mot parallella session-anrop. */
    public synchronized void set(int productId, int quantity) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Ogiltig vara.");
        }
        if (quantity < 0 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Antalet måste vara mellan 0 och 999.");
        }
        if (quantity == 0) {
            quantities.remove(productId);
        } else {
            if (!quantities.containsKey(productId) && quantities.size() >= MAX_LINES) {
                throw new IllegalArgumentException("Korgen får innehålla högst 100 olika varor.");
            }
            quantities.put(productId, quantity);
        }
    }

    /** Fristående, oföränderlig kopia i produkt-id-ordning för deterministisk låsordning. */
    public synchronized Map<Integer, Integer> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(quantities));
    }

    public synchronized void clear() {
        quantities.clear();
    }

    public synchronized int getItemCount() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }
}
