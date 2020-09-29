package wm_demo.seller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import wm_demo.common.DataOffer;
import wm_demo.common.Config;

import java.net.InetSocketAddress;

public class Seller {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);
    private static final PricingModel pricingModel = new PricingModel.Builder()
        .quantityMin(1)
        .quantityMax(3)
        .minUnitPrice(100.0)
        .drilledPriceMultiplier(1.05)
        .milledPriceMultiplier(1.15)
        .quantityPriceMultiplier(0.99)
        .build();

    private Seller() {}

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Seller System");
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.seller.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort(Config.SELLER_HOSTNAME, Config.SELLER_PORT)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress(Config.SR_HOSTNAME, Config.SR_PORT)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

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
                    }

                    @Override
                    public void onOffer(final TrustedContractNegotiationDto negotiation, final TrustedContractNegotiatorResponder responder) {
                        try {
                            final var offer = DataOffer.fromContract(negotiation.offer().contracts().get(0));
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
        }
        catch (final Throwable throwable) {
            logger.error("Seller system start-up failed", throwable);
        }
    }
}
