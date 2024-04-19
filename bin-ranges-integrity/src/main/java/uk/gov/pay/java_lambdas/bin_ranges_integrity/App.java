package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.exception.BinRangeValidationException;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.exception.UnexpectedFilesizeException;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.PercentageChange;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.RequestBuilder;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.IssuerBINDetailRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE;
import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.PROMOTED_BIN_RANGES_S3_KEY;
import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.S3_PROMOTED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.S3_STAGED_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize.DeserializerModifierWithValidation.deserializeAndValidateModule;

public class App implements RequestHandler<Candidate, Candidate> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private final S3Client s3Client;
    private final CsvMapper mapper;
    private final CsvSchema schema;

    public App() {
        s3Client = DependencyFactory.s3Client();
        mapper = CsvMapper.builder()
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();
        mapper.registerModule(deserializeAndValidateModule());
        schema = mapper.schemaFor(IssuerBINDetailRecord.class).withoutQuoteChar().withoutHeader();
    }

    @Override
    public Candidate handleRequest(Candidate candidate, final Context context) {
        logger.info("fn: [{}], version: [{}].", context.getFunctionName(), context.getFunctionVersion());
        try {
            logger.info("Starting data integrity check for {}", candidate.s3Key());
            checkFileSizes(candidate);
            parseAndValidate(candidate);
            logger.info("Data integrity check successful");
            return Candidate.from(candidate, true);
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return Candidate.from(candidate, false);
        }
    }

    private void checkFileSizes(Candidate candidate) {
        var headObjectCandidateBinRangesRequest = RequestBuilder.headObjectRequest(S3_STAGED_BUCKET_NAME, candidate.s3Key());
        var headObjectPromotedBinRangesRequest = RequestBuilder.headObjectRequest(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);

        Map<String, Long> fileSizes = getFileSizes(headObjectCandidateBinRangesRequest, headObjectPromotedBinRangesRequest);
        double change = PercentageChange.get(fileSizes.get(candidate.s3Key()), fileSizes.get(PROMOTED_BIN_RANGES_S3_KEY));
        logger.debug("Change percentage [actual: {}] [acceptable: {}}]", change, ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE);
        if (change > ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE) {
            throw new UnexpectedFilesizeException(
                format("Candidate outside of acceptable change percentage [actual: %.2f] [acceptable: %.2f]", change, ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE)
            );
        }
    }

    private Map<String, Long> getFileSizes(HeadObjectRequest headObjectCandidateBinRangesRequest, HeadObjectRequest headObjectPromotedBinRangesRequest) {
        List<HeadObjectRequest> requests = Arrays.asList(headObjectCandidateBinRangesRequest, headObjectPromotedBinRangesRequest);
        Map<String, Long> fileSizes = new HashMap<>();
        
        requests.forEach(headObjectRequest -> {
            logger.debug("Retrieving HEAD for {} from {}", headObjectRequest.key(), headObjectRequest.bucket());
            HeadObjectResponse s3HeadObjectResponse = s3Client.headObject(headObjectRequest);
            logger.debug("[key: {}] [size: {}]", headObjectRequest.key(), s3HeadObjectResponse.contentLength());
            fileSizes.put(headObjectRequest.key(), s3HeadObjectResponse.contentLength());
        });
        return fileSizes;
    }

    public void parseAndValidate(Candidate candidate) {
        var getObjectCandidateBinRangesRequest = RequestBuilder.getObjectRequest(S3_STAGED_BUCKET_NAME, candidate.s3Key());
        logger.debug("Downloading {}", getObjectCandidateBinRangesRequest.key());
        try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getObjectCandidateBinRangesRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {
            
            AtomicInteger index = new AtomicInteger(0);
            Map<Integer, String> candidateRows = reader.lines()
                .collect(Collectors.toMap(line -> index.getAndIncrement(), line -> line));
            
            candidateRows.remove(candidateRows.size() - 1); // remove trailer record
            candidateRows.remove(0); // remove header record
            logger.debug("Candidate record total: {}", candidateRows.size());
            
            List<IssuerBINDetailRecord> records = Collections.synchronizedList(new ArrayList<>());
            candidateRows.entrySet().parallelStream().forEach(line -> {
                try {
                    records.add(mapper.readerFor(IssuerBINDetailRecord.class)
                        .with(schema)
                        .readValue(line.getValue()));
                } catch (JsonProcessingException e) {
                    logger.error("Error on line {} [{}]", line.getKey(), e.getMessage());
                } catch (ConstraintViolationException e) {
                    Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
                    Set<String> violationMessages = violations.stream()
                        .map(violation -> format("%s: %s",
                            violation.getPropertyPath(),
                            violation.getMessage()))
                        .collect(Collectors.toSet());
                    logger.error("Violation on line {} {}", line.getKey(), violationMessages);
                }
            });
            
            logger.debug("Validated record total: {}", records.size());
            if (records.size() != candidateRows.size()) {
                throw new BinRangeValidationException("Error validating candidate BIN range, see logs for more detail");
            } 
        } catch (IOException e) {
            throw new BinRangeValidationException(format("Error validating candidate BIN range: %s", e.getMessage()));
        }
    }
}
