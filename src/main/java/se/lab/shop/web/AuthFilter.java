package se.lab.shop.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import se.lab.shop.model.Role;
import se.lab.shop.model.User;

/** Refreshes authentication, checks route roles and validates every submitted form. */
@WebFilter("/*")
public final class AuthFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        String path = request.getServletPath();
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        if (session.getAttribute("csrfToken") == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            session.setAttribute("csrfToken", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            user = WebSupport.service(request).findActiveUser(user.id()).orElse(null);
            if (user == null) {
                session.removeAttribute("user");
                session.removeAttribute("cart");
            } else {
                session.setAttribute("user", user);
            }
        }
        request.setAttribute("currentUser", user);
        request.setAttribute("csrfToken", session.getAttribute("csrfToken"));

        if (!"/login".equals(path) && user == null) {
            WebSupport.redirect(request, response, "/login");
            return;
        }
        if ((path.startsWith("/admin/") && user.role() != Role.ADMIN)
                || (path.startsWith("/warehouse/") && user.role() != Role.ADMIN && user.role() != Role.WAREHOUSE)) {
            WebSupport.error(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Du har inte behörighet till den här sidan.");
            return;
        }
        if ("POST".equals(request.getMethod()) && !validToken(request, session)) {
            WebSupport.error(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Formuläret är inte giltigt. Ladda om sidan och försök igen.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean validToken(HttpServletRequest request, HttpSession session) {
        String supplied = request.getParameter("csrfToken");
        String expected = (String) session.getAttribute("csrfToken");
        return supplied != null && expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
