package uk.gov.pay.java_lambdas.bin_ranges_promotion.config;

import software.amazon.awssdk.regions.Region;

import static java.lang.String.format;

public class Constants {

    private Constants() {

    }

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final String AWS_ACCOUNT_NAME = System.getenv("AWS_ACCOUNT_NAME");
    public static final String S3_STAGED_BUCKET_NAME = format("bin-ranges-staged-%s", AWS_ACCOUNT_NAME);
    public static final String S3_PROMOTED_BUCKET_NAME = format("bin-ranges-promoted-%s", AWS_ACCOUNT_NAME);
    public static final String PROMOTED_BIN_RANGES_S3_KEY = "latest/worldpay-v3.csv";
}
