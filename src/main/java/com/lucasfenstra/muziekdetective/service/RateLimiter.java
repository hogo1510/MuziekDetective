package com.lucasfenstra.muziekdetective.service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding-window rate limiter per sleutel (IP-adres). Verlopen sleutels worden
 * eens per venster opgeruimd, zodat de map niet blijft groeien met elk nieuw IP.
 */
public class RateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup;

    public RateLimiter(int maxRequests, Duration window) {
        this(maxRequests, window, Clock.systemUTC());
    }

    public RateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.windowMillis = window.toMillis();
        this.clock = clock;
        this.lastCleanup = new AtomicLong(clock.millis());
    }

    public boolean tryAcquire(String key) {
        long now = clock.millis();
        cleanupIfDue(now);

        boolean[] allowed = {false};
        // compute() is atomair per sleutel, dus geen race met cleanup().
        requestTimestamps.compute(key, (k, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayDeque<>();
            }
            evictExpired(timestamps, now);
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                allowed[0] = true;
            }
            return timestamps;
        });
        return allowed[0];
    }

    /** Verwijdert verlopen tijdstempels en sleutels zonder recente verzoeken. */
    public void cleanup() {
        long now = clock.millis();
        for (String key : requestTimestamps.keySet()) {
            requestTimestamps.computeIfPresent(key, (k, timestamps) -> {
                evictExpired(timestamps, now);
                return timestamps.isEmpty() ? null : timestamps;
            });
        }
    }

    int trackedKeys() {
        return requestTimestamps.size();
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }

    private void cleanupIfDue(long now) {
        long last = lastCleanup.get();
        if (now - last >= windowMillis && lastCleanup.compareAndSet(last, now)) {
            cleanup();
        }
    }

    private void evictExpired(Deque<Long> timestamps, long now) {
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
            timestamps.pollFirst();
        }
    }
}
