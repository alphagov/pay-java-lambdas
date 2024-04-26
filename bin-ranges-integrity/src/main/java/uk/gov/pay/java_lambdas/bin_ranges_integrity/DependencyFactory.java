package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.AWS_REGION;
import static uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants.LOCALSTACK_ENABLED;

public class DependencyFactory {
    private static final Logger logger = LoggerFactory.getLogger(DependencyFactory.class);

    private DependencyFactory() {}

    public static S3Client s3Client() {
        if (LOCALSTACK_ENABLED) {
            logger.debug("Localstack environment detected");
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
}
