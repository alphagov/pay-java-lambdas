package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.Pair;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


public class DateTime {

    private static final Logger logger = LoggerFactory.getLogger(DateTime.class);

    private DateTime() {
    }

    public static List<Pair<String, String>> createIntervals(Instant start, Instant end, Duration interval) {
        List<Pair<String, String>> pairIntervals = new ArrayList<>();
        Instant startFrom = setMillisResolution(start).plus(1L, ChronoUnit.MILLIS);
        Instant endAt = setMillisResolution(end);
        if (!startFrom.truncatedTo(ChronoUnit.DAYS).equals(endAt.truncatedTo(ChronoUnit.DAYS))) {
            endAt = getEndOfDayFromInstant(startFrom);
            logger.debug("[{}] crossed date boundary, setting to {}", end, endAt);
        }
        logger.debug("Generating intervals from {} to {}", startFrom, endAt);
        while (startFrom.isBefore(endAt)) {
            var nextInterval = startFrom.plus(interval).minus(1L, ChronoUnit.MILLIS);
            if (nextInterval.isAfter(endAt)) {
                pairIntervals.add(Pair.of(startFrom.toString(), endAt.toString()));
            } else {
                pairIntervals.add(Pair.of(startFrom.toString(), nextInterval.toString()));
            }
            startFrom = startFrom.plus(interval);
        }
        logger.debug("Generated {} intervals", pairIntervals.size());
        return pairIntervals;
    }

    public static String getDateString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }
    
    //-- PRIVATE METHODS
    
    private static Instant setMillisResolution(Instant time) {
        var truncTime = Instant.ofEpochMilli(time.toEpochMilli());
        logger.debug("[{}] truncated to millis {}", time, truncTime);
        return truncTime;
    }
    
    private static Instant getEndOfDayFromInstant(Instant time) {
        return time.truncatedTo(ChronoUnit.DAYS)
            .plus(23L, ChronoUnit.HOURS)
            .plus(59L, ChronoUnit.MINUTES)
            .plus(59L, ChronoUnit.SECONDS)
            .plus(999L, ChronoUnit.MILLIS);
    }
}
