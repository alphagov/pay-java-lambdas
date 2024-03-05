package uk.gov.pay.java_lambdas.bin_ranges_diff;


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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.pay.java_lambdas.bin_ranges_diff.config.Constants;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTest {

    @Mock
    private Context context;

    @Mock
    private S3Client mockS3Client;
    private MockedStatic<Constants> constantsMockedStatic;
    private MockedStatic<DependencyFactory> dependencyFactoryMockedStatic;

    @BeforeEach
    public void setUp() {
        constantsMockedStatic = mockStatic(Constants.class);
        dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class, CALLS_REAL_METHODS);
        dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        when(context.getFunctionName()).thenReturn(this.getClass().getName());
        when(context.getFunctionVersion()).thenReturn("TEST");
    }

    @AfterEach
    public void tearDown() {
        constantsMockedStatic.close();
        dependencyFactoryMockedStatic.close();
    }

    @ParameterizedTest
    @MethodSource
    void handleRequest_shouldDetermineWhenSHAsAreDifferent(String input, boolean expected) throws IOException {
        String candidateDataPath = Paths.get("src/test/resources/testData", format("candidate_%s.csv", input)).toString();
        String promotedDataPath = Paths.get("src/test/resources/testData", format("promoted_%s.csv", input)).toString();


        try (FileInputStream candidateFIS = new FileInputStream(candidateDataPath);
             FileInputStream promotedFIS = new FileInputStream(promotedDataPath)) {

            ResponseInputStream<GetObjectResponse> candidateResponseInputStream = new ResponseInputStream<>(GetObjectResponse.builder().build(), candidateFIS);
            ResponseInputStream<GetObjectResponse> promotedResponseInputStream = new ResponseInputStream<>(GetObjectResponse.builder().build(), promotedFIS);

            when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(candidateResponseInputStream)
                .thenReturn(promotedResponseInputStream);

            App function = new App();
            Candidate c = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(c, context);
            assertEquals(expected, result.proceed());

            verify(mockS3Client, atMost(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            dependencyFactoryMockedStatic.verify(DependencyFactory::s3Client);
        }
    }

    private static Stream<Arguments> handleRequest_shouldDetermineWhenSHAsAreDifferent() {
        return Stream.of(
            Arguments.of("same", false),
            Arguments.of("diff", true)
        );
    }
}
