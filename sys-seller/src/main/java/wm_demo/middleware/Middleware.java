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
import wm_demo.common.DataOrderBuilder;
import wm_demo.shared.Global;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.whitelist;
import static se.arkalix.util.concurrent.Future.done;

public class Middleware {
    private static final Logger logger = LoggerFactory.getLogger(Middleware.class);
    private static final String HTML;

    private Middleware() { }

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Middleware System");
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath(Global.MIDDLEWARE_KEYSTORE)
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read(Global.TRUSTSTORE, password))
                .localHostnamePort(Global.MIDDLEWARE_HOSTNAME, Global.MIDDLEWARE_PORT)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(
                    new InetSocketAddress(Global.SR_HOSTNAME, Global.SR_PORT)),
                    new HttpJsonTrustedContractObserverPlugin())
                .build();

            logger.info("Setup middleware system; starting to provide service ...");

            final var articleIdQuantityMap = new ConcurrentHashMap<String, Integer>();
            final var serialIdArticleIdMap = new ConcurrentHashMap<Long, String>();
            final var seenSessionIds = new ConcurrentHashMap<Long, Boolean>();
            final var nextSerialId = new AtomicLong(1000);

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
                    if (seenSessionIds.putIfAbsent(session.id(), true) != null) {
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
                    articleIdQuantityMap.compute(articleId, (id, count) -> {
                        if (count == null) {
                            count = 0;
                        }
                        return count + quantity;
                    });
                    for (var i = quantity; i-- != 0; ) {
                        serialIdArticleIdMap.put(nextSerialId.getAndIncrement(), articleId);
                    }
                    logger.info("Increased the order count of `{}` by `{}`", articleId, quantity);
                });

            system.provide(new HttpService()
                .name("middleware-back-end")
                .encodings(JSON)
                .basePath("/back")
                .accessPolicy(token())

                .get("/order/:serialId", (request, response) -> {
                    response // TODO: Implement!
                        .status(OK);
                    return done();
                }))

                .ifSuccess(handle -> logger.info("Middleware back-end service is now being served"))
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve middleware back-end service", throwable))
                .await();

            system.provide(new HttpService()
                .name("middleware-front-end")
                .encodings(JSON, EncodingDescriptor.getOrCreate("HTML"))
                .basePath("/")
                .accessPolicy(whitelist("middleware_operator"))

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
                        .body(articleIdQuantityMap.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(entry -> new DataOrderBuilder()
                                .articleId(entry.getKey())
                                .quantity(entry.getValue())
                                .build())
                            .collect(Collectors.toUnmodifiableList()));
                    return done();
                }))

                .ifSuccess(handle -> logger.info("Middleware front-end service is now being served"))
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve middleware front-end service", throwable))
                .await();
        }
        catch (final Throwable throwable) {
            logger.error("Middleware system start-up failed", throwable);
        }
    }

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
}
