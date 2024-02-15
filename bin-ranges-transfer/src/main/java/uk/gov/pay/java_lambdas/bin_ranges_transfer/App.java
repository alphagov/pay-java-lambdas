package uk.gov.pay.java_lambdas.bin_ranges_transfer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.model.SftpDownload;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.AWS_ACCOUNT_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.PASSPHRASE_PARAMETER_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.PRIVATE_KEY_PARAMETER_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.S3_BUCKET_NAME;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.SFTP_DIRECTORY;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.SFTP_FILE_PREFIX;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.getSftpHost;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.getSftpPort;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.config.Constants.getSftpUsername;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.createTempPrivateKeyFile;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractDateString;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.parameterRequestWithDecryption;

public class App implements RequestHandler<Void, Object> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private final S3Client s3Client;
    private final SshClient sshClient;
    private final SsmClient ssmClient;

    public App() throws GeneralSecurityException, IOException {
        s3Client = DependencyFactory.s3Client();
        ssmClient = DependencyFactory.ssmClient();
        HashMap<String, String> params = getSsmParameters();
        sshClient = DependencyFactory.sshClient(
            createTempPrivateKeyFile(params.get("privateKey")),
            params.get("passphrase")
        );
    }

    @Override
    public String handleRequest(Void input, final Context context) {
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        sshClient.start();
        try {
            ClientSession session = sshClient.connect(getSftpUsername(), getSftpHost(), getSftpPort())
                .verify()
                .getSession();

            session.auth().verify(10, TimeUnit.SECONDS);

            try (SftpClient sftpClient = DependencyFactory.sftpClient(session)) {

                SftpClient.Handle handle = sftpClient.openDir(SFTP_DIRECTORY);
                List<SftpClient.DirEntry> dirEntries = sftpClient.readDir(handle);

                List<SftpDownload> downloadList = dirEntries.stream()
                    .filter(entry -> entry.getFilename().startsWith(SFTP_FILE_PREFIX))
                    .map(entry -> new SftpDownload(
                        String.format("%s/%s", SFTP_DIRECTORY, entry.getFilename()),
                        entry.getFilename(),
                        extractDateString(entry.getFilename()),
                        entry.getAttributes().getSize()))
                    .toList();
                if (!downloadList.isEmpty()) { 
                    streamFilesToS3(sftpClient, downloadList); 
                } else {
                    throw new IOException();
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            return "Error: failed to download BIN ranges from Worldpay";
        } finally {
            sshClient.stop();
        }
        return "Success: downloaded BIN ranges from Worldpay";
    }

    private void streamFilesToS3(SftpClient sftpClient, List<SftpDownload> downloadList) {
        String s3BucketName = String.format("%s-%s", S3_BUCKET_NAME, AWS_ACCOUNT_NAME);
        downloadList.forEach(sftpDownload -> {
            try (InputStream inputStream = sftpClient.read(sftpDownload.filePath())) {
                
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(sftpDownload.getS3Key())
                    .build();
                
                logger.debug("S3 key: {}", sftpDownload.getS3Key());

                RequestBody requestBody = RequestBody.fromInputStream(inputStream, sftpDownload.size());

                s3Client.putObject(putObjectRequest, requestBody);
                logger.info("{} streamed and uploaded successfully to {}", sftpDownload.fileName(), s3BucketName);
            } catch (Exception e) {
                logger.error("Error streaming file from SFTP to S3: {}", e.getMessage());
            }
        });
    }

    private HashMap<String, String> getSsmParameters() {
        GetParameterResponse passphraseResponse = ssmClient.getParameter(
            parameterRequestWithDecryption(PASSPHRASE_PARAMETER_NAME)
        );
        GetParameterResponse privateKeyResponse = ssmClient.getParameter(
            parameterRequestWithDecryption(PRIVATE_KEY_PARAMETER_NAME)
        );
        
        Map<String, String> iMap = Map.of("passphrase", passphraseResponse.parameter().value(),
            "privateKey", privateKeyResponse.parameter().value());

        return new HashMap<>(iMap);
    }
}
