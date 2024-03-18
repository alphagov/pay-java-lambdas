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
import java.util.ArrayList;
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
        List<String> linesFromS3CandidateFile = getLinesFromS3File(S3_STAGING_BUCKET_NAME, candidate.s3Key());
        if (linesFromS3CandidateFile == null) {
            logger.error("Failed to read file contents {}", candidate.s3Key());
            return Candidate.from(candidate, false);
        }
        List<String> linesFromS3PromotedFile = getLinesFromS3File(S3_PROMOTED_BUCKET_NAME, PROMOTED_BIN_RANGES_S3_KEY);
        if (linesFromS3PromotedFile == null) {
            logger.error("Failed to read promoted file contents {}", PROMOTED_BIN_RANGES_S3_KEY);
            return Candidate.from(candidate, false);
        }

        if (!validateFileLength(linesFromS3CandidateFile, linesFromS3PromotedFile)) {
            return Candidate.from(candidate, false);
        }

        boolean shouldProceed = validateRowContents(linesFromS3CandidateFile);
        return Candidate.from(candidate, shouldProceed);
    }

    private static boolean validateRowContents(List<String> linesFromS3File) {
        int lineCount = 0;
        MappingIterator<Map<String, String>> it = parseCsv(linesFromS3File);

        if (null == it) {
            logger.error("No information could be gained from parsing the CSV (iterator is null).");
            return false;
        }
        try {
            Map<String, String> firstRow = it.nextValue();
            lineCount++;
            if (!validateFirstRow(firstRow)) {
                logger.info("Failed on line {}", lineCount);
                return false;
            }
            while (it.hasNextValue()) {
                Map<String, String> currentRow = it.nextValue();
                lineCount++;

                if (it.hasNextValue()) {
                    if (!validateMiddleRow(currentRow)) {
                        logger.info("Failed on line {}", lineCount);
                        return false;
                    }
                } else {
                    if (!validateFinalRow(currentRow)) {
                        logger.info("Failed on line {}", lineCount);
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
        String cardSchemeBrandName = currentRow.get("Card Scheme Brand Name");
        String issuerName = currentRow.get("Issuer Name");
        String issuerCountryCode = currentRow.get("Issuer Country Code");
        String cardClass = currentRow.get("Card Class");
        String cardholderCurrencyCode = currentRow.get("Cardholder Currency Code");
        String dccFlag = currentRow.get("DCC Flag");
        String anonymousPPMarker = currentRow.get("Anonymous Pre-Paid Marker");
        String acceptsGamingOctPayments = currentRow.get("Accepts Gaming OCT Payments");
        String tokenisedFlag = currentRow.get("Tokenised Flag");
        String panLengthStr = currentRow.get("Pan Length");
        String fastFundsIndicator = currentRow.get("Fast Funds Indicator");
        long panLength = Long.parseLong(panLengthStr.isEmpty() ? "0" : panLengthStr);
        long binStart = Long.parseLong(binLowerRangeStr);
        long binEnd = Long.parseLong(binUpperRangeSr);
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
        if (cardSchemeBrandName.length() < 2) {
            logger.error("Card Scheme Brand Name should be at least 2 chars long, {} was provided.", cardSchemeBrandName);
            return false;
        }
        int maxCardSchemeBrandNameLength = 30;
        if (cardSchemeBrandName.length() > maxCardSchemeBrandNameLength) {
            logger.error("Card Scheme Brand Name no more than {} chars long, {} was provided with length of {}.", maxCardSchemeBrandNameLength, cardSchemeBrandName, cardSchemeBrandName.length());
            return false;
        }
        if (issuerName.length() < 2) {
            logger.error("Issuer Name should be at least 2 chars long, {} was provided.", cardSchemeBrandName);
            return false;
        }
        int maxIssuerNameLength = 80;
        if (issuerName.length() > maxIssuerNameLength) {
            logger.error("Issuer Name should be at no more than {} chars long, {} was provided with length of {}.", maxIssuerNameLength, issuerName, issuerName.length());
            return false;
        }
        if (issuerCountryCode.length() != 3) {
            logger.error("Issuer country code should be exactly three digits long but {} was provided", issuerCountryCode);
            return false;
        }
        if (!cardClass.equals("C") && !cardClass.equals("D") && !cardClass.equals("P")) {
            logger.error("Card class is expected to be C, D or P but the value was {}.", cardClass);
            return false;
        }
        if (cardholderCurrencyCode.length() != 3) {
            logger.error("Cardholder Currency Code should be exactly 3 chars long, {} was provided.", cardholderCurrencyCode);
            return false;
        }
        if (!dccFlag.isEmpty() && !dccFlag.equals("DCC allowed")) {
            logger.error("DCC Flag can only be empty or exactly 'DCC allowed', {} was provided.", dccFlag);
            return false;
        }
        if (!anonymousPPMarker.equals("N") && !anonymousPPMarker.equals("E") && !anonymousPPMarker.equals("A") && !anonymousPPMarker.equals("U")) {
            logger.error("Anonymous Pre-Paid Flag can only contain N, E, A, or U. {} was provided.", anonymousPPMarker);
            return false;
        }
        if (!acceptsGamingOctPayments.isEmpty() && !acceptsGamingOctPayments.equals("Y") && !acceptsGamingOctPayments.equals("N")) {
            logger.error("Gaming OCT Payements can only contain Y, N, or be blank. {} was provided.", acceptsGamingOctPayments);
            return false;
        }
        if (!tokenisedFlag.isEmpty() && !tokenisedFlag.equals("Y")) {
            logger.error("Tokenised Flag can only contain Y or be blank. {} was provided.", tokenisedFlag);
            return false;
        }
        if (panLength > 19) {
            logger.error("Pan length is greater than 19, {} was provided", panLengthStr);
            return false;
        }
        if (!fastFundsIndicator.isEmpty() && !fastFundsIndicator.equals("D") && !fastFundsIndicator.equals("N") && !fastFundsIndicator.equals("Y") && !fastFundsIndicator.equals("C")) {
            logger.error("Fast Funds Indicator can only contain D, N, Y, C, or be blank. {} was provided.", fastFundsIndicator);
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
                .addColumn("Product Type", CsvSchema.ColumnType.STRING)
                .addColumn("Card Scheme Brand Name", CsvSchema.ColumnType.STRING)
                .addColumn("Issuer Name", CsvSchema.ColumnType.STRING)
                .addColumn("Issuer Country Code", CsvSchema.ColumnType.STRING)
                .addColumn("Issuer Country Code Numeric", CsvSchema.ColumnType.NUMBER)
                .addColumn("Issuer Country Name", CsvSchema.ColumnType.STRING)
                .addColumn("Card Class", CsvSchema.ColumnType.STRING)
                .addColumn("Cardholder Currency Code", CsvSchema.ColumnType.STRING)
                .addColumn("DCC Flag", CsvSchema.ColumnType.STRING)
                .addColumn("Scheme Product", CsvSchema.ColumnType.STRING)
                .addColumn("Anonymous Pre-Paid Marker", CsvSchema.ColumnType.STRING)
                .addColumn("Accepts Gaming OCT Payments", CsvSchema.ColumnType.STRING)
                .addColumn("Tokenised Flag", CsvSchema.ColumnType.STRING)
                .addColumn("Pan Length", CsvSchema.ColumnType.NUMBER)
                .addColumn("Fast Funds Indicator", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 19", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 20", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 21", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 22", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 23", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 24", CsvSchema.ColumnType.STRING)
                .addColumn("Reservered 25", CsvSchema.ColumnType.STRING)
                .build()
                .withoutQuoteChar();
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
