package com.asterexcrisys.adblocker.utilities;

import com.asterexcrisys.adblocker.GlobalSettings;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.resolvers.STDResolver;
import com.asterexcrisys.adblocker.services.contexts.ContextLease;
import com.asterexcrisys.adblocker.services.contexts.ContextPool;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class GlobalUtilities {

    private static final GlobalSettings SETTINGS = GlobalSettings.getInstance();

    public static <T> List<T> fillList(Supplier<T> supplier, int size) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(supplier.get());
        }
        return list;
    }

    public static <T> T switchReturn(boolean condition, Supplier<T> trueSupplier, Supplier<T> falseSupplier) {
        if (condition) {
            return trueSupplier.get();
        }
        return falseSupplier.get();
    }

    public static <T> T tryOrDefault(Callable<T> callable, T defaultValue) {
        try {
            return callable.call();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static <T extends AutoCloseable, R> R acquireAccess(ContextPool<T> pool, Function<T, R> function) throws InterruptedException {
        try (ContextLease<T> lease = pool.acquire()) {
            return function.apply(lease.get());
        }
    }

    public static <T extends AutoCloseable, R> Optional<R> acquireAccessOrTimeout(ContextPool<T> pool, Function<T, R> function) throws InterruptedException {
        Optional<ContextLease<T>> optional = pool.tryAcquire(SETTINGS.getRequestTimeout(), TimeUnit.MILLISECONDS);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        try (ContextLease<T> lease = optional.get()) {
            return Optional.of(function.apply(lease.get()));
        }
    }

    public static String resolveDomainAddress(String domain, boolean preferIPv4) throws Exception {
        Name name = Name.fromString(domain, Name.root);
        Record question = Record.newRecord(name, preferIPv4? Type.A:Type.AAAA, DClass.IN);
        Message request = Message.newQuery(question);
        try (Resolver resolver = new STDResolver("1.1.1.2")) {
            Message response = resolver.resolve(request);
            if (!ResolverUtilities.validateResponse(response)) {
                throw new IllegalArgumentException("response must have a header, question, and answer(s) field to be considered valid");
            }
            long maximumTTL = -1;
            String address = null;
            for (Record answer : response.getSection(Section.ANSWER)) {
                if (answer.getTTL() > maximumTTL && answer.getName() != null) {
                    address = answer.getName().toString(true);
                    maximumTTL = answer.getTTL();
                }
            }
            if (address == null) {
                throw new IllegalArgumentException("no suitable address for domain '%s' was found in the response".formatted(domain));
            }
            return address;
        }
    }

    public static CircuitBreaker buildCircuitBreaker(int windowSize, int minimumCalls, int maximumCalls, float rateThreshold, Duration timeoutDuration, Duration recoveryDuration) {
        CircuitBreakerConfig configuration = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(windowSize)
                .minimumNumberOfCalls(minimumCalls)
                .permittedNumberOfCallsInHalfOpenState(maximumCalls)
                .failureRateThreshold(rateThreshold)
                .waitDurationInOpenState(timeoutDuration)
                .maxWaitDurationInHalfOpenState(recoveryDuration)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .recordResult((result) -> {
                    if (result instanceof Message message) {
                        return message.getHeader().getRcode() == Rcode.SERVFAIL;
                    }
                    return false;
                }).build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(configuration);
        return registry.circuitBreaker("resolver_circuit_breaker");
    }

}