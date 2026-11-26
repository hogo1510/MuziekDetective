package com.lucasfenstra.muziekdetective.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    /** Klok die we handmatig vooruit zetten. */
    static class FakeClock extends Clock {
        private Instant now = Instant.parse("2026-11-26T12:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final FakeClock clock = new FakeClock();
    private final RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(10), clock);

    @Test
    void staatMaxVerzoekenToeEnWeigertDaarna() {
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }

    @Test
    void sleutelsWordenApartGeteld() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("a");
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void naHetVensterMagHetWeer() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("a");
        clock.advance(Duration.ofMinutes(9));
        assertThat(limiter.tryAcquire("a")).isFalse();
        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void geweigerdVerzoekTeltNietMee() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("a");
        clock.advance(Duration.ofMinutes(5));
        limiter.tryAcquire("a"); // geweigerd
        clock.advance(Duration.ofMinutes(5));
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void cleanupVerwijdertVerlopenSleutels() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("b");
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        clock.advance(Duration.ofMinutes(10));
        limiter.cleanup();
        assertThat(limiter.trackedKeys()).isZero();
    }

    @Test
    void ruimtAutomatischOpNaEenVenster() {
        for (int i = 0; i < 100; i++) limiter.tryAcquire("ip-" + i);
        assertThat(limiter.trackedKeys()).isEqualTo(100);

        clock.advance(Duration.ofMinutes(10));
        limiter.tryAcquire("nieuw");
        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }
}
