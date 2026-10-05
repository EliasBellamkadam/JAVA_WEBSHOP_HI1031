package se.lab.shop.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.lab.shop.model.Category;
import se.lab.shop.model.Product;
import se.lab.shop.model.Role;
import se.lab.shop.model.User;
import se.lab.shop.service.AccessDeniedException;
import se.lab.shop.service.ShopException;
import se.lab.shop.service.ShopService;

/** MVC controller: translates HTTP input and delegates all business rules to the service. */
@WebServlet(urlPatterns = {"", "/login", "/logout", "/products", "/cart", "/cart/add", "/cart/update",
        "/checkout", "/admin/users", "/admin/users/save", "/admin/products", "/admin/products/save",
        "/admin/categories", "/admin/categories/save", "/warehouse/orders", "/warehouse/orders/pack"})
public final class ShopController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        try {
            ShopService service = WebSupport.service(request);
            switch (request.getServletPath()) {
                case "", "/", "/products" -> {
                    request.setAttribute("products", service.products());
                    WebSupport.view(request, response, "products");
                }
                case "/login" -> {
                    if (request.getAttribute("currentUser") != null) {
                        WebSupport.redirect(request, response, "/products");
                    } else {
                        WebSupport.view(request, response, "login");
                    }
                }
                case "/cart" -> {
                    request.setAttribute("cartView", service.viewCart(WebSupport.cart(request)));
                    WebSupport.view(request, response, "cart");
                }
                case "/admin/users" -> {
                    List<User> users = service.users(currentUser(request));
                    request.setAttribute("users", users);
                    request.setAttribute("roles", Role.values());
                    Integer id = optionalId(request);
                    if (id != null) {
                        request.setAttribute("editUser", users.stream().filter(user -> user.id() == id)
                                .findFirst().orElseThrow(() -> new ShopException("Användaren finns inte.")));
                    }
                    WebSupport.view(request, response, "users");
                }
                case "/admin/products" -> {
                    List<Product> products = service.products();
                    request.setAttribute("products", products);
                    request.setAttribute("categories", service.categories());
                    Integer id = optionalId(request);
                    if (id != null) {
                        request.setAttribute("editProduct", products.stream().filter(product -> product.id() == id)
                                .findFirst().orElseThrow(() -> new ShopException("Varan finns inte.")));
                    }
                    WebSupport.view(request, response, "product-admin");
                }
                case "/admin/categories" -> {
                    List<Category> categories = service.categories();
                    request.setAttribute("categories", categories);
                    Integer id = optionalId(request);
                    if (id != null) {
                        request.setAttribute("editCategory", categories.stream().filter(category -> category.id() == id)
                                .findFirst().orElseThrow(() -> new ShopException("Kategorin finns inte.")));
                    }
                    WebSupport.view(request, response, "categories");
                }
                case "/warehouse/orders" -> {
                    request.setAttribute("orders", service.orders(currentUser(request)));
                    WebSupport.view(request, response, "orders");
                }
                default -> WebSupport.error(request, response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Den här åtgärden måste skickas med ett formulär.");
            }
        } catch (AccessDeniedException exception) {
            WebSupport.error(request, response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (ShopException exception) {
            WebSupport.error(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            WebSupport.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Ett fält har ett ogiltigt värde.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ShopService service = WebSupport.service(request);
            String destination;
            String message;
            switch (request.getServletPath()) {
                case "/login" -> {
                    User user = service.login(value(request, "username"), value(request, "password"))
                            .orElseThrow(() -> new ShopException("Fel användarnamn eller lösenord."));
                    request.changeSessionId();
                    request.getSession().setAttribute("user", user);
                    request.getSession().removeAttribute("cart");
                    destination = "/products";
                    message = "Du är inloggad.";
                }
                case "/logout" -> {
                    request.getSession().invalidate();
                    WebSupport.redirect(request, response, "/login");
                    return;
                }
                case "/cart/add" -> {
                    service.addToCart(WebSupport.cart(request), number(request, "productId"), number(request, "quantity"));
                    destination = "/products";
                    message = "Varan har lagts i varukorgen.";
                }
                case "/cart/update" -> {
                    service.updateCart(WebSupport.cart(request), number(request, "productId"), number(request, "quantity"));
                    destination = "/cart";
                    message = "Varukorgen är uppdaterad.";
                }
                case "/checkout" -> {
                    int id = service.checkout(currentUser(request), WebSupport.cart(request));
                    destination = "/cart";
                    message = "Order " + id + " har skickats.";
                }
                case "/admin/users/save" -> {
                    service.saveUser(currentUser(request), optionalId(request), value(request, "username"),
                            value(request, "password"), Role.valueOf(value(request, "role")),
                            request.getParameter("active") != null);
                    destination = "/admin/users";
                    message = "Användaren har sparats.";
                }
                case "/admin/products/save" -> {
                    service.saveProduct(currentUser(request), optionalId(request), number(request, "categoryId"),
                            value(request, "name"), new BigDecimal(value(request, "price")), number(request, "stock"));
                    destination = "/admin/products";
                    message = "Varan har sparats.";
                }
                case "/admin/categories/save" -> {
                    service.saveCategory(currentUser(request), optionalId(request), value(request, "name"));
                    destination = "/admin/categories";
                    message = "Kategorin har sparats.";
                }
                case "/warehouse/orders/pack" -> {
                    service.packOrder(currentUser(request), number(request, "id"));
                    destination = "/warehouse/orders";
                    message = "Ordern är packad.";
                }
                default -> {
                    WebSupport.error(request, response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                            "Den här sidan stöder inte den åtgärden.");
                    return;
                }
            }
            request.getSession().setAttribute("flash", message);
            WebSupport.redirect(request, response, destination);
        } catch (AccessDeniedException exception) {
            WebSupport.error(request, response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (ShopException exception) {
            failedForm(request, response, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            failedForm(request, response, "Ett fält har ett ogiltigt värde.");
        }
    }

    private void failedForm(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        if ("/login".equals(request.getServletPath())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", message);
            request.setAttribute("username", value(request, "username"));
            WebSupport.view(request, response, "login");
            return;
        }
        String destination = switch (request.getServletPath()) {
            case "/cart/add" -> "/products";
            case "/cart/update", "/checkout" -> "/cart";
            case "/admin/users/save" -> "/admin/users";
            case "/admin/products/save" -> "/admin/products";
            case "/admin/categories/save" -> "/admin/categories";
            case "/warehouse/orders/pack" -> "/warehouse/orders";
            default -> "/products";
        };
        request.getSession().setAttribute("flashError", message);
        WebSupport.redirect(request, response, destination);
    }

    private void consumeFlash(HttpServletRequest request) {
        for (String name : List.of("flash", "flashError")) {
            Object message = request.getSession().getAttribute(name);
            if (message != null) {
                request.setAttribute(name, message);
                request.getSession().removeAttribute(name);
            }
        }
    }

    private User currentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            throw new AccessDeniedException("Du måste vara inloggad.");
        }
        return user;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value;
    }

    private static int number(HttpServletRequest request, String name) {
        return Integer.parseInt(value(request, name));
    }

    private static Integer optionalId(HttpServletRequest request) {
        String value = value(request, "id");
        return value.isBlank() ? null : Integer.valueOf(value);
    }
}
