package uk.gov.pay.java_lambdas.bin_ranges_diff.config;

import software.amazon.awssdk.regions.Region;

import static java.lang.String.format;

public class Constants {

    private Constants() {
    }

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final String AWS_ACCOUNT_NAME = System.getenv("AWS_ACCOUNT_NAME");
    public static final String S3_STAGING_BUCKET_NAME = format("bin-ranges-staging-%s", AWS_ACCOUNT_NAME);
    public static final String S3_PROMOTED_BUCKET_NAME = format("bin-ranges-promoted-%s", AWS_ACCOUNT_NAME);
    public static final String PROMOTED_BIN_RANGES_S3_KEY = "/latest/WP_341BIN_V03.CSV";
    
}
