package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.util.concurrent.Future;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class Front {
    private static final Logger logger = LoggerFactory.getLogger(Front.class);
    private static final String HTML;

    static {
        try {
            final var bytes = Objects.requireNonNull(Front.class
                .getClassLoader()
                .getResourceAsStream("index.html"))
                .readAllBytes();

            HTML = new String(bytes, StandardCharsets.UTF_8);
        }
        catch (final Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private final ArSystem system;

    private Front(final ArSystem system) {
        this.system = system;
    }

    public static Future<Front> createAndBindTo(final int port, final RequestHandler handler) {
        return new ArSystem.Builder()
            .name("front-end-" + port)
            .insecure()
            .localPort(port)
            .buildAsync()
            .flatMap(system -> {
                logger.info("Setup front-end system; starting to provide service ...");

                final var frontEnd = new Front(system);
                return system.provide(new HttpService()
                    .name("html")
                    .encodings(JSON, EncodingDescriptor.getOrCreate("HTML"))
                    .basePath("/")
                    .accessPolicy(unrestricted())

                    .get("/", (request, response) -> {
                        response
                            .status(OK)
                            .header("content-type", "text/html")
                            .header("cache-control", "no-store")
                            .body(HTML);
                        return done();
                    })

                    .get("/orders", (request, response) -> {
                        response
                            .status(OK)
                            .body(handler.onGetOrders());
                        return done();
                    })

                    .get("/offers", (request, response) -> {
                        response
                            .status(OK)
                            .body(handler.onGetOffers());
                        return done();
                    })

                    .post("/offers", (request, response) -> request
                        .bodyAs(DataOfferNewDto.class)
                        .flatMap(handler::onOffer)
                        .ifSuccess(ignored -> response.status(OK))))

                    .ifSuccess(handle -> logger.info("Front-end service is now being served"))
                    .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve front-end service", throwable))

                    .pass(frontEnd);
            });
    }

    public int port() {
        return system.localPort();
    }

    public interface RequestHandler {
        DataOfferDto[] onGetOffers();

        DataOrderDto[] onGetOrders();

        Future<?> onOffer(final DataOfferNewDto offer);
    }
}
