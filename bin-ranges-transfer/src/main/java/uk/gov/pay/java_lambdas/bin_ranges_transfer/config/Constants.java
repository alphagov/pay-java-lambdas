package uk.gov.pay.java_lambdas.bin_ranges_transfer.config;

public class Constants {
    
    private Constants() {
    }
    
    public static final String AWS_ACCOUNT_NAME = System.getenv("AWS_ACCOUNT_NAME");
    public static final String SFTP_DIRECTORY = "/Streamline/Universal";
    public static final String SFTP_FILE_PREFIX = "WP_341BIN_";
    public static final String S3_BUCKET_NAME = "bin-ranges-staging";
    public static final String PASSPHRASE_PARAMETER_NAME = System.getenv("PASSPHRASE_PARAMETER_NAME");
    public static final String PRIVATE_KEY_PARAMETER_NAME = System.getenv("PRIVATE_KEY_PARAMETER_NAME");
    private static final int SFTP_PORT = 22;
    private static final String SFTP_HOST = "mfg.worldpay.com";
    private static final String SFTP_USERNAME = "MFG_MTCPGOVD";


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
