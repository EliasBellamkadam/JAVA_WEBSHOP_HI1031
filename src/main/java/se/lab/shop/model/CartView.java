package se.lab.shop.model;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartLine> lines, BigDecimal total) {
    public CartView {
        lines = List.copyOf(lines);
    }

    public List<CartLine> getLines() { return lines; }
    public BigDecimal getTotal() { return total; }
}
