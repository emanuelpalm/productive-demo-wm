package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import wm_demo.common.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.CREATED;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.whitelist;
import static se.arkalix.util.concurrent.Future.done;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String HTML;

    private static final Map<Long, DataOfferWithContextDto> offers = new ConcurrentHashMap<>();
    private static final Map<String, DataOrderDto> orders = new ConcurrentHashMap<>();
    private static final Map<Long, PendingCounterOffer> pendingCounterOffers = new ConcurrentHashMap<>();

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Buyer System");
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort("sys-buyer.uni", 9001)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress("service-registry.uni", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            final var facade = system
                .pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                .orElseThrow(() -> new IllegalStateException("No " +
                    "HttpJsonTrustedContractNegotiatorPlugin is " +
                    "available; cannot negotiate"));

            system.provide(new HttpService()
                .name("buyer-front-end")
                .encodings(JSON, EncodingDescriptor.getOrCreate("HTML"))
                .accessPolicy(whitelist("buyer_operator"))
                .basePath("/")

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
                        .body(new ArrayList<>(orders.values()));
                    return done();
                })

                .get("/offers", (request, response) -> {
                    response
                        .status(OK)
                        .body(new ArrayList<>(offers.values()));
                    return done();
                })

                .post("/offers", (request, response) -> request
                    .bodyAs(DataOfferDto.class)
                    .flatMap(offer -> handleOfferUsing(offer, facade))
                    .ifSuccess(ignored -> response.status(OK))))

                .ifSuccess(handle -> logger.info("Buyer front-end service is now being served"))
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve buyer front-end service", throwable))
                .await();

            system.provide(new HttpService()
                .name("buyer-back-end")
                .encodings(JSON)
                .accessPolicy(token())
                .basePath("/back")

                .post("/orders", (request, response) -> request
                    .bodyAs(DataOrderDto.class)
                    .ifSuccess(order -> {
                        final var isCreated = orders.put(order.articleId(), order) == null;
                        response.status(isCreated ? CREATED : OK);
                    })))

                .ifSuccess(handle -> logger.info("Buyer back-end service is now being served"))
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve buyer back-end service", throwable))
                .await();
        }
        catch (final Throwable throwable) {
            logger.error("Buyer system start-up failed", throwable);
        }
    }

    private static Future<?> handleOfferUsing(final DataOffer offer, final ArTrustedContractNegotiatorPluginFacade facade) {
        final var contract = offer.toContract();

        // Handle counter-offer replies.
        if (offer.id().isPresent()) {
            final var id = offer.id().get();
            final var counterOffer = pendingCounterOffers.remove(id);
            if (counterOffer == null) {
                return Future.failure(new IllegalStateException("No pending counter-offer with ID " + id + " exists"));
            }
            if (contract.equals(counterOffer.contract)) {
                return counterOffer.responder.accept();
            }
            return counterOffer.responder.offer(new SimplifiedContractCounterOffer.Builder()
                .contracts(contract)
                .validFor(Duration.ofMinutes(3))
                .build());
        }

        return facade.offer("Buyer", "Seller", Duration.ofMinutes(3), List.of(contract),
            new TrustedContractNegotiatorHandler() {
                @Override
                public void onAccept(final TrustedContractNegotiationDto negotiation) {
                    offers.computeIfPresent(negotiation.id(), (id, offer) -> offer.rebuild()
                        .status(DataOfferStatus.ACCEPTED)
                        .build());
                    orders.compute(offer.articleId(), (articleId, order) -> {
                        if (order == null) {
                            return new DataOrderBuilder()
                                .quantity(offer.quantity())
                                .articleId(articleId)
                                .build();
                        }
                        else {
                            return order.rebuild()
                                .quantity(order.quantity() + offer.quantity())
                                .build();
                        }
                    });
                }

                @Override
                public void onOffer(final TrustedContractNegotiationDto negotiation, final TrustedContractNegotiatorResponder responder) {
                    final var contracts = negotiation.offer().contracts();
                    if (contracts.size() != 1) {
                        throw new IllegalArgumentException("Negotiation offer must contain exactly one contract");
                    }
                    final var contract = negotiation.offer().contracts().get(0);
                    pendingCounterOffers.put(negotiation.id(), new PendingCounterOffer(contract, responder));
                    offers.computeIfPresent(negotiation.id(), (id, offer) -> offer.rebuild()
                        .status(DataOfferStatus.COUNTERED)
                        .counterOffer(DataOffer.fromContract(contract))
                        .build());
                }

                @Override
                public void onReject(final TrustedContractNegotiationDto negotiation) {
                    offers.computeIfPresent(negotiation.id(), (id, offer) -> offer.rebuild()
                        .status(DataOfferStatus.REJECTED)
                        .build());
                }

                @Override
                public void onExpiry(final long negotiationId) {
                    offers.computeIfPresent(negotiationId, (id, offer) -> {
                        final var status = offer.status();
                        if (status != DataOfferStatus.ACCEPTED && status != DataOfferStatus.REJECTED) {
                            offer = offer.rebuild()
                                .status(DataOfferStatus.EXPIRED)
                                .build();
                        }
                        return offer;
                    });
                }

                @Override
                public void onFault(final long negotiationId, final Throwable throwable) {
                    logger.error("Failed to handle contract offer", throwable);
                    offers.computeIfPresent(negotiationId, (id, offer) -> offer.rebuild()
                        .status(DataOfferStatus.FAILED)
                        .build());
                }
            })
            .ifSuccess(negotiationId -> offers.put(negotiationId, new DataOfferWithContextBuilder()
                .id(negotiationId)
                .timestamp(Instant.now())
                .drilled(offer.drilled())
                .milled(offer.milled())
                .quantity(offer.quantity())
                .pricePerUnit(offer.pricePerUnit())
                .status(DataOfferStatus.PENDING)
                .build()));
    }

    static class PendingCounterOffer {
        final TrustedContract contract;
        final TrustedContractNegotiatorResponder responder;

        PendingCounterOffer(final TrustedContract contract, final TrustedContractNegotiatorResponder responder) {
            this.contract = contract;
            this.responder = responder;
        }
    }

    static {
        try {
            final var bytes = Objects.requireNonNull(Main.class
                .getClassLoader()
                .getResourceAsStream("index.html"))
                .readAllBytes();

            HTML = new String(bytes, StandardCharsets.UTF_8);
        }
        catch (final Throwable exception) {
            throw new RuntimeException(exception);
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
