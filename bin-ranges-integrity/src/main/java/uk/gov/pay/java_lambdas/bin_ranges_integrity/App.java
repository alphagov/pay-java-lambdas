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
import uk.gov.pay.java_lambdas.bin_ranges_integrity.util.FieldName;
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

        if (!validateFileLength(linesFromS3CandidateFile, linesFromS3PromotedFile)) {
            return Candidate.from(candidate, false);
        }

        boolean shouldProceed = validateRowContents(linesFromS3CandidateFile);
        return Candidate.from(candidate, shouldProceed);
    }

    private boolean validateRowContents(List<String> linesFromS3File) {
        int lineCount = 0;
        MappingIterator<Map<String, String>> it = parseCsv(linesFromS3File);

        if (null == it) {/* I haven't managed to hit this condition but the IDE says it's important */
            logger.error("No information could be gained from parsing the CSV (iterator is null).");
            return false;
        }
        try {
            Map<String, String> firstRow = it.nextValue();
            lineCount++;
            if (!validateFirstRow(firstRow)) {
                logger.info("Failed on line [{}]", lineCount);
                return false;
            }
            while (it.hasNextValue()) {
                Map<String, String> currentRow = it.nextValue();
                lineCount++;

                if (it.hasNextValue()) {
                    if (!validateMiddleRow(currentRow)) {
                        logger.info("Failed on line [{}]", lineCount);
                        return false;
                    }
                } else {
                    if (!validateFinalRow(currentRow)) {
                        logger.info("Failed on line [{}]", lineCount);
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(String.format("IOException while iterating over CSV contents. [%s]", e.getMessage()));
            return false;
        }
        return true;
    }

    private boolean validateFileLength(List<String> linesFromS3File, List<String> linesFromS3PromotedFile) {
        int candidateLineCount = linesFromS3File.size();
        int promotedLineCount = linesFromS3PromotedFile.size();

        if (candidateLineCount > Math.floorDiv(promotedLineCount * (100 + ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error(String.format("Contents of candidate is too large, candidate is [%s] lines long, promoted is [%s] lines long", candidateLineCount, promotedLineCount));
            return false;
        }

        if (candidateLineCount < Math.ceilDiv(promotedLineCount * (100 - ACCEPTABLE_LINE_DIFFERENCE_PERCENTAGE), 100)) {
            logger.error(String.format("Contents of candidate is too short, candidate is [%s] lines long, promoted is [%s] lines long", candidateLineCount, promotedLineCount));
            return false;
        }
        return true;
    }

    private boolean validateFirstRow(Map<String, String> firstRow) {
        String firstHeaderRecord = firstRow.get(FieldName.HEADER_RECORD);
        if (!firstHeaderRecord.equals("00")) {
            logger.error(String.format("Expected the first row to have a 00 header record as per the documentation, actual value was [%s].", firstHeaderRecord));
            return false;
        }
        return true;
    }

    private boolean validateMiddleRow(Map<String, String> currentRow) {
        String headerRecord = currentRow.get(FieldName.HEADER_RECORD);
        String binLowerRangeStr = currentRow.get(FieldName.BIN_LOWER_RANGE);
        String binUpperRangeSr = currentRow.get(FieldName.BIN_UPPER_RANGE);
        String productType = currentRow.get(FieldName.PRODUCT_TYPE);
        String cardSchemeBrandName = currentRow.get(FieldName.CARD_SCHEME_BRAND_NAME);
        String issuerName = currentRow.get(FieldName.ISSUER_NAME);
        String issuerCountryCode = currentRow.get(FieldName.ISSUER_COUNTRY_CODE);
        String cardClass = currentRow.get(FieldName.CARD_CLASS);
        String cardholderCurrencyCode = currentRow.get(FieldName.CARDHOLDER_CURRENCY_CODE);
        String dccFlag = currentRow.get(FieldName.DCC_FLAG);
        String anonymousPPMarker = currentRow.get(FieldName.ANON_PREPAID_MARKER);
        String acceptsGamingOctPayments = currentRow.get(FieldName.ACCEPTS_GAMING_OCT_PAYMENTS);
        String tokenisedFlag = currentRow.get(FieldName.TOKENISED_FLAG);
        String panLengthStr = currentRow.get(FieldName.PAN_LENGTH);
        String fastFundsIndicator = currentRow.get(FieldName.FAST_FUNDS_INDICATOR);
        long panLength = Long.parseLong(panLengthStr.isEmpty() ? "0" : panLengthStr);
        if (!headerRecord.equals("01")) {
            logger.error(String.format("Expected bin data rows to have a 01 header record as per the documentation, actual value was [%s].", headerRecord));
            return false;
        }
        if (binLowerRangeStr.length() != 18) {
            logger.error(String.format("Expected bin lower range information to be 18 digits long but [%s] was [%s] digits long", binLowerRangeStr, binLowerRangeStr.length()));
            return false;
        }
        if (binUpperRangeSr.length() != 18) {
            logger.error(String.format("Expected bin upper range information to be 18 digits long but [%s] was [%s] digits long", binUpperRangeSr, binUpperRangeSr.length()));
            return false;
        }
        long binStart = Long.parseLong(binLowerRangeStr);
        long binEnd = Long.parseLong(binUpperRangeSr);
        if (binEnd < binStart) {
            logger.error(String.format("Expected bin start to be a lower number than bin end.  Start: [%s], End: [%s].", binLowerRangeStr, binUpperRangeSr));
            return false;
        }
        if (!productType.equals("CN") && !productType.equals("CP")) {
            logger.error(String.format("Product type is expected to be CN or CP but the value was [%s].", productType));
            return false;
        }
        if (cardSchemeBrandName.length() < 2) {
            logger.error(String.format("Card Scheme Brand Name should be at least 2 chars long, [%s] was provided.", cardSchemeBrandName));
            return false;
        }
        int maxCardSchemeBrandNameLength = 30;
        if (cardSchemeBrandName.length() > maxCardSchemeBrandNameLength) {
            logger.error(String.format("Card Scheme Brand Name no more than [%s] chars long, [%s] was provided with length of [%s].", maxCardSchemeBrandNameLength, cardSchemeBrandName, cardSchemeBrandName.length()));
            return false;
        }
        if (issuerName.length() < 2) {
            logger.error(String.format("Issuer Name should be at least 2 chars long, [%s] was provided.", issuerName));
            return false;
        }
        int maxIssuerNameLength = 80;
        if (issuerName.length() > maxIssuerNameLength) {
            logger.error(String.format("Issuer Name should be at no more than [%s] chars long, [%s] was provided with length of [%s].", maxIssuerNameLength, issuerName, issuerName.length()));
            return false;
        }
        if (issuerCountryCode.length() != 3) {
            logger.error(String.format("Issuer country code should be exactly three digits long but [%s] was provided.", issuerCountryCode));
            return false;
        }
        if (!cardClass.equals("C") && !cardClass.equals("D") && !cardClass.equals("P")) {
            logger.error(String.format("ED_YETED_YET: Card class is expected to be C, D or P but the value was [%s].", cardClass));
            return false;
        }
        if (cardholderCurrencyCode.length() != 3) {
            logger.error(String.format("Cardholder Currency Code should be exactly 3 chars long, [%s] was provided.", cardholderCurrencyCode));
            return false;
        }
        if (!dccFlag.isEmpty() && !dccFlag.equals("DCC allowed")) {
            logger.error(String.format("DCC Flag can only be empty or exactly [DCC allowed], [%s] was provided.", dccFlag));
            return false;
        }
        if (!anonymousPPMarker.equals("N") && !anonymousPPMarker.equals("E") && !anonymousPPMarker.equals("A") && !anonymousPPMarker.equals("U")) {
            logger.error(String.format("Anonymous Pre-Paid Flag can only contain N, E, A, or U. [%s] was provided.", anonymousPPMarker));
            return false;
        }
        if (!acceptsGamingOctPayments.isEmpty() && !acceptsGamingOctPayments.equals("Y") && !acceptsGamingOctPayments.equals("N")) {
            logger.error(String.format("Gaming OCT Payments can only contain Y, N, or be blank. [%s] was provided.", acceptsGamingOctPayments));
            return false;
        }
        if (!tokenisedFlag.isEmpty() && !tokenisedFlag.equals("Y")) {
            logger.error(String.format("Tokenised Flag can only contain Y or be blank. [%s] was provided.", tokenisedFlag));
            return false;
        }
        if (panLength > 19) {
            logger.error(String.format("Pan length is greater than 19, [%s] was provided.", panLengthStr));
            return false;
        }
        if (!fastFundsIndicator.isEmpty() && !fastFundsIndicator.equals("D") && !fastFundsIndicator.equals("N") && !fastFundsIndicator.equals("Y") && !fastFundsIndicator.equals("C")) {
            logger.error(String.format("Fast Funds Indicator can only contain D, N, Y, C, or be blank. [%s] was provided.", fastFundsIndicator));
            return false;
        }
        return true;
    }

    private boolean validateFinalRow(Map<String, String> currentRow) {
        String lastHeaderRecord = currentRow.get(FieldName.HEADER_RECORD);
        if (!lastHeaderRecord.equals("99")) {
            logger.error(String.format("Expected the last row to have a 99 header record as per the documentation, actual value was [%s].", lastHeaderRecord));
            return false;
        }
        return true;
    }

    private List<String> getLinesFromS3File(String s3BucketName, String candidateKey) {
        try {
            var getCandidateBinRangesRequest = RequestBuilder.getObjectRequest(s3BucketName, candidateKey);
            ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getCandidateBinRangesRequest);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {
                logger.debug("Reading file [%s]", candidateKey);
                return reader.lines().toList();
            }
        } catch (IOException e) {
            logger.error(String.format("NOT_TESTED_YET: Error reading file from S3: [%s]", e.getMessage()));
        }
        return null;
    }

    private MappingIterator<Map<String, String>> parseCsv(List<String> lines) {
        try {
            String CSV_DOC = String.join("\n", lines);
            CsvSchema schema = CsvSchema.builder()
                .addColumn(FieldName.HEADER_RECORD, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.BIN_LOWER_RANGE, CsvSchema.ColumnType.NUMBER)
                .addColumn(FieldName.BIN_UPPER_RANGE, CsvSchema.ColumnType.NUMBER)
                .addColumn(FieldName.PRODUCT_TYPE, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.CARD_SCHEME_BRAND_NAME, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.ISSUER_NAME, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.ISSUER_COUNTRY_CODE, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.ISSUER_COUNTRY_CODE_NUMERIC, CsvSchema.ColumnType.NUMBER)
                .addColumn(FieldName.ISSUER_COUNTRY_NAME, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.CARD_CLASS, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.CARDHOLDER_CURRENCY_CODE, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.DCC_FLAG, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.SCHEME_PRODUCT, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.ANON_PREPAID_MARKER, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.ACCEPTS_GAMING_OCT_PAYMENTS, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.TOKENISED_FLAG, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.PAN_LENGTH, CsvSchema.ColumnType.NUMBER)
                .addColumn(FieldName.FAST_FUNDS_INDICATOR, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_19, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_20, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_21, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_22, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_23, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_24, CsvSchema.ColumnType.STRING)
                .addColumn(FieldName.RESERVED_25, CsvSchema.ColumnType.STRING)
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
