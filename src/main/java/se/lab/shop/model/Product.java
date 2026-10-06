package se.lab.shop.model;

import java.math.BigDecimal;

public record Product(int id, int categoryId, String categoryName, String name,
                      BigDecimal price, int stock) {
    public Product(Product original){
        this(original.id(), original.categoryId(), original.categoryName(),
                original.name(), original.price(), original.stock());
    }

    public int getId() { return id; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
}
