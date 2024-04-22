package uk.gov.pay.java_lambdas.bin_ranges_transfer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.model.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.createTempPrivateKeyFile;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractDateString;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractVersion;

class UtilsTest {
    private static final String BIN_RANGE_VERSION_NOT_FOUND_ERROR_MESSAGE = "No version string found in file name";
    private static final String BIN_RANGE_DATE_NOT_FOUND_ERROR_MESSAGE = "No date string found in file name";
    
    @ParameterizedTest
    @CsvSource({
        "WP_341BIN_V04_20240212_001.CSV,2024-02-12", 
        "WP_341BIN_V03_20240102_001.CSV,2024-01-02",
        "WP_341BIN_V04_20221111_001.CSV,2022-11-11",
        "WP_341BIN_V03_19911122_001.CSV,1991-11-22",
    })
    void extractDateString_shouldReturnDateString_whenPresent (String input, String expected) {
        String result = extractDateString(input);
        assertEquals(expected, result);
    }
    
    @Test
    void extractDateString_shouldThrowException_whenDateStringIsMissing () {
        String fileName = "WP_341BIN_V04_no_date_here_001.CSV";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> extractDateString(fileName));
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(BIN_RANGE_DATE_NOT_FOUND_ERROR_MESSAGE));
    }

    @ParameterizedTest
    @CsvSource({
        "WP_341BIN_V04_00000000_001.CSV",
        "WP_341BIN_V04_20242311_001.CSV",
        "WP_341BIN_V04_20001035_001.CSV"
    })
    void extractDateString_shouldThrowException_whenDateStringIsInvalid (String input) {
        assertThrows(DateTimeParseException.class, () -> extractDateString(input));
    }
    
    @Test
    void createTempPrivateKeyFile_shouldWriteKeyToTemporaryFile() throws IOException {
        String expected = new String(Files.readAllBytes(
            Path.of(ClassLoader.getSystemResource("ssh/test_private_key.rsa").getPath()))
        );
        Path tempPrivateKeyPath = createTempPrivateKeyFile(expected);
        String result = new String(Files.readAllBytes(
            tempPrivateKeyPath
        ));
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource
    void extractVersion_shouldReturnVersion_whenPresent (String input, Version expected) {
        Version result = extractVersion(input);
        assertEquals(expected, result);
    }


    @ParameterizedTest
    @MethodSource
    void extractVersion_shouldThrowException_whenVersionIsMissingOrInvalidOrNotRecognised (String input, String expected) {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> extractVersion(input));
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expected));
    }
    
    // ---- PRIVATE

    private static Stream<Arguments> extractVersion_shouldReturnVersion_whenPresent() {
        return Stream.of(
            Arguments.of("WP_341BIN_V04_20240212_001.CSV", Version.V04),
            Arguments.of("WP_341BIN_V03_20240102_001.CSV", Version.V03),
            Arguments.of("WP_341BIN_V01_20242311_001.CSV", Version.UNKNOWN),
            Arguments.of("WP_341BIN_V99_20242311_001.CSV", Version.UNKNOWN)
        );
    }

    private static Stream<Arguments> extractVersion_shouldThrowException_whenVersionIsMissingOrInvalidOrNotRecognised() {
        return Stream.of(
            Arguments.of("WP_341BIN_00000000_001.CSV", BIN_RANGE_VERSION_NOT_FOUND_ERROR_MESSAGE),
            Arguments.of("WP_341BIN_VAA_20001035_001.CSV", BIN_RANGE_VERSION_NOT_FOUND_ERROR_MESSAGE)
            
        );
    }
}
