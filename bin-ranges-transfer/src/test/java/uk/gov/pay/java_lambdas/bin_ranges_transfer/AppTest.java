package uk.gov.pay.java_lambdas.bin_ranges_transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.helper.MockSftpServer.TEST_SERVER_USERNAME;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.helper.MockSftpServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

@ExtendWith(MockitoExtension.class)
class AppTest {

    @Mock
    private Context context;
    private MockSftpServer mockSftpServer;
    private String testPassphrase;
    private String testPrivateKey;

    @BeforeEach
    public void setUp() throws Exception {
        when(context.getFunctionName()).thenReturn(this.getClass().getName());
        when(context.getFunctionVersion()).thenReturn("TEST");
        testPrivateKey = new String(Files.readAllBytes(
            Path.of(ClassLoader.getSystemResource("ssh/test_private_key.rsa").getPath()))
        );
        testPassphrase = String.join("", Files.readAllLines(
            Path.of(ClassLoader.getSystemResource("ssh/test_passphrase").getPath())
        ));
        mockSftpServer = new MockSftpServer();
        mockSftpServer.startServer();
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockSftpServer.stopServer();
    }

    @Test
    void handleRequest_shouldSuccessValue() throws GeneralSecurityException, IOException {
        S3Client mockS3Client = mock(S3Client.class);
        SsmClient mockSsmClient = mock(SsmClient.class);
        
        try (MockedStatic<DependencyFactory> dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class, CALLS_REAL_METHODS);
             MockedStatic<Constants> constantsMockedStatic = mockStatic(Constants.class)) {
            
            dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
            dependencyFactoryMockedStatic.when(DependencyFactory::ssmClient).thenReturn(mockSsmClient);
            constantsMockedStatic.when(Constants::getSftpPort).thenReturn(mockSftpServer.getPort());
            constantsMockedStatic.when(Constants::getSftpHost).thenReturn(mockSftpServer.getHost());
            constantsMockedStatic.when(Constants::getSftpUsername).thenReturn(TEST_SERVER_USERNAME);

            when(mockSsmClient.getParameter(any(GetParameterRequest.class)))
                .thenReturn(ssmParameterResponseBuilder(testPassphrase))
                .thenReturn(ssmParameterResponseBuilder(testPrivateKey));

            App function = new App();
            String result = function.handleRequest(null, context);
            assertEquals("Success: downloaded BIN ranges from Worldpay", result);

            verify(mockS3Client, atMost(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            dependencyFactoryMockedStatic.verify(DependencyFactory::s3Client);
            dependencyFactoryMockedStatic.verify(DependencyFactory::ssmClient);
            dependencyFactoryMockedStatic.verify(() -> DependencyFactory.sshClient(any(Path.class), eq(testPassphrase)));
        }
    }

    private GetParameterResponse ssmParameterResponseBuilder(String parameterValue) {
        return GetParameterResponse.builder()
            .parameter(Parameter.builder()
                .value(parameterValue)
                .build())
            .build();
    }
}
