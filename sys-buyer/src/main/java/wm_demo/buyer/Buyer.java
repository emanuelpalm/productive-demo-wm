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
import static se.arkalix.security.access.AccessPolicy.cloud;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class Buyer {
    private static final Logger logger = LoggerFactory.getLogger(Buyer.class);
    private static final String HTML;

    private static final Map<String, DataOfferWithContextDto> offers = new ConcurrentHashMap<>();
    private static final Map<String, DataOrderSummaryDto> orderSummaries = new ConcurrentHashMap<>();
    private static final Map<String, PendingCounterOffer> pendingCounterOffers = new ConcurrentHashMap<>();

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Buyer System");
        try {
            final var prop = JaimeProperties.getProp();

            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort(
                    prop.getProperty("server.address", Config.BUYER_HOSTNAME),
                    prop.getIntProperty("server.port", Config.BUYER_PORT))
                .plugins(
                    new HttpJsonCloudPlugin.Builder()
                        .serviceRegistrationPredicate(service -> service.interfaces()
                            .stream()
                            .allMatch(i -> i.encoding().isDto()))
                        .serviceRegistrySocketAddress(new InetSocketAddress(
                            prop.getProperty("sr_hostname", Config.SR_HOSTNAME),
                            prop.getIntProperty("sr_port", Config.SR_PORT)))
                        .build(),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            final var facade = system
                .pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                .orElseThrow(() -> new IllegalStateException("No " +
                    "HttpJsonTrustedContractNegotiatorPlugin is " +
                    "available; cannot negotiate"));

            system.provide(new HttpService()
                .name("buyer-operation")
                .encodings(JSON, EncodingDescriptor.getOrCreate("HTML"))
                .accessPolicy(cloud())
                .basePath("/")

                .get("/", (request, response) -> {
                    response
                        .status(OK)
                        .header("content-type", "text/html")
                        .header("cache-control", "no-store")
                        .body(HTML);
                    return done();
                })

                .get("/order-summaries", (request, response) -> {
                    response
                        .status(OK)
                        .body(new ArrayList<>(orderSummaries.values()));
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
                .name("buyer")
                .encodings(JSON)
                .accessPolicy(token())
                .basePath("/buyer")

                .post("/orders", (request, response) -> request
                    .bodyAs(DataOrderSummaryDto.class)
                    .ifSuccess(order -> {
                        final var isCreated = orderSummaries.put(order.articleId(), order) == null;
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

    private static Future<?> handleOfferUsing(
        final DataOffer offer,
        final ArTrustedContractNegotiatorPluginFacade facade
    ) {
        final var contract = offer.toContract();

        // Handle counter-offer replies.
        if (offer.id().isPresent()) {
            final var id = offer.id().get();
            final var counterOffer = pendingCounterOffers.remove(id);
            if (counterOffer == null) {
                return Future.failure(new IllegalStateException("No pending counter-offer with ID " + id + " exists"));
            }
            if (contract.equals(counterOffer.contract)) {
                onAccept(id, offer);
                return counterOffer.responder.accept();
            }
            onCounterOffer(id, offer);
            return counterOffer.responder.offer(new SimplifiedContractCounterOffer.Builder()
                .contracts(contract)
                .validFor(Duration.ofMinutes(3))
                .build());
        }

        return facade.offer("Buyer", "Seller", Duration.ofMinutes(3), List.of(contract),
            new TrustedContractNegotiatorHandler() {
                @Override
                public void onAccept(final TrustedContractNegotiationDto negotiation) {
                    final var contracts = negotiation.offer().contracts();
                    if (contracts.size() != 1) {
                        throw new IllegalArgumentException("Negotiation offer must contain exactly one contract");
                    }
                    final var contract = negotiation.offer().contracts().get(0);
                    final var id = Long.toString(negotiation.id());
                    Buyer.onAccept(id, DataOffer.fromContract(contract));
                }

                @Override
                public void onOffer(
                    final TrustedContractNegotiationDto negotiation,
                    final TrustedContractNegotiatorResponder responder
                ) {
                    final var contracts = negotiation.offer().contracts();
                    if (contracts.size() != 1) {
                        throw new IllegalArgumentException("Negotiation offer must contain exactly one contract");
                    }
                    final var contract = negotiation.offer().contracts().get(0);
                    final var id = Long.toString(negotiation.id());
                    pendingCounterOffers.put(id, new PendingCounterOffer(contract, responder));
                    Buyer.onOffer(id, DataOffer.fromContract(contract));
                }

                @Override
                public void onReject(final TrustedContractNegotiationDto negotiation) {
                    Buyer.onReject(Long.toString(negotiation.id()));
                }

                @Override
                public void onExpiry(final long negotiationId) {
                    Buyer.onExpiry(Long.toString(negotiationId));
                }

                @Override
                public void onFault(final long negotiationId, final Throwable throwable) {
                    Buyer.onFault(Long.toString(negotiationId), throwable);
                }
            })
            .ifSuccess(negotiationId -> offers.put(negotiationId.toString(), new DataOfferWithContextBuilder()
                .id(negotiationId.toString())
                .timestamp(Instant.now())
                .drilled(offer.drilled())
                .milled(offer.milled())
                .quantity(offer.quantity())
                .pricePerUnit(offer.pricePerUnit())
                .status(DataOfferStatus.PENDING)
                .build()));
    }

    private static void onAccept(final String id, final DataOffer offer) {
        offers.computeIfPresent(id, (id0, offer0) -> offer0.rebuild()
            .drilled(offer.drilled())
            .milled(offer.milled())
            .quantity(offer.quantity())
            .pricePerUnit(offer.pricePerUnit())
            .status(DataOfferStatus.ACCEPTED)
            .build());
        orderSummaries.compute(offer.articleId(), (articleId, order) -> {
            if (order == null) {
                return new DataOrderSummaryBuilder()
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

    private static void onCounterOffer(final String id, DataOffer offer) {
        offers.computeIfPresent(id, (id0, offer0) -> offer0.rebuild()
            .drilled(offer.drilled())
            .milled(offer.milled())
            .quantity(offer.quantity())
            .pricePerUnit(offer.pricePerUnit())
            .status(DataOfferStatus.PENDING)
            .build());
    }

    private static void onOffer(final String id, final DataOfferDto offer) {
        offers.computeIfPresent(id, (id0, offer0) -> offer0.rebuild()
            .status(DataOfferStatus.COUNTERED)
            .counterOffer(offer)
            .build());
    }

    private static void onReject(final String id) {
        offers.computeIfPresent(id, (id0, offer) -> offer.rebuild()
            .status(DataOfferStatus.REJECTED)
            .build());
    }

    private static void onExpiry(final String id) {
        offers.computeIfPresent(id, (id0, offer) -> {
            final var status = offer.status();
            if (status != DataOfferStatus.ACCEPTED && status != DataOfferStatus.REJECTED) {
                offer = offer.rebuild()
                    .status(DataOfferStatus.EXPIRED)
                    .build();
            }
            return offer;
        });
    }

    private static void onFault(final String id, final Throwable throwable) {
        logger.error("Failed to handle contract offer", throwable);
        offers.computeIfPresent(id, (id0, offer) -> offer.rebuild()
            .status(DataOfferStatus.FAILED)
            .build());
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
            final var bytes = Objects.requireNonNull(Buyer.class
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
        final var logLevel = Level.ALL;
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        final var root = java.util.logging.Logger.getLogger("");
        root.setLevel(logLevel);
        for (final var handler : root.getHandlers()) {
            handler.setLevel(logLevel);
        }
    }
}
