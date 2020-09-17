package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Buyer System");
        Front
            .createAndBindTo(8080, new Front.RequestHandler() {
                @Override
                public OrderDto[] onGetOrders() {
                    return new OrderDto[]{
                        new OrderBuilder()
                            .quantity(5)
                            .articleId("ART-PM")
                            .build(),
                        new OrderBuilder()
                            .quantity(1)
                            .articleId("ART-DM")
                            .build(),
                        new OrderBuilder()
                            .quantity(3)
                            .articleId("ART-PP")
                            .build(),
                        new OrderBuilder()
                            .quantity(18)
                            .articleId("ART-DP")
                            .build(),
                    };
                }

                @Override
                public void onOffer(OfferDto offer) {

                }
            })
            .ifSuccess(front -> logger.info("Application front-end now available via port " + front.port()))
            .onFailure(throwable -> logger.error("Failed to setup application front-end", throwable));
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
