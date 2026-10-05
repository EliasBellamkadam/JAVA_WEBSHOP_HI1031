package se.lab.shop.service;
public final class AccessDeniedException extends ShopException {
    public AccessDeniedException() { super("Du saknar behörighet för åtgärden."); }
    public AccessDeniedException(String message) { super(message); }
}
