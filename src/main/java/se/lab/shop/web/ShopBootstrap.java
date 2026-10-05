package se.lab.shop.web;

import java.sql.SQLException;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import se.lab.shop.persistence.Database;
import se.lab.shop.service.ShopService;

/** Creates the application's database and service once when Tomcat starts it. */
@WebListener
public final class ShopBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String configuredUrl = event.getServletContext().getInitParameter("shopDbUrl");
        Database database = configuredUrl == null || configuredUrl.isBlank()
                ? Database.fromEnvironment() : new Database(configuredUrl,
                        event.getServletContext().getInitParameter("shopDbUser"),
                        event.getServletContext().getInitParameter("shopDbPassword"));
        try {
            database.initialize();
        } catch (SQLException exception) {
            throw new IllegalStateException("Kunde inte starta webbshopens databas.", exception);
        }
        event.getServletContext().setAttribute("shopService", new ShopService(database));
    }
}
