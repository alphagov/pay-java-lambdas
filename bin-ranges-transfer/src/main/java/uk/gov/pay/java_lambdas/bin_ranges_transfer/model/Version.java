package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Version {
    V03, V04;

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    public static Version fromString(String versionStr) {
        for (Version v : Version.values()) {
            if (v.name().equals(versionStr.toUpperCase())) {
                logger.debug("Version validated: {}", v.name());
                return v;
            }
        }
        throw new IllegalArgumentException("File version not recognised: " + versionStr);
    }
}
