package se.lab.shop.service;
/** A validation or business error that can safely be displayed to the user. */
public class ShopException extends RuntimeException {
    public ShopException(String message) { super(message); }
    public ShopException(String message, Throwable cause) { super(message, cause); }
}
