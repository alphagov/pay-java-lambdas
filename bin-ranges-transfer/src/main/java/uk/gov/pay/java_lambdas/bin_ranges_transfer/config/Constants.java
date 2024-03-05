package uk.gov.pay.java_lambdas.bin_ranges_transfer.config;

import software.amazon.awssdk.regions.Region;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.model.Version;

public class Constants {

    private Constants() {
    }

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final String SFTP_DIRECTORY = "/Streamline/Universal";
    public static final String SFTP_FILE_PREFIX = "WP_341BIN_";
    public static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME");
    public static final String PASSPHRASE_PARAMETER_NAME = System.getenv("PASSPHRASE_PARAMETER_NAME");
    public static final String PRIVATE_KEY_PARAMETER_NAME = System.getenv("PRIVATE_KEY_PARAMETER_NAME");
    public static final Version WORLDPAY_FILE_VERSION = Version.fromString(System.getenv("WORLDPAY_FILE_VERSION"));
    private static final int SFTP_PORT = 22;
    private static final String SFTP_HOST = "mfg.worldpay.com";
    private static final String SFTP_USERNAME = "MFG_MTCPGOVD";

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
