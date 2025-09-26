package com.asterexcrisys.adblocker.utility;

import com.asterexcrisys.adblocker.GlobalSettings;
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

    public static <T> T acquireAccess(ReentrantLock lock, Supplier<T> supplier) {
        lock.lock();
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

    public static <T> List<T> fillList(Supplier<T> supplier, int size) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(supplier.get());
        }
        return list;
    }

}