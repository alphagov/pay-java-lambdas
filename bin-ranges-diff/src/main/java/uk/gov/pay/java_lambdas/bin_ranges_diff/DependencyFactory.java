package uk.gov.pay.java_lambdas.bin_ranges_diff;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class DependencyFactory {

    private DependencyFactory() {}

    public static S3Client s3Client() {
        Region region = Region.EU_WEST_1;
        return S3Client.builder()
                .region(region)
                .build();
    }
}
