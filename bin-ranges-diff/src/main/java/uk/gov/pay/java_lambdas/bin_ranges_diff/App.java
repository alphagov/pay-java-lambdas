package uk.gov.pay.java_lambdas.bin_ranges_diff;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Pair;
import uk.gov.pay.java_lambdas.bin_ranges_diff.util.RequestBuilder;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.pay.java_lambdas.bin_ranges_diff.config.Constants.PROMOTED_BIN_RANGES_S3_KEY;
import static uk.gov.pay.java_lambdas.bin_ranges_diff.config.Constants.S3_PROMOTED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_diff.config.Constants.S3_STAGING_BUCKET_NAME;

public class App implements RequestHandler<Candidate, Candidate> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private final S3Client s3Client;

    public App() {
        s3Client = DependencyFactory.s3Client();
    }

    @Override
    public Candidate handleRequest(Candidate candidate, final Context context) {
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        var digests = getShaDigestsFromS3(candidate.s3Key());
        logger.info("Object SHAs [candidate: {}] [promoted: {}]", digests.left(), digests.right());
        return digests.left().equals(digests.right()) 
            ? Candidate.from(candidate, false)
            : Candidate.from(candidate, true);
    }

    private Pair<String, String> getShaDigestsFromS3(String candidateKey) {

//        var promotedBinRangesHeadObject = s3Client.headObject(RequestBuilder.headObjectRequest(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY));
//        var existingDigest = Optional.ofNullable(promotedBinRangesHeadObject.metadata().get("sha256MessageDigest"));
//        
//        if (existingDigest.isPresent()) {
//            
//        }

        var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(S3_STAGING_BUCKET_NAME, candidateKey);
        var getPromotedBinRangesRequest = RequestBuilder.getObjectRequest(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);

        return getSha256MessageDigests(getCandidateBinRangesRequest, getPromotedBinRangesRequest);
    }


    private Pair<String, String> getSha256MessageDigests(GetObjectRequest getCandidateBinRangesRequest, GetObjectRequest getPromotedBinRangesRequest) {
        List<GetObjectRequest> requests = Arrays.asList(getCandidateBinRangesRequest, getPromotedBinRangesRequest);
        Map<String, String> objectDigests = new ConcurrentHashMap<>();

        requests.parallelStream().forEach(req -> {
            try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(req);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {

                logger.debug("Creating SHA-256 digest for {}", req.key());
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

                String line;
                boolean firstLine = true; // skip the first line as it includes the date the file was published

                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        logger.debug("Ignored first line: {}", line);
                        firstLine = false;
                        continue;
                    }
                    messageDigest.update(line.getBytes(StandardCharsets.UTF_8));
                }

                byte[] shaBytes = messageDigest.digest();
                String base64Sha = Base64.getEncoder().encodeToString(shaBytes);
                objectDigests.put(req.key(), base64Sha);
                logger.debug("Generated SHA-256 digest for {}: {}", req.key(), base64Sha);
            } catch (IOException | NoSuchAlgorithmException e) {
                logger.error("Error reading file from S3: {}", e.getMessage());
            }
        });
        return Pair.of(
            objectDigests.get(getCandidateBinRangesRequest.key()),
            objectDigests.get(getPromotedBinRangesRequest.key())
        );
    }
}
