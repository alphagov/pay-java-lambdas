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
import static uk.gov.pay.java_lambdas.bin_ranges_diff.config.Constants.S3_STAGED_BUCKET_NAME;

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
        Pair<String, String> digests = getShaDigestsFromS3(candidate.s3Key());
        var candidateSha = digests.left();
        var promotedSha = digests.right();
        logger.info("Object SHAs [candidate: {}] [promoted: {}]", candidateSha, promotedSha);
        return candidateSha.equals(promotedSha)
            ? Candidate.halt(candidate, "BIN ranges candidate SHA is identical to promoted, halting ingest")
            : Candidate.proceed(candidate);
    }

    private Pair<String, String> getShaDigestsFromS3(String candidateKey) {
        var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(S3_STAGED_BUCKET_NAME, candidateKey);
        var getPromotedBinRangesRequest = RequestBuilder.getObjectRequest(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);

        List<GetObjectRequest> requests = Arrays.asList(getCandidateBinRangesRequest, getPromotedBinRangesRequest);
        Map<String, String> objectDigests = new ConcurrentHashMap<>();

        requests.parallelStream().forEach(req -> {
            try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(req);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {

                logger.debug("Creating SHA-256 digest for {}", req.key());
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                reader.lines()
                    .skip(1) // Skip the first line as it includes the date the file was published
                    .map(line -> line.getBytes(StandardCharsets.UTF_8))
                    .forEach(messageDigest::update);

                String base64Sha = BASE_64_ENCODER.encodeToString(messageDigest.digest());
                objectDigests.put(req.key(), base64Sha);
                logger.debug("Generated SHA-256 digest for {}: {}", req.key(), base64Sha);
            } catch (IOException e) {
                logger.error("Error reading file from S3: {}", e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                logger.error("This should never happen: {}", e.getMessage());
            }
        });
        return Pair.of(
            objectDigests.get(getCandidateBinRangesRequest.key()),
            objectDigests.get(getPromotedBinRangesRequest.key())
        );
    }
}
