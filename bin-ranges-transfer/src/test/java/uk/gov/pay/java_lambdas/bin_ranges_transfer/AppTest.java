package uk.gov.pay.java_lambdas.bin_ranges_transfer;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.exception.SshConnectionException;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;
import uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.DependencyFactory.SFTP_VERSION;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.TEST_SERVER_USERNAME;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.V03_FILENAME;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.loadData;

@ExtendWith(MockitoExtension.class)
class AppTest {
    @Mock
    private Context mockContext;
    @Mock
    private S3Client mockS3Client;
    @Mock
    private SsmClient mockSsmClient;
    private MockedStatic<Constants> constantsMockedStatic;
    private MockedStatic<DependencyFactory> dependencyFactoryMockedStatic;
    private LocalSftpServer localSftpServer;
    private String testPassphrase;

    @BeforeEach
    public void setUp() throws Exception {
        localSftpServer = new LocalSftpServer();
        localSftpServer.startServer();

        testPassphrase = loadData("/ssh-config/test_passphrase", "", getClass());
        String testPrivateKey = loadData("/ssh-config/test_private_key.rsa", "\n", getClass());

        constantsMockedStatic = mockStatic(Constants.class);
        dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class, CALLS_REAL_METHODS);

        constantsMockedStatic.when(Constants::getSftpPort).thenReturn(localSftpServer.getPort());
        constantsMockedStatic.when(Constants::getSftpHost).thenReturn(localSftpServer.getHost());
        dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        dependencyFactoryMockedStatic.when(DependencyFactory::ssmClient).thenReturn(mockSsmClient);

        when(mockContext.getFunctionName()).thenReturn(this.getClass().getName());
        when(mockContext.getFunctionVersion()).thenReturn("TEST");
        when(mockSsmClient.getParameter(any(GetParameterRequest.class)))
            .thenReturn(ssmParameterResponseBuilder(testPassphrase))
            .thenReturn(ssmParameterResponseBuilder(testPrivateKey));
    }

    @AfterEach
    public void tearDown() throws Exception {
        localSftpServer.stopServer();
        constantsMockedStatic.close();
        dependencyFactoryMockedStatic.close();
    }

    @Test
    void handleRequest_shouldProceed_ifBinRangesAreFound() throws GeneralSecurityException, IOException {
        constantsMockedStatic.when(Constants::getSftpUsername).thenReturn(TEST_SERVER_USERNAME);

        App function = new App();
        Candidate result = function.handleRequest(null, mockContext);
        assertTrue(result.s3Key().contains(V03_FILENAME));
        assertTrue(result.proceed());

        verify(mockS3Client, atMost(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        dependencyFactoryMockedStatic.verify(() -> DependencyFactory.sshClient(any(Path.class), eq(testPassphrase)));
    }

    @Test
    void handleRequest_shouldThrowError_ifNoBinRangesAreFound() throws IOException, GeneralSecurityException {
        constantsMockedStatic.when(Constants::getSftpUsername).thenReturn(TEST_SERVER_USERNAME);
        var simulatedDirEntries = List.of(
            new SftpClient.DirEntry("not_a_bin_range.csv", "", new SftpClient.Attributes()),
            new SftpClient.DirEntry("not_a_bin_range_2.csv", "", new SftpClient.Attributes()),
            new SftpClient.DirEntry("not_a_bin_range_3.csv", "", new SftpClient.Attributes())
        );
        dependencyFactoryMockedStatic.when(() -> DependencyFactory.sftpClient(any(ClientSession.class)))
            .thenAnswer((Answer<SftpClient>) invocation -> {
                ClientSession session = invocation.getArgument(0);
                SftpClient spySftpClient = spy(SftpClientFactory.instance().createSftpClient(session, SFTP_VERSION));
                doReturn(simulatedDirEntries).when(spySftpClient).readDir(any(SftpClient.Handle.class));
                return spySftpClient;
            });

        App function = new App();
        var sshConnectionException = assertThrows(SshConnectionException.class, () -> function.handleRequest(null, mockContext));
        assertTrue(sshConnectionException.getMessage().contains("No BIN ranges data found on server"));
    }

    @Test
    void handleRequest_shouldThrowError_ifSshConnectionFails() throws GeneralSecurityException, IOException {
        constantsMockedStatic.when(Constants::getSftpUsername).thenReturn("wrongUser");

        App function = new App();
        assertThrows(SshConnectionException.class, () -> function.handleRequest(null, mockContext));
    }

    private GetParameterResponse ssmParameterResponseBuilder(String parameterValue) {
        return GetParameterResponse.builder()
            .parameter(Parameter.builder()
                .value(parameterValue)
                .build())
            .build();
    }
}
