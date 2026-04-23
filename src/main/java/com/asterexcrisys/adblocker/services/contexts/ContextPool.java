package com.asterexcrisys.adblocker.services.contexts;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ContextPool<T extends AutoCloseable> implements AutoCloseable {

    private final BlockingQueue<T> pool;

    public ContextPool(Collection<T> instances) {
        Objects.requireNonNull(instances);
        pool = new ArrayBlockingQueue<>(instances.size(), true, instances);
    }

    public ContextLease<T> acquire() throws InterruptedException {
        T instance = pool.take();
        return new ContextLease<>(this, instance);
    }

    public Optional<ContextLease<T>> tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        T instance = pool.poll(timeout, unit);
        if (instance == null) {
            return Optional.empty();
        }
        return Optional.of(new ContextLease<>(this, instance));
    }

    public void release(T instance) {
        pool.offer(instance);
    }

    @Override
    public void close() throws Exception {
        List<T> instances = new ArrayList<>(pool.size());
        pool.drainTo(instances);
        for (T instance : instances) {
            instance.close();
        }
    }

}