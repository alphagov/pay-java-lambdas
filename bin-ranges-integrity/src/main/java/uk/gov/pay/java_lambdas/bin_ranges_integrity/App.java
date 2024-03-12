package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.RequestBuilder;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.*;

public class App implements RequestHandler<Candidate, Candidate> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
    private final S3Client s3Client;

    public App() {
        s3Client = DependencyFactory.s3Client();
    }

    @Override
    public Candidate handleRequest(Candidate candidate, final Context context) {
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        List<String> linesFromS3File = getLinesFromS3File(S3_STAGING_BUCKET_NAME, candidate.s3Key());
        if (linesFromS3File == null) {
            logger.error("Failed to read file contents {}", candidate.s3Key());
            return result(candidate, false); 
        }
        List<String> linesFromS3PromotedFile = getLinesFromS3File(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);
        if (linesFromS3PromotedFile == null) {
            logger.error("Failed to read promoted file contents {}", PROMOTED_BIN_RANGES_S3_KEY);
            return result(candidate, false);
        }

        int candidateLineCount = linesFromS3File.size();
        int promotedLineCount = linesFromS3PromotedFile.size();
        
        if (candidateLineCount > Math.floorDiv(promotedLineCount * (100 + ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error("Contents of candidate is too large {}, candidate is {} lines long, promoted is {} lines long", candidate.s3Key(), candidateLineCount, promotedLineCount);
            return result(candidate, false);
        }
        
        if (candidateLineCount < Math.ceilDiv(promotedLineCount * (100 - ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error("Contents of candidate is too short {}, candidate is {} lines long, promoted is {} lines long", candidate.s3Key(), candidateLineCount, promotedLineCount);
            return result(candidate, false);
        }
        return result(candidate, true);
    }

    private static Candidate result(Candidate candidate, boolean shouldProceed) {
        return Candidate.from(candidate, shouldProceed);
    }


    private List<String> getLinesFromS3File(String s3StagingBucketName, String candidateKey) {
        // todo: check for pre-existing sha digests on the promoted head object

        var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(s3StagingBucketName, candidateKey);

        try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getCandidateBinRangesRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {

            logger.debug("Reading file {}", candidateKey);
            return reader.lines().toList();
        } catch (IOException e) {
            logger.error("Error reading file from S3: {}", e.getMessage());
        }
        return null;
    }
}
