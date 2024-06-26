
package uk.gov.pay.java_lambdas.bin_ranges_transfer;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;

import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.AWS_REGION;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.LOCALSTACK_ENABLED;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {
    private static final Logger logger = LoggerFactory.getLogger(DependencyFactory.class);
    public static final int SFTP_VERSION = 3;

    private DependencyFactory() {
    }
    
    public static SsmClient ssmClient() {
        if (LOCALSTACK_ENABLED) {
            logger.debug("Localstack environment detected");
            return SsmClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(AWS_REGION)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .endpointOverride(URI.create(System.getenv("AWS_ENDPOINT_URL")))
                .build();
        } else {
            return SsmClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(AWS_REGION)
                .httpClientBuilder(AwsCrtHttpClient.builder())
                .build();
        }
    }

    public static S3Client s3Client() {
        if (LOCALSTACK_ENABLED) {
            S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
            return S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(AWS_REGION)
                .serviceConfiguration(s3Config)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .endpointOverride(URI.create(System.getenv("AWS_ENDPOINT_URL")))
                .build();
        } else {
            return S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(AWS_REGION)
                .httpClientBuilder(AwsCrtHttpClient.builder())
                .build();
        }
    }

    public static SshClient sshClient(Path privateKeyPath, String passphrase) throws GeneralSecurityException, IOException {
        KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
        Collection<KeyPair> keys = loader.loadKeyPairs(null, privateKeyPath, FilePasswordProvider.of(passphrase));

        SshClient sshClient = ClientBuilder.builder()
            .serverKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE)
            .build();

        sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keys));

        return sshClient;
    }

    public static SftpClient sftpClient(ClientSession session) throws IOException {
        return SftpClientFactory.instance()
            .createSftpClient(session, SFTP_VERSION);
    }
}
