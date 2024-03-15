package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
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
import java.util.List;
import java.util.Map;

import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.*;

public class App implements RequestHandler<Candidate, Candidate> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
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
            return Candidate.from(candidate, false);
        }
        List<String> linesFromS3PromotedFile = getLinesFromS3File(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);
        if (linesFromS3PromotedFile == null) {
            logger.error("Failed to read promoted file contents {}", PROMOTED_BIN_RANGES_S3_KEY);
            return Candidate.from(candidate, false);
        }

        if (!validateFileLength(linesFromS3File, linesFromS3PromotedFile)) {
            return Candidate.from(candidate, false);
        }

        boolean shouldProceed = validateRowContents(linesFromS3File);
        return Candidate.from(candidate, shouldProceed);
    }

    private static boolean validateRowContents(List<String> linesFromS3File) {

        MappingIterator<Map<String, String>> it = parseCsv(linesFromS3File);

        if (null == it) {
            logger.error("No information could be gained from parsing the CSV (iterator is null).");
            return false;
        }
        try {
            Map<String, String> firstRow = it.nextValue();
            if (!validateFirstRow(firstRow)) {
                return false;
            }
            while (it.hasNextValue()) {
                Map<String, String> currentRow = it.nextValue();
                if (it.hasNextValue()) {
                    if (!validateMiddleRow(currentRow)) {
                        return false;
                    }
                } else {
                    if (!validateFinalRow(currentRow)) {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("IOException while iterating over CSV contents. {}", e.getMessage());
            return false;
        }
        return true;
    }

    private static boolean validateFileLength(List<String> linesFromS3File, List<String> linesFromS3PromotedFile) {
        int candidateLineCount = linesFromS3File.size();
        int promotedLineCount = linesFromS3PromotedFile.size();

        if (candidateLineCount > Math.floorDiv(promotedLineCount * (100 + ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error("Contents of candidate is too large, candidate is {} lines long, promoted is {} lines long", candidateLineCount, promotedLineCount);
            return false;
        }

        if (candidateLineCount < Math.ceilDiv(promotedLineCount * (100 - ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error("Contents of candidate is too short, candidate is {} lines long, promoted is {} lines long", candidateLineCount, promotedLineCount);
            return false;
        }
        return true;
    }

    private static boolean validateFirstRow(Map<String, String> firstRow) {
        String firstHeaderRecord = firstRow.get("Header Record");
        if (!firstHeaderRecord.equals("00")) {
            logger.error("Expected the first row to have a 00 header record as per the documentation, actual value was {}.", firstHeaderRecord);
            return false;
        }
        return true;
    }

    private static boolean validateMiddleRow(Map<String, String> currentRow) {
        String headerRecord = currentRow.get("Header Record");
        String binLowerRangeStr = currentRow.get("BIN Lower Range");
        String binUpperRangeSr = currentRow.get("BIN Upper Range");
        String productType = currentRow.get("Product Type");
        int binStart = Integer.parseInt(binLowerRangeStr);
        int binEnd = Integer.parseInt(binUpperRangeSr);
        if (!headerRecord.equals("01")) {
            logger.error("Expected bin data rows to have a 01 header record as per the documentation, actual value was {}.", headerRecord);
            return false;
        }
        if (binLowerRangeStr.length() != 18) {
            logger.error("Expected bin lower range information to be 18 digits long but {} was {} digits long", binLowerRangeStr, binLowerRangeStr.length());
            return false;
        }
        if (binUpperRangeSr.length() != 18) {
            logger.error("Expected bin upper range information to be 18 digits long but {} was {} digits long", binUpperRangeSr, binUpperRangeSr.length());
            return false;
        }
        if (binEnd < binStart) {
            logger.error("Expected bin start to be a lower number than bin end.  Start: {}, End: {}.", binLowerRangeStr, binUpperRangeSr);
            return false;
        }
        if (!productType.equals("CN") && !productType.equals("CP")) {
            logger.error("Product type is expected to be CN or CP but the value was {}.", productType);
            return false;
        }
        return true;
    }

    private static boolean validateFinalRow(Map<String, String> currentRow) {
        String lastHeaderRecord = currentRow.get("Header Record");
        if (!lastHeaderRecord.equals("99")) {
            logger.error("Expected the last row to have a 99 header record as per the documentation, actual value was {}.", lastHeaderRecord);
            return false;
        }
        return true;
    }

    private List<String> getLinesFromS3File(String s3BucketName, String candidateKey) {
        try {
            var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(s3BucketName, candidateKey);
            ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getCandidateBinRangesRequest);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {
                logger.debug("Reading file {}", candidateKey);
                return reader.lines().toList();
            }
        } catch (IOException e) {
            logger.error("Error reading file from S3: {}", e.getMessage());
        }
        return null;
    }

    private static MappingIterator<Map<String, String>> parseCsv(List<String> lines) {
        try {
            String CSV_DOC = String.join("\n", lines);
            CsvSchema schema = CsvSchema.builder()
                .addColumn("Header Record", CsvSchema.ColumnType.STRING)
                .addColumn("BIN Lower Range", CsvSchema.ColumnType.NUMBER)
                .addColumn("BIN Upper Range", CsvSchema.ColumnType.NUMBER)
                .addColumn("Product Type")
                .addColumn("Card Scheme Brand Name")
                .addColumn("Issuer Name")
                .addColumn("Issuer Country Code")
                .addColumn("Issuer Country Code Numeric")
                .addColumn("Issuer Country Name")
                .addColumn("Card Class")
                .addColumn("Cardholder Currency Code")
                .addColumn("DDC Flag")
                .addColumn("Scheme Product")
                .addColumn("Anonymous Pre-Paid Marker")
                .addColumn("Accepts Gaming OCT Payments")
                .addColumn("Tokenised Flag")
                .addColumn("Pan Length")
                .addColumn("Fast Funds Indicator")
                .addColumn("Reservered 19")
                .addColumn("Reservered 20")
                .addColumn("Reservered 21")
                .addColumn("Reservered 22")
                .addColumn("Reservered 23")
                .build();
            CsvMapper mapper = new CsvMapper();
            MappingIterator<Map<String, String>> it = mapper
                .readerForMapOf(String.class)
                .with(schema)
                .readValues(CSV_DOC);
            return it;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
