package se.lab.shop.model;

import java.math.BigDecimal;

public record CartLine(Product product, int quantity) {
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotal() { return product.price().multiply(BigDecimal.valueOf(quantity)); }
}
