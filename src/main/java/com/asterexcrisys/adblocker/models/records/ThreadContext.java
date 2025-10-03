package com.asterexcrisys.adblocker.models.records;

import com.asterexcrisys.adblocker.services.ProxyManager;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public record ThreadContext(ReentrantLock lock, ProxyManager manager) {

    public ThreadContext {
        Objects.requireNonNull(lock);
        Objects.requireNonNull(manager);
    }

    public void clear() throws Exception {
        lock.unlock();
        manager.clearResolvers();
        manager.clearFilteredDomains();
        manager.close();
    }

    public static ThreadContext of(ReentrantLock lock, ProxyManager manager) {
        return new ThreadContext(lock, manager);
    }

}