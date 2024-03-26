package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.AWS_REGION;

public class DependencyFactory {

    private DependencyFactory() {
    }

    public static S3Client s3Client() {
        return S3Client.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(AWS_REGION)
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .build();
    }

    public static Logger logger() {
        return LoggerFactory.getLogger(App.class);
    }

    
}
