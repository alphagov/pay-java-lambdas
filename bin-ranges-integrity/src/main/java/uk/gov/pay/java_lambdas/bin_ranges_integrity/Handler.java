package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import software.amazon.awssdk.services.s3.S3Client;

public class Handler {
    private final S3Client s3Client;

    public Handler() {
        s3Client = DependencyFactory.s3Client();
    }

    public void sendRequest() {
        // TODO: api calls using s3Client. Get CSV files from buckets etc...
    }
}
