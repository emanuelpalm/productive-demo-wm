package wm_demo.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wm_demo.middleware.Middleware;
import wm_demo.seller.Seller;

import java.util.logging.Level;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        Seller.main(args);
        Middleware.main(args);
    }

    static {
        final var logLevel = Level.ALL;
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        final var root = java.util.logging.Logger.getLogger("");
        root.setLevel(logLevel);
        for (final var handler : root.getHandlers()) {
            handler.setLevel(logLevel);
        }
    }
}