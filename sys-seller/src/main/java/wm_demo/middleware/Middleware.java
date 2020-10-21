package wm_demo.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.ArTrustedContractObserverPluginFacade;
import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.HttpJsonTrustedContractObserverPlugin;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.HttpIncomingResponse;
import se.arkalix.net.http.client.HttpClientResponse;
import se.arkalix.net.http.consumer.HttpConsumer;
import se.arkalix.net.http.consumer.HttpConsumerRequest;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import wm_demo.common.Config;
import wm_demo.common.DataOrderBuilder;
import wm_demo.common.DataOrderSummaryBuilder;
import wm_demo.common.JaimeProperties;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpMethod.POST;
import static se.arkalix.net.http.HttpStatus.NOT_FOUND;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.cloud;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class Middleware {
    private static final Logger logger = LoggerFactory.getLogger(Middleware.class);
    private static final String HTML;

    private Middleware() { }

    public static void main(final String[] args) {
        logger.info("Productive 4.0 Workflow Manager Demonstrator - Middleware System");
        try {
            final var prop = JaimeProperties.getProp();

            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.middleware.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort(
                    prop.getProperty("server.address", Config.MIDDLEWARE_HOSTNAME),
                    prop.getIntProperty("server.port", Config.MIDDLEWARE_PORT))
                .plugins(
                    new HttpJsonCloudPlugin.Builder()
                        .serviceRegistrationPredicate(service -> service.interfaces()
                            .stream()
                            .allMatch(i -> i.encoding().isDto()))
                        .serviceRegistrySocketAddress(new InetSocketAddress(
                            prop.getProperty("sr_hostname", Config.SR_HOSTNAME),
                            prop.getIntProperty("sr_port", Config.SR_PORT)))
                        .build(),
                    new HttpJsonTrustedContractObserverPlugin())
                .build();

            logger.info("Setup middleware system; starting to provide service ...");

            final var articleIdToSerialIds = new ConcurrentHashMap<String, Set<Long>>();
            final var serialIdToArticleId = new ConcurrentHashMap<Long, String>();
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
                    final var newSerialIds = LongStream.range(0, quantity)
                        .map(ignored -> nextSerialId.getAndIncrement())
                        .boxed()
                        .collect(Collectors.toUnmodifiableList());
                    articleIdToSerialIds.compute(articleId, (articleId0, serialIds) -> {
                        if (serialIds == null) {
                            serialIds = new CopyOnWriteArraySet<>(newSerialIds);
                        }
                        else {
                            serialIds.addAll(newSerialIds);
                        }
                        return serialIds;
                    });
                    for (final var newSerialId : newSerialIds) {
                        serialIdToArticleId.put(newSerialId, articleId);
                    }
                    logger.info("Increased the order count of `{}` by `{}`", articleId, quantity);
                });

            system.provide(new HttpService()
                .name("middleware")
                .encodings(JSON)
                .basePath("/middleware")
                .accessPolicy(token())

                .get("/orders/#serialId", (request, response) -> {
                    final var serialId = Long.parseLong(request.pathParameter(0));
                    final var articleId = serialIdToArticleId.get(serialId);
                    if (articleId != null) {
                        response
                            .status(OK)
                            .body(new DataOrderBuilder()
                                .serialId(serialId)
                                .articleId(articleId)
                                .build());

                        if (articleIdToSerialIds.get(articleId).remove(serialId)) {
                            return system.consume()
                                .name("buyer")
                                .encodings(JSON)
                                .oneUsing(HttpConsumer.factory())
                                .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                                    .method(POST)
                                    .uri("/order-summaries")
                                    .body(articleIdToSerialIds.entrySet()
                                        .stream()
                                        .map(entry -> new DataOrderSummaryBuilder()
                                            .articleId(entry.getKey())
                                            .quantity(entry.getValue().size())
                                            .build())
                                        .collect(Collectors.toUnmodifiableList()))))
                                .flatMap(HttpIncomingResponse::rejectIfNotSuccess)
                                .ifFailure(Throwable.class, throwable ->
                                    logger.warn("Failed to send update order summaries to buyer system", throwable));
                        }
                    }
                    else {
                        response.status(NOT_FOUND);
                    }
                    return done();
                }))

                .ifSuccess(handle -> logger.info("Middleware back-end service is now being served"))
                .ifFailure(Throwable.class, throwable -> logger.error("Failed to serve middleware back-end service", throwable))
                .await();

            system.provide(new HttpService()
                .name("middleware-operation")
                .encodings(JSON, EncodingDescriptor.getOrCreate("HTML"))
                .basePath("/")
                .accessPolicy(cloud())

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
                        .body(articleIdToSerialIds.entrySet()
                            .stream()
                            .flatMap(entry -> entry.getValue()
                                .stream()
                                .map(serialId -> new DataOrderBuilder()
                                    .serialId(serialId)
                                    .articleId(entry.getKey())
                                    .build()))
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
