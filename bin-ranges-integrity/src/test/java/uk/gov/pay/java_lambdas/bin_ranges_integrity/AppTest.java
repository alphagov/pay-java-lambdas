package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.extension.ExtendWith;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

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

    @Test
    void handleRequest_baselineShouldProceed() throws IOException {
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headObjRes(1000L))
            .thenReturn(headObjRes(1000L));
        
        var path = Paths.get("src/test/resources/testData/candidate_baseline.csv").toString();

        try (FileInputStream candidateFIS = new FileInputStream(path);
             FileInputStream promotedFIS = new FileInputStream(path)) {

            when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(getObjRes(candidateFIS))
                .thenReturn(getObjRes(promotedFIS));

            App function = new App();
            Candidate candidate = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(candidate, context);
            assertTrue(result.proceed());
        }
    }

    @Test
    void handleRequest_shouldLogError_ifFileSizeUnexpected() throws IOException {
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headObjRes(1000L))
            .thenReturn(headObjRes(1060L)); // 6% increase

        App function = new App();
        Candidate candidate = new Candidate("/an/s3/key.csv", true);
        Candidate result = function.handleRequest(candidate, context);
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(result.proceed());
        assertTrue(logsList.getLast().getFormattedMessage().contains("Error: Candidate outside of acceptable change percentage [actual: 6.00], [acceptable: 5.00]"));
    }
    
    private HeadObjectResponse headObjRes(long contentLength) {
        return HeadObjectResponse.builder()
            .contentLength(contentLength)
            .build();
    }

    private ResponseInputStream<GetObjectResponse> getObjRes(FileInputStream fileInputStream) {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(), fileInputStream);
    }
}
