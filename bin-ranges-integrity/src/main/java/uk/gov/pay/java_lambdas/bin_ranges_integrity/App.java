package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.RequestBuilder;

import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators.CsvMiddleRow;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;


import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.*;

public class App implements RequestHandler<Candidate, Candidate> {
    private final Logger logger;
    private final S3Client s3Client;

    public App() {
        s3Client = DependencyFactory.s3Client();
        logger = DependencyFactory.logger();
    }

    @Override
    public Candidate handleRequest(Candidate candidate, final Context context) {
        logger.info("fn: [{}], version: [{}].", context.getFunctionName(), context.getFunctionVersion());
        List<String> linesFromS3CandidateFile = getLinesFromS3File(S3_STAGING_BUCKET_NAME, candidate.s3Key());
        if (linesFromS3CandidateFile == null) { /* I haven't managed to hit this condition but the IDE says it's important */
            logger.error(String.format("Failed to read file contents [%s]", candidate.s3Key()));
            return Candidate.from(candidate, false);
        }
        List<String> linesFromS3PromotedFile = getLinesFromS3File(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);
        if (linesFromS3PromotedFile == null) {/* I haven't managed to hit this condition but the IDE says it's important */
            logger.error(String.format("Failed to read promoted file contents [%s]", PROMOTED_BIN_RANGES_S3_KEY));
            return Candidate.from(candidate, false);
        }

        try {
            validateCsv(linesFromS3CandidateFile);
            return Candidate.from(candidate, true);
        } catch (Exception e) {
            logger.error("CSV parsing failled with error", e);
            return Candidate.from(candidate, false);
        }
    }
    
    private List<String> getLinesFromS3File(String s3BucketName, String candidateKey) {
        try {
            var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(s3BucketName, candidateKey);
            ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getCandidateBinRangesRequest);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {
                logger.debug("Reading file [{}]", candidateKey);
                return reader.lines().toList();
            }
        } catch (IOException e) {
            logger.error(String.format("NOT_TESTED_YET: Error reading file from S3: [%s]", e.getMessage()));
        }
        return null;
    }


    public void validateCsv(List<String> csvLines) throws Exception {
        // remove first and last elements (junk data we don't care about)
        List<String> linesToValidate = csvLines.subList(1, csvLines.size() - 1);
        // todo maybe we can pass the s3 object inputstream directly to readValues() so we don't have rejoin the array
        String csvContent = String.join("\n", linesToValidate);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(CsvMiddleRow.class).withoutQuoteChar().withoutHeader();

        // todo handle exceptions etc
        try (StringReader reader = new StringReader(csvContent)) {
            MappingIterator<CsvMiddleRow> it = mapper.readerFor(CsvMiddleRow.class)
                .with(schema)
                .readValues(reader);

            List<CsvMiddleRow> rows = it.readAll();

            for (CsvMiddleRow row : rows) {
                // do stuff with row if needed, we've hopefully already validated via javax annotations on the class
            }
        }

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

//        for (int i = 0; i < rows.size(); i++) {
//            Object row = rows.get(i);
//            if (i == 0 && !(row instanceof CsvFirstRow)) {
//                throw new Exception("First row must be of type 00");
//            } else if (i == rows.size() - 1 && !(row instanceof CsvLastRow)) {
//                throw new Exception("Last row must be of type 99");
//            } else if (!(row instanceof CsvMiddleRow)) {
//                throw new Exception("Middle rows must be of type 01");
//            }
//
//            Set<ConstraintViolation<Object>> violations = validator.validate(row);
//            if (!violations.isEmpty()) {
//                throw new Exception("Invalid row: " + violations.toString());
//            }
//        }
    }
}
