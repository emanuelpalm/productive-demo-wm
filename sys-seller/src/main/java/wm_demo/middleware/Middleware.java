package wm_demo.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.ArTrustedContractObserverPluginFacade;
import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.HttpJsonTrustedContractObserverPlugin;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import wm_demo.shared.Global;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class Middleware {
    private static final Logger logger = LoggerFactory.getLogger(Middleware.class);
    private static final String HTML;

    static {
        try {
            final var bytes = Objects.requireNonNull(Middleware.class
                .getClassLoader()
                .getResourceAsStream("middleware.html"))
                .readAllBytes();

            HTML = new String(bytes, StandardCharsets.UTF_8);
        }
        catch (final Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private final ArSystem system;
    private final Map<String, Integer> articleIdQuantityMap = new ConcurrentHashMap<>();

    private Middleware(final ArSystem system) {
        this.system = system;
    }

    public static Future<Middleware> createAndBindTo(final int port) {
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            return new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath(Global.KEYSTORE_FACTORY)
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read(Global.TRUSTSTORE, password))
                .localHostnamePort(Global.HOSTNAME_LOCAL, port)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(
                    new InetSocketAddress(Global.HOSTNAME_SR, Global.PORT_SR)),
                    new HttpJsonTrustedContractObserverPlugin())
                .buildAsync()
                .flatMap(system -> {
                    logger.info("Setup factory system; starting to provide service ...");

                    final var middleware = new Middleware(system);
                    final var seenOrderIds = new ConcurrentHashMap<Long, Boolean>();

                    system.pluginFacadeOf(HttpJsonTrustedContractObserverPlugin.class)
                        .map(f -> (ArTrustedContractObserverPluginFacade) f)
                        .orElseThrow(() -> new IllegalStateException("No " +
                            "HttpJsonTrustedContractObserverPlugin is " +
                            "available; cannot observe negotiations"))
                        .observe(session -> {
                            if (session.status() != ContractNegotiationStatus.ACCEPTED) {
                                return;
                            }
                            final var contracts = session.offer().contracts();
                            if (contracts.size() != 1) {
                                return;
                            }
                            final var contract = contracts.get(0);
                            if (!contract.templateName().equals("component-order.txt")) {
                                return;
                            }
                            if (seenOrderIds.putIfAbsent(session.id(), true) != null) {
                                return;
                            }
                            final var articleId = contract.arguments().get("articleId");
                            if (articleId == null) {
                                logger.warn("No `articleId` in `component-order.txt` contract; ignoring");
                                return;
                            }
                            final var quantityStr = contract.arguments().get("quantity");
                            if (quantityStr == null) {
                                logger.warn("No `quantity` in `component-order.txt` contract; ignoring");
                                return;
                            }
                            final int quantity;
                            try {
                                quantity = Integer.parseInt(quantityStr);
                            }
                            catch (final NumberFormatException exception) {
                                logger.warn("Invalid `quantity` in `component-order.txt` contract; ignoring", exception);
                                return;
                            }
                            middleware.articleIdQuantityMap.compute(articleId, (id, count) -> {
                                if (count == null) {
                                    count = 0;
                                }
                                return count + quantity;
                            });
                            logger.info("Increased the order count of `{}` by `{}`", articleId, quantity);
                        });

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
                        }))

                        .ifSuccess(handle -> logger.info("Factory service is now being served"))
                        .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve factory service", throwable))

                        .pass(middleware);
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
