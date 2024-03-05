package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinRangeSftpDownloadTest {
    
    BinRangeSftpDownload underTest;
    
    @BeforeEach
    void setUp() {
        underTest = new BinRangeSftpDownload("path/to/file", "WP_341BIN_V03_20240226_001.csv", 0L);
    }
    
    @Test
    void constructor_ExtractsDateAndVersion_FromFileName() {
        assertEquals("2024-02-26", underTest.date());
        assertEquals(Version.V03, underTest.version());
    }

    @Test
    void getS3Key_ReturnsExpectedFormat() {
        assertEquals("2024-02-26/WP_341BIN_V03_20240226_001.csv", underTest.getS3Key());
    }
}
