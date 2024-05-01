package uk.gov.pay.java_lambdas.bin_ranges_transfer.config;

import software.amazon.awssdk.regions.Region;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.model.Version;

import java.util.Optional;

import static java.lang.String.format;

public class Constants {

    private Constants() {
    }

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final String AWS_ACCOUNT_NAME = System.getenv("AWS_ACCOUNT_NAME");
    public static final String SFTP_DIRECTORY = "/Streamline/Universal";
    public static final String SFTP_FILE_PREFIX = "WP_341BIN_";
    public static final String S3_STAGED_BUCKET_NAME = format("bin-ranges-staged-%s", AWS_ACCOUNT_NAME);
    public static final String PASSPHRASE_PARAMETER_NAME = System.getenv("PASSPHRASE_PARAMETER_NAME");
    public static final String PRIVATE_KEY_PARAMETER_NAME = System.getenv("PRIVATE_KEY_PARAMETER_NAME");
    public static final Version WORLDPAY_FILE_VERSION = Version.fromEnvironment(
        Optional.ofNullable(System.getenv("WORLDPAY_FILE_VERSION")).orElse(Version.V03.name())
    );
    private static final int SFTP_PORT = Integer.parseInt(
        Optional.ofNullable(
            System.getenv("SFTP_PORT")
        ).orElse("22")
    );
    private static final String SFTP_HOST = Optional.ofNullable(System.getenv("SFTP_HOST")).orElse("mfg.worldpay.com");
    private static final String SFTP_USERNAME = Optional.ofNullable(System.getenv("SFTP_USERNAME")).orElse("MFG_MTCPGOVD");
    public static final boolean LOCALSTACK_ENABLED = Boolean.parseBoolean(System.getenv("LOCALSTACK_ENABLED"));

    /*
        using getters here allows us to mock static configuration constants in our tests
     */

    public static int getSftpPort() {
        return SFTP_PORT;
    }

    public static String getSftpHost() {
        return SFTP_HOST;
    }

    public static String getSftpUsername() {
        return SFTP_USERNAME;
    }
}
