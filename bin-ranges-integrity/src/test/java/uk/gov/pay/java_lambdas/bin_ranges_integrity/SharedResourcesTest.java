package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SharedResourcesTest {

    @Test
    void testUsingSharedData() {
        InputStream resourceAsStream = getClass().getResourceAsStream("/realistic-ranges/BIN_V03_REDACTED_2.zip");
        assertNotNull(resourceAsStream);
    }
}
