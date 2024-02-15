
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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {

    private DependencyFactory() {
    }
    
    public static SsmClient ssmClient() {
        return SsmClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.EU_WEST_1)
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .build();
    }

    public static S3Client s3Client() {
        return S3Client.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .region(Region.EU_WEST_1)
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .build();
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
            .createSftpClient(session, 3);
    }
}
