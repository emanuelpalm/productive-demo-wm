package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import wm_demo.common.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.CREATED;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;

public class Back {
    private static final Logger logger = LoggerFactory.getLogger(Back.class);

    private final ArSystem system;
    private final ArTrustedContractNegotiatorPluginFacade facade;

    private final Map<Long, DataOfferWithContextDto> offers = new ConcurrentHashMap<>();
    private final Map<String, DataOrderDto> orders = new ConcurrentHashMap<>();
    private final Map<Long, CounterOffer> pendingCounterOffers = new ConcurrentHashMap<>();

    private Back(final ArSystem system, final ArTrustedContractNegotiatorPluginFacade facade) {
        this.system = system;
        this.facade = facade;
    }

    public static Future<Back> createAndBindTo(final int port) {
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            return new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort("sys-buyer.uni", port)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress("service-registry.uni", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .buildAsync()
                .flatMap(system -> {
                    final var facade = system
                        .pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                        .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                        .orElseThrow(() -> new IllegalStateException("No " +
                            "HttpJsonTrustedContractNegotiatorPlugin is " +
                            "available; cannot negotiate"));

                    final var back = new Back(system, facade);

                    return system.provide(new HttpService()
                        .name("buyer-back-end")
                        .encodings(JSON)
                        .accessPolicy(token())
                        .basePath("/")

                        .post("/orders", (request, response) -> request
                            .bodyAs(DataOrderDto.class)
                            .ifSuccess(order -> {
                                final var isCreated = back.orders.put(order.articleId(), order) == null;
                                response.status(isCreated ? CREATED : OK);
                            })))

                        .pass(back);
                });
        }
        catch (final Throwable throwable) {
            return Future.failure(throwable);
        }
    }

    public int port() {
        return system.localPort();
    }

    public Front.RequestHandler handler() {
        return new Front.RequestHandler() {
            @Override
            public DataOfferWithContextDto[] onGetOffers() {
                return offers.values().toArray(new DataOfferWithContextDto[0]);
            }

            @Override
            public DataOrderDto[] onGetOrders() {
                return orders.values().toArray(new DataOrderDto[0]);
            }

            @Override
            public Future<?> onOffer(final DataOfferDto offer) {
                final var articleId = "ART-" +
                    (offer.drilled() ? "D" : "P") +
                    (offer.milled() ? "M" : "P");

                final var contract = new TrustedContractBuilder()
                    .templateName("component-order.txt")
                    .arguments(Map.of(
                        "articleId", articleId,
                        "quantity", "" + offer.quantity(),
                        "unitPrice", "" + offer.pricePerUnit()
                    ))
                    .build();

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
                            orders.compute(articleId, (articleId, order) -> {
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
                            pendingCounterOffers.put(negotiation.id(), new CounterOffer(contract, responder));
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
        };
    }

    static class CounterOffer {
        final TrustedContract contract;
        final TrustedContractNegotiatorResponder responder;

        CounterOffer(final TrustedContract contract, final TrustedContractNegotiatorResponder responder) {
            this.contract = contract;
            this.responder = responder;
        }
    }
}
