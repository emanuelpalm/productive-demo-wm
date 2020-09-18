package wm_demo.buyer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Back {
    private static final Logger logger = LoggerFactory.getLogger(Back.class);

    private final ArSystem system;
    private final ArTrustedContractNegotiatorPluginFacade facade;

    private final Map<Long, DataOfferDto> offers = new ConcurrentHashMap<>();
    private final Map<String, DataOrderDto> orders = new ConcurrentHashMap<>();

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
                .localHostnamePort("172.3.1.16", port)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress("172.3.1.12", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .buildAsync()
                .map(system -> {
                    final var facade = system.pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                        .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                        .orElseThrow(() -> new IllegalStateException("No " +
                            "HttpJsonTrustedContractNegotiatorPlugin is " +
                            "available; cannot negotiate"));

                    return new Back(system, facade);
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
            public DataOfferDto[] onGetOffers() {
                return offers.values().toArray(new DataOfferDto[0]);
            }

            @Override
            public DataOrderDto[] onGetOrders() {
                return orders.values().toArray(new DataOrderDto[0]);
            }

            @Override
            public Future<?> onOffer(DataOfferNewDto offer) {
                final var articleId = "ART-" +
                    (offer.drilled() ? "D" : "P") +
                    (offer.milled() ? "M" : "P");

                final var contract = new TrustedContractBuilder()
                    .templateName("component-order.txt")
                    .arguments(Map.of(
                        "article-id", articleId,
                        "quantity", "" + offer.quantity(),
                        "unit-price", "" + offer.pricePerUnit()
                    ))
                    .build();

                final AtomicLong id = new AtomicLong(-1);

                facade.offer("Buyer", "Seller", Duration.ofMinutes(3), List.of(contract),
                    new TrustedContractNegotiatorHandler() {
                        @Override
                        public void onSubmit(long negotiationId) {
                            id.set(negotiationId);
                            offers.put(negotiationId, new DataOfferBuilder()
                                .id(negotiationId)
                                .drilled(offer.drilled())
                                .milled(offer.milled())
                                .quantity(offer.quantity())
                                .pricePerUnit(offer.pricePerUnit())
                                .status(DataOffer.Status.PENDING)
                                .build());
                        }

                        @Override
                        public void onAccept(final TrustedContractNegotiationDto negotiation) {
                            offers.computeIfPresent(negotiation.id(), (id, offer) -> offer.rebuild()
                                .status(DataOffer.Status.ACCEPTED)
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
                            throw new UnsupportedOperationException(); // TODO: Implement.
                        }

                        @Override
                        public void onReject(final TrustedContractNegotiationDto negotiation) {
                            offers.computeIfPresent(negotiation.id(), (id, offer) -> offer.rebuild()
                                .status(DataOffer.Status.REJECTED)
                                .build());
                        }

                        @Override
                        public void onExpiry() {
                            offers.computeIfPresent(id.get(), (id, offer) -> offer.rebuild()
                                .status(DataOffer.Status.EXPIRED)
                                .build());
                        }

                        @Override
                        public void onFault(final Throwable throwable) {
                            logger.error("Failed to handle contract offer", throwable);
                            offers.computeIfPresent(id.get(), (id, offer) -> offer.rebuild()
                                .status(DataOffer.Status.FAILED)
                                .build());
                        }
                    });
                return Future.done();
            }
        };
    }
}
