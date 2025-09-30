package com.asterexcrisys.adblocker.utility;

import com.asterexcrisys.adblocker.GlobalSettings;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.resolvers.STDResolver;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class GlobalUtility {

    private static final GlobalSettings SETTINGS;

    static {
        SETTINGS = GlobalSettings.getInstance();
    }

    public static <T> List<T> fillList(Supplier<T> supplier, int size) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(supplier.get());
        }
        return list;
    }

    public static <T> T acquireAccess(ReentrantLock lock, Supplier<T> supplier) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public static <T> Optional<T> acquireAccessOrTimeout(ReentrantLock lock, Supplier<T> supplier) throws InterruptedException {
        if (!lock.tryLock(SETTINGS.getRequestTimeout(), TimeUnit.MILLISECONDS)) {
            return Optional.empty();
        }
        try {
            return Optional.of(supplier.get());
        } finally {
            lock.unlock();
        }
    }

    public static String resolveDomainAddress(String domain, boolean preferIPv4) throws Exception {
        Name name = Name.fromString(domain, Name.root);
        Record question = Record.newRecord(name, preferIPv4? Type.A:Type.AAAA, DClass.IN);
        Message request = Message.newQuery(question);
        try (Resolver resolver = new STDResolver("1.1.1.1")) {
            Message response = resolver.resolve(request);
            if (!ResolverUtility.validateResponse(response)) {
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