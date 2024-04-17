package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
            checkFileSizes(candidate);
            return parseAndValidate(candidate);
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
            return Candidate.from(candidate, false);
        }
    }

    private void checkFileSizes(Candidate candidate) {
        var headObjectCandidateBinRangesRequest = RequestBuilder.headObjectRequest(S3_STAGED_BUCKET_NAME, candidate.s3Key());
        var headObjectPromotedBinRangesRequest = RequestBuilder.headObjectRequest(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);

        List<HeadObjectRequest> requests = Arrays.asList(headObjectCandidateBinRangesRequest, headObjectPromotedBinRangesRequest);
        Map<String, Long> fileSizes = new ConcurrentHashMap<>();

        requests.parallelStream().forEach(headObjectRequest -> {
            HeadObjectResponse s3HeadObjectResponse = s3Client.headObject(headObjectRequest);
            fileSizes.put(headObjectRequest.key(), s3HeadObjectResponse.contentLength());
        });

        double change = PercentageChange.get(fileSizes.get(candidate.s3Key()), fileSizes.get(PROMOTED_BIN_RANGES_S3_KEY));
        if (change > ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE) {
            throw new UnexpectedFilesizeException(
                format("Candidate outside of acceptable change percentage [actual: %.2f], [acceptable: %.2f]", change, ACCEPTABLE_FILESIZE_DIFFERENCE_PERCENTAGE)
            );
        }
    }

    public Candidate parseAndValidate(Candidate candidate) {
        var getObjectCandidateBinRangesRequest = RequestBuilder.getObjectRequest(S3_STAGED_BUCKET_NAME, candidate.s3Key());
        try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getObjectCandidateBinRangesRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {

            List<String> candidateRows = reader.lines()
                .collect(Collectors.toList());
            
            candidateRows.removeFirst();
            candidateRows.removeLast();

            List<IssuerBINDetailRecord> records = Collections.synchronizedList(new ArrayList<>());

            AtomicInteger index = new AtomicInteger(1); // account for header record
            candidateRows.parallelStream().forEach(line -> {
                int currentIndex = index.getAndIncrement();
                try {
                    records.add(mapper.readerFor(IssuerBINDetailRecord.class)
                        .with(schema)
                        .readValue(line));
                } catch (JsonProcessingException e) {
                    // not logging appropriate line number, fix
                    logger.error("Error on line: {} [{}]", currentIndex, e.getMessage());
                }
            });
            return Candidate.from(candidate, (candidateRows.size() == records.size()));
        } catch (IOException e) {
            throw new BinRangeValidationException(format("Error validating candidate BIN range: %s", e.getMessage()));
        }
    }
}
