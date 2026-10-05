package se.lab.shop.model;

import java.math.BigDecimal;

/** Namn och pris är ögonblicksbilder från när ordern skickades. */
public record OrderLine(int productId, String productName, BigDecimal unitPrice, int quantity) {
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
