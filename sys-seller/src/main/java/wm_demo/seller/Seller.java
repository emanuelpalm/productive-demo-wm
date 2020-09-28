package wm_demo.seller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import wm_demo.common.DataOffer;
import wm_demo.shared.Global;

import java.net.InetSocketAddress;

public class Seller {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);

    private final ArSystem system;

    private Seller(final ArSystem system) {
        this.system = system;
    }

    public static Future<Seller> createBindToPortAndUse(final int port, final PricingModel pricingModel) {
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            return new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath(Global.KEYSTORE_SELLER)
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read(Global.TRUSTSTORE, password))
                .localHostnamePort(Global.HOSTNAME_LOCAL, port)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress(Global.HOSTNAME_SR, Global.PORT_SR)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .buildAsync()
                .map(system -> {
                    system
                        .pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                        .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                        .orElseThrow(() -> new IllegalStateException("No " +
                            "HttpJsonTrustedContractNegotiatorPlugin is " +
                            "available; cannot negotiate"))
                        .listen("Seller", () -> new TrustedContractNegotiatorHandler() {
                            @Override
                            public void onAccept(final TrustedContractNegotiationDto negotiation) {
                                logger.info("Counter-offer accepted " + negotiation.offer());
                                // TODO: Notify factory.
                            }

                            @Override
                            public void onOffer(final TrustedContractNegotiationDto negotiation, final TrustedContractNegotiatorResponder responder) {
                                try {
                                    final var offer= DataOffer.fromContract(negotiation.offer().contracts().get(0));
                                    final var counterOffer = pricingModel.getCounterOfferIfNotAcceptable(offer);
                                    if (counterOffer.isPresent()) {
                                        responder
                                            .offer(counterOffer.get().toSimplifiedContractCounterOffer())
                                            .onFailure(throwable -> logger.error("Failed to make counter-offer", throwable));
                                    }
                                    else {
                                        responder
                                            .accept()
                                            .onFailure(throwable -> logger.error("Failed to accept offer", throwable));
                                        // TODO: Notify factory.
                                    }
                                }
                                catch (final IllegalArgumentException exception) {
                                    logger.warn("Rejecting bad offer " + negotiation.offer(), exception);
                                    responder
                                        .reject()
                                        .onFailure(throwable -> logger.error("Failed to reject offer", throwable));
                                }
                            }

                            @Override
                            public void onReject(final TrustedContractNegotiationDto negotiation) {
                                logger.info("Counter-offer rejected " + negotiation.offer());
                            }
                        });
                    return new Seller(system);
                });
        }
        catch (final Throwable throwable) {
            return Future.failure(throwable);
        }
    }

    public int port() {
        return system.localPort();
    }
}
