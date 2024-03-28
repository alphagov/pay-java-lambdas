
package uk.gov.pay.java_lambdas.bin_ranges_promotion;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

import static uk.gov.pay.java_lambdas.bin_ranges_promotion.config.Constants.AWS_REGION;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of S3AsyncClient
     */
    public static S3Client s3Client() {
        return S3Client.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(AWS_REGION)
                       .httpClientBuilder(AwsCrtHttpClient.builder())
                       .build();
    }
}
