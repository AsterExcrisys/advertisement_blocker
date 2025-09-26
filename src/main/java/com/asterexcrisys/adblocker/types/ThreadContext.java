package com.asterexcrisys.adblocker.types;

import com.asterexcrisys.adblocker.services.ProxyManager;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public record ThreadContext(ReentrantLock lock, ProxyManager manager) {

    public ThreadContext {
        Objects.requireNonNull(lock);
        Objects.requireNonNull(manager);
    }

    public void remove() {
        lock.unlock();
        manager.clearResolvers();
        manager.clearFilteredDomains();
    }

    public static ThreadContext of(ReentrantLock lock, ProxyManager manager) {
        return new ThreadContext(lock, manager);
    }

}