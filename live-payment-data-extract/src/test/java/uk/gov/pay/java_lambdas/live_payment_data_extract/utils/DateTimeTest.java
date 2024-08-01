package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.createIntervals;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.getDateString;

class DateTimeTest {

    private Clock clock;


    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-07-05T12:15:49.7891237Z"),
            ZoneId.of("UTC"));
    }

    @Test
    void shouldCreateIntervals() {
        var fiveMinutesAgo = Instant.now(clock).minusSeconds(300L);
        var now = Instant.now(clock);
        var result = createIntervals(fiveMinutesAgo, now, Duration.of(30L, ChronoUnit.SECONDS));

        assertEquals(10, result.size());
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plus(1L, ChronoUnit.MILLIS),
            Instant.parse(result.getFirst().left()));
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plusSeconds(30L),
            Instant.parse(result.getFirst().right()));
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plusSeconds(30L)
                .plus(1L, ChronoUnit.MILLIS),
            Instant.parse(result.get(1).left()));
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plusSeconds(30L)
                .plusSeconds(30L),
            Instant.parse(result.get(1).right()));
    }

    @Test
    void shouldHandleAlternativeDurationTypes() {
        var fiveMinutesAgo = Instant.now(clock).minusSeconds(300L);
        var now = Instant.now(clock);
        var result = createIntervals(fiveMinutesAgo, now, Duration.of(1L, ChronoUnit.MINUTES));

        assertEquals(5, result.size());
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plus(1L, ChronoUnit.MILLIS).toString(),
            result.getFirst().left());
        assertEquals(fiveMinutesAgo
                .truncatedTo(ChronoUnit.MILLIS)
                .plusSeconds(60L).toString(),
            result.getFirst().right());
    }

    @Test
    void shouldTruncateTimeToMillis() {
        var from = Instant.now(clock).minusSeconds(137L);
        var now = Instant.now(clock).minusMillis(30L).minusNanos(7589L);
        var result = createIntervals(from, now, Duration.of(30L, ChronoUnit.SECONDS));

        assertEquals(Instant.now(clock)
                .truncatedTo(ChronoUnit.MILLIS)
                .minusSeconds(137L)
                .plusMillis(1L),
            Instant.parse(result.getFirst().left()));
        assertEquals(Instant.now(clock)
                .truncatedTo(ChronoUnit.MILLIS)
                .minusMillis(30L),
            Instant.parse(result.getLast().right()));
    }

    @Test
    void shouldHandleOverlappingDays() {
        var from = Instant.now(clock)
            .plus(11L, ChronoUnit.HOURS)
            .plus(42L, ChronoUnit.MINUTES);
        var now = Instant.now(clock)
            .plus(11L, ChronoUnit.HOURS)
            .plus(47L, ChronoUnit.MINUTES);
        assertNotEquals(from.truncatedTo(ChronoUnit.DAYS), now.truncatedTo(ChronoUnit.DAYS));
        var result = createIntervals(from, now, Duration.of(30L, ChronoUnit.SECONDS));
        assertEquals(Instant.parse(result.getLast().right()).truncatedTo(ChronoUnit.DAYS),
            from.truncatedTo(ChronoUnit.DAYS));
    }
    
    @Test
    void shouldReturnDateString() {
        var result = getDateString(Instant.now(clock));
        assertEquals("2024-07-05", result);
    }
}
