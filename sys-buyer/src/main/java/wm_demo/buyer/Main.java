package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Buyer System");
        try {
            final var back = Back.createAndBindTo(63001)
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to setup application back-end", throwable))
                .await();
            logger.info("Application back-end now available via port " + back.port());
            Front.createAndBindTo(8080, back.handler())
                .ifSuccess(front -> logger.info("Application front-end now available via port " + front.port()))
                .onFailure(throwable -> logger.error("Failed to setup application front-end", throwable));
        }
        catch (final Throwable throwable) {
            logger.error("Application start-up failed", throwable);
        }
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
