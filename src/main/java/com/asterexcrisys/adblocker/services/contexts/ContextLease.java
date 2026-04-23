package com.asterexcrisys.adblocker.services.contexts;

import java.util.Objects;

public class ContextLease<T extends AutoCloseable> implements AutoCloseable {

    private final ContextPool<T> pool;
    private final T instance;

    public ContextLease(ContextPool<T> pool, T instance) {
        this.pool = Objects.requireNonNull(pool);
        this.instance = Objects.requireNonNull(instance);
    }

    public T get() {
        return instance;
    }

    @Override
    public void close() {
        pool.release(instance);
    }

}