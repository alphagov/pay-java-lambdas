package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"V03", "v03"})
    void fromString_isCaseInsensitive(String input) {
        assertEquals(Version.V03, Version.fromString(input));
    }

    @Test
    void fromEnvironment_shouldThrowException_ifVersionNotRecognised() {
        var versionString = "V01";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Version.fromEnvironment(versionString));
        String actualMessage = exception.getMessage();
        assertEquals(format("Version not supported: %s", versionString), actualMessage);
    }
    
    @Test
    void fromString_shouldReturnUnknown_ifVersionNotRecognised() {
        var versionString = "V01";
        var result = Version.fromString(versionString);
        assertEquals(Version.UNKNOWN, result);
    }
}
