package wm_demo.shared;

import wm_demo.middleware.Middleware;
import wm_demo.seller.Seller;

import java.util.logging.Level;

public class Main {
    public static void main(final String[] args) {
        Seller.main(args);
        Middleware.main(args);
    }

    static {
        final var logLevel = Level.INFO;
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        final var root = java.util.logging.Logger.getLogger("");
        root.setLevel(logLevel);
        for (final var handler : root.getHandlers()) {
            handler.setLevel(logLevel);
        }
    }
}