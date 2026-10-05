package se.lab.shop.web;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import se.lab.shop.model.Cart;
import se.lab.shop.service.ShopService;

/** Small helpers shared by the HTTP boundary; no business rules belong here. */
final class WebSupport {
    private WebSupport() { }

    static ShopService service(HttpServletRequest request) {
        return (ShopService) request.getServletContext().getAttribute("shopService");
    }

    static Cart cart(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    static void view(HttpServletRequest request, HttpServletResponse response, String name)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(request, response);
    }

    static void error(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("error", message);
        view(request, response, "error");
    }

    static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        // Paths are fixed application routes; user input never chooses the destination.
        response.sendRedirect(request.getContextPath() + path);
    }
}
