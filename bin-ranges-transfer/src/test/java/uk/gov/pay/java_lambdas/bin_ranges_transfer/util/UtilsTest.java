package uk.gov.pay.java_lambdas.bin_ranges_transfer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.createTempPrivateKeyFile;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractDateString;

class UtilsTest {
    
    @ParameterizedTest
    @MethodSource
    void extractDateString_shouldReturnDateString_whenPresent (String input, String expected) {
        String result = extractDateString(input);
        assertEquals(expected, result);
    }
    
    @Test
    void extractDateString_shouldThrowException_whenDateStringIsMissing () {
        String fileName = "WP_341BIN_V04_no_date_here_001.CSV";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> extractDateString(fileName));

        String expectedMessage = String.format("no date string found in file name: %s", fileName);
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @ParameterizedTest
    @MethodSource
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
    
    // ---- PRIVATE
    
    private static Stream<Arguments> extractDateString_shouldReturnDateString_whenPresent() {
        return Stream.of(
            Arguments.of("WP_341BIN_V04_20240212_001.CSV", "2024-02-12"),
            Arguments.of("WP_341BIN_V03_20240102_001.CSV", "2024-01-02"),
            Arguments.of("WP_341BIN_V04_20221111_001.CSV", "2022-11-11"),
            Arguments.of("WP_341BIN_V03_19911122_001.CSV", "1991-11-22")
        );
    }
    
    private static Stream<Arguments> extractDateString_shouldThrowException_whenDateStringIsInvalid() {
        return Stream.of(
            Arguments.of("WP_341BIN_V04_00000000_001.CSV"),
            Arguments.of("WP_341BIN_V04_20242311_001.CSV"),
            Arguments.of("WP_341BIN_V04_20001035_001.CSV")
        );
    }
}
