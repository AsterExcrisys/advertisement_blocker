package com.asterexcrisys.adblocker.services;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter {

    private final AtomicInteger counter;
    private final int start;
    private final int end;

    public AtomicCounter(int start, int end) {
        if (start >= end) {
            throw new IllegalArgumentException("start must be less than end");
        }
        counter = new AtomicInteger(start);
        this.start = start;
        this.end = end;
    }

    public int count() {
        return counter.updateAndGet((value) -> {
            if (value >= end) {
                return start;
            }
            return value + 1;
        });
    }

}