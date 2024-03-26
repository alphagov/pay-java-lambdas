package uk.gov.pay.java_lambdas.bin_ranges_integrity.util;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class RequestBuilder {

    private RequestBuilder() {
        
    }

    public static GetObjectRequest getObjectRequest(String bucketName, String objectKey) {
        return GetObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build();
    }

    public static HeadObjectRequest headObjectRequest(String bucketName, String objectKey) {
        return HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build();
    }
}
