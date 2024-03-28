package uk.gov.pay.java_lambdas.bin_ranges_promotion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.pay.java_lambdas.bin_ranges_promotion.exception.S3TransferException;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.PROMOTED_BIN_RANGES_S3_KEY;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.S3_PROMOTED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.S3_STAGED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.util.RequestBuilder.copyCandidateToPromotedBucket;
import static uk.gov.pay.java_lambdas.bin_ranges_promotion.util.RequestBuilder.overwritePromotedRange;

public class App implements RequestHandler<Candidate, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private final S3Client s3Client;

    public App() {
        s3Client = DependencyFactory.s3Client();
    }

    @Override
    public Boolean handleRequest(final Candidate candidate, final Context context) {
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        try {
            logger.debug("Attempting transfer of {} from {} to {}", candidate.s3Key(), S3_STAGED_BUCKET_NAME, S3_PROMOTED_BUCKET_NAME);
            var candidateCopyResponse = s3Client.copyObject(copyCandidateToPromotedBucket(candidate.s3Key()));
            logger.info("Transfer succeeded [SHA256: {}]", candidateCopyResponse.copyObjectResult().checksumSHA256());
            logger.debug("Overwriting contents of {} with {}", PROMOTED_BIN_RANGES_S3_KEY, candidate.s3Key());
            var overwritePromotedRangeResponse = s3Client.copyObject(overwritePromotedRange(candidate.s3Key()));
            logger.info("Overwrite succeeded [SHA256: {}]", overwritePromotedRangeResponse.copyObjectResult().checksumSHA256());
        } catch (Exception e) {
            throw new S3TransferException(e.getMessage());
        }
        return true;
    }
}
