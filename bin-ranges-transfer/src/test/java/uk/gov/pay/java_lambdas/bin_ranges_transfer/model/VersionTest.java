package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"V03", "v03"})
    void fromString_isCaseInsensitive(String input) {
        assertEquals(Version.V03, Version.fromString(input));
    }
}
