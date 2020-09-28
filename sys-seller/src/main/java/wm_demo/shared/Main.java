package wm_demo.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wm_demo.middleware.Middleware;
import wm_demo.seller.PricingModel;
import wm_demo.seller.Seller;

import java.util.logging.Level;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Buyer System");

        final var sellerPricingModel = new PricingModel.Builder()
            .quantityMin(1)
            .quantityMax(3)
            .minUnitPrice(100.0)
            .drilledPriceMultiplier(1.05)
            .milledPriceMultiplier(1.15)
            .quantityPriceMultiplier(0.99)
            .build();

        try {
            final var seller = Seller.createBindToPortAndUse(9002, sellerPricingModel)
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to setup seller system", throwable))
                .await();
            logger.info("Seller now available via port " + seller.port());

            final var middleware = Middleware.createAndBindTo(9003)
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to setup middleware system", throwable))
                .await();
            logger.info("Middleware now available via port " + middleware.port());
        }
        catch (final Throwable throwable) {
            logger.error("Application start-up failed", throwable);
        }
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