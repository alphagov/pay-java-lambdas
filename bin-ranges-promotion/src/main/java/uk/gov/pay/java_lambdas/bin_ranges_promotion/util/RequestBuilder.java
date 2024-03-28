package uk.gov.pay.java_lambdas.bin_ranges_promotion.util;

import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.PROMOTED_BIN_RANGES_S3_KEY;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.S3_PROMOTED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.S3_STAGED_BUCKET_NAME;

public class RequestBuilder {
    
    private RequestBuilder () {
        
    }
    
    public static CopyObjectRequest copyCandidateToPromotedBucket(String candidateKey) {
        return CopyObjectRequest.builder()
            .sourceBucket(S3_STAGED_BUCKET_NAME)
            .sourceKey(candidateKey)
            .destinationBucket(S3_PROMOTED_BUCKET_NAME)
            .destinationKey(candidateKey)
            .build();
    }
    
    public static CopyObjectRequest overwritePromotedRange(String candidateKey) {
        return CopyObjectRequest.builder()
            .sourceBucket(S3_PROMOTED_BUCKET_NAME)
            .sourceKey(candidateKey)
            .destinationBucket(S3_PROMOTED_BUCKET_NAME)
            .destinationKey(PROMOTED_BIN_RANGES_S3_KEY)
            .build();
    }
}
