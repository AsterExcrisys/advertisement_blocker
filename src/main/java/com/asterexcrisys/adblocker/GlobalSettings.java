package com.asterexcrisys.adblocker;

import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
public final class GlobalSettings {

    private static volatile GlobalSettings INSTANCE;

    private final AtomicLong requestTimeout;

    private GlobalSettings() {
        requestTimeout = new AtomicLong(5000);
    }

    public long getRequestTimeout() {
        return requestTimeout.get();
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout.set(requestTimeout);
    }

    public static GlobalSettings getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (GlobalSettings.class) {
            if (INSTANCE == null) {
                INSTANCE = new GlobalSettings();
            }
        }
        return INSTANCE;
    }

    public static void clearInstance() {
        synchronized (GlobalSettings.class) {
            if (INSTANCE != null) {
                INSTANCE = null;
            }
        }
    }

}