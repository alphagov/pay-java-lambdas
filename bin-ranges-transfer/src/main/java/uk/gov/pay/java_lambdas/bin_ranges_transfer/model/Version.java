package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public enum Version {
    V03, V04, UNKNOWN;
    
    private static final Logger logger = LoggerFactory.getLogger(Version.class);
    private static final List<Version> SUPPORTED_VERSIONS = Arrays.asList(V03, V04);

    public static Version fromEnvironment(String versionStr) {
        for (Version v : SUPPORTED_VERSIONS) {
            if (v.name().equals(versionStr.toUpperCase())) {
                logger.info("Configured Worldpay File Version: {}", v.name());
                return v;
            }
        }
        throw new IllegalArgumentException("Version not supported: " + versionStr);
    }

    public static Version fromString(String versionStr) {
        for (Version v : SUPPORTED_VERSIONS) {
            if (v.name().equals(versionStr.toUpperCase())) {
                logger.debug("Version validated: {}", v.name());
                return v;
            }
        }
        logger.warn("Version not recognised: {}", versionStr);
        return UNKNOWN;
    }
}
