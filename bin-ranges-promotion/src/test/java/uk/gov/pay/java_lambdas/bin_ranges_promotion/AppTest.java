package uk.gov.pay.java_lambdas.bin_ranges_promotion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class AppTest {
    @Mock
    private Context mockContext;
    @Mock
    private S3Client mockS3Client;
    private MockedStatic<DependencyFactory> dependencyFactoryMockedStatic;
    @Captor
    private ArgumentCaptor<CopyObjectRequest> argumentCaptor;
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
    
    @BeforeEach
    public void setUp() {
        dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class);
        dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        when(mockContext.getFunctionName()).thenReturn(this.getClass().getName());
        when(mockContext.getFunctionVersion()).thenReturn("TEST");
    }
    
    @AfterEach
    public void tearDown() {
        dependencyFactoryMockedStatic.close();
    }
    
    @Test
    void handleRequest_shouldReturnConstantValue() throws NoSuchAlgorithmException {
        when(mockS3Client.copyObject(any(CopyObjectRequest.class)))
            .thenReturn(copyObjectResponseBuilder("promoted"))
            .thenReturn(copyObjectResponseBuilder("candidate"));
        App function = new App();
        var c = new Candidate("an/s3/key.csv", true);
        var result = function.handleRequest(c, mockContext);
        assertTrue(result);
        verify(mockS3Client, times(2)).copyObject(argumentCaptor.capture());
    }

    private CopyObjectResponse copyObjectResponseBuilder(String value) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(value.getBytes(StandardCharsets.UTF_8));
        String base64Sha = BASE_64_ENCODER.encodeToString(messageDigest.digest());
        return CopyObjectResponse.builder()
            .copyObjectResult(CopyObjectResult.builder()
                .checksumSHA256(base64Sha)
                .build())
            .build();
    }
}
