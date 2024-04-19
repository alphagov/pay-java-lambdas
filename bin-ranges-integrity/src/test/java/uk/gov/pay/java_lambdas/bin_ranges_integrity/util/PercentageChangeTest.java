package uk.gov.pay.java_lambdas.bin_ranges_integrity.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentageChangeTest {
    
    @ParameterizedTest
    @MethodSource
    void percentageChange_shouldGetChangePercentageAbsoluteRounded(Long candidate, Long promoted, Double expected) {
        var result = PercentageChange.get(candidate, promoted);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> percentageChange_shouldGetChangePercentageAbsoluteRounded() {
        return Stream.of(
            Arguments.of(1060L, 1000L, 6.00),
            Arguments.of(1000L, 1060L, 5.660377358490567),
            Arguments.of(500L, 1000L, 50.00),
            Arguments.of(2000L, 1000L, 100.00)
        );
    }
}
