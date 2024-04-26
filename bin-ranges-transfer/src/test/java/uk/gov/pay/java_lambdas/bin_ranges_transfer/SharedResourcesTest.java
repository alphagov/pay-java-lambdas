package uk.gov.pay.java_lambdas.bin_ranges_transfer;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SharedResourcesTest {
    @Test
    void testUsingSharedData() {

        String privateKeyPath = getClass().getResource("/ssh-config/test_private_key.rsa").getPath();
        InputStream resourceAsStream = getClass().getResourceAsStream("/realistic-ranges/BIN_V03_REDACTED_2.zip");
        assertNotNull(resourceAsStream);
        assertNotNull(privateKeyPath);
    }
}
