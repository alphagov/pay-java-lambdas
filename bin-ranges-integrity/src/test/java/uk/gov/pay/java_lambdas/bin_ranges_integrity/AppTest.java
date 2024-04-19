package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTest {
    private ListAppender<ILoggingEvent> listAppender;
    @Mock
    private Context context;
    @Mock
    private S3Client mockS3Client;
    private MockedStatic<Constants> constantsMockedStatic;
    private MockedStatic<DependencyFactory> dependencyFactoryMockedStatic;

    @BeforeEach
    public void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(App.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        constantsMockedStatic = mockStatic(Constants.class);
        dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class);
        dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        when(context.getFunctionName()).thenReturn(this.getClass().getName());
        when(context.getFunctionVersion()).thenReturn("TEST");
    }

    @AfterEach
    public void tearDown() {
        constantsMockedStatic.close();
        dependencyFactoryMockedStatic.close();
    }

    private static Stream<Arguments> handleRequest_shouldValidateRanges() {
        return Stream.of(
            Arguments.of("candidate_acceptable.csv", true),
            Arguments.of("candidate_bad_record_type.csv", false),
            Arguments.of("candidate_unknown_product.csv", false),
            Arguments.of("candidate_unknown_scheme.csv", false),
            Arguments.of("candidate_lower_range_missing.csv", false),
            Arguments.of("candidate_upper_range_missing.csv", false),
            Arguments.of("candidate_garbled.csv", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void handleRequest_shouldValidateRanges(String candidateFilename, boolean expected) throws IOException {
        var candidatePath = Paths.get(format("src/test/resources/test-data/%s", candidateFilename)).toString();
        var promotedPath = Paths.get("src/test/resources/test-data/promoted.csv").toString();

        try (FileInputStream candidateFIS = new FileInputStream(candidatePath);
             FileInputStream promotedFIS = new FileInputStream(promotedPath)) {

            when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headObjRes(candidateFIS.available()))
                .thenReturn(headObjRes(promotedFIS.available()));

            when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(getObjRes(candidateFIS))
                .thenReturn(getObjRes(promotedFIS));

            App function = new App();
            Candidate candidate = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(candidate, context);
            assertEquals(expected, result.proceed());
        }
    }

    private static Stream<Arguments> handleRequest_shouldLogError_andReturnFalse_ifPercentageChange_isTooGreat() {
        return Stream.of(
            Arguments.of("candidate_too_large", "47.14"),
            Arguments.of("candidate_too_small", "50.84")
        );
    }

    @ParameterizedTest
    @MethodSource
    void handleRequest_shouldLogError_andReturnFalse_ifPercentageChange_isTooGreat(String filename, String percentageChange) throws IOException {
        var candidatePath = Paths.get(format("src/test/resources/test-data/%s.csv", filename)).toString();
        var promotedPath = Paths.get("src/test/resources/test-data/promoted.csv").toString();

        try (FileInputStream candidateFIS = new FileInputStream(candidatePath);
             FileInputStream promotedFIS = new FileInputStream(promotedPath)) {

            when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headObjRes(candidateFIS.available()))
                .thenReturn(headObjRes(promotedFIS.available()));

            App function = new App();
            Candidate candidate = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(candidate, context);
            List<ILoggingEvent> logsList = listAppender.list;
            assertFalse(result.proceed());
            assertTrue(logsList
                .getLast()
                .getFormattedMessage()
                .contains(format("Candidate outside of acceptable change percentage [actual: %s] [acceptable: 5.00]", percentageChange)));
        }
    }

    @Test
    void handleRequest_processesRealisticRangeSizes() throws IOException {
        var candidatePath = Paths.get("src/test/resources/test-data/realistic-ranges/BIN_V03_REDACTED_2.zip").toString();
        var promotedPath = Paths.get("src/test/resources/test-data/realistic-ranges/BIN_V03_REDACTED_1.zip").toString();
        
        try (
            FileInputStream candidateFIS = new FileInputStream(candidatePath);
            FileInputStream promotedFIS = new FileInputStream(promotedPath);
            ZipInputStream candidateZIS = new ZipInputStream(candidateFIS);
            ZipInputStream promotedZIS = new ZipInputStream(promotedFIS);
        ) {
            candidateZIS.getNextEntry();
            promotedZIS.getNextEntry();

            when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headObjRes(candidateFIS.available()))
                .thenReturn(headObjRes(promotedFIS.available()));

            when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(getObjRes(candidateZIS))
                .thenReturn(getObjRes(promotedZIS));

            App function = new App();
            Candidate candidate = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(candidate, context);
            assertTrue(result.proceed());
        }
    }

    // --- HELPER METHODS --- 

    private HeadObjectResponse headObjRes(long contentLength) {
        return HeadObjectResponse.builder()
            .contentLength(contentLength)
            .build();
    }

    private ResponseInputStream<GetObjectResponse> getObjRes(InputStream inputStream) {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(), inputStream);
    }
}
