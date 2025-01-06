
package uk.gov.pay.java_lambdas.live_payment_data_extract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;

import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.AWS_REGION;

public class DependencyFactory {

    private DependencyFactory() {
    }

    public static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:3010")) // TODO remove this
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(AWS_REGION)
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .build();
    }

    public static S3Client s3Client() {
        return S3Client.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(AWS_REGION)
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .build();
    }

    public static S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(AWS_REGION)
            .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
            .build();
    }

    public static SdkHttpClient httpClient() {
        return AwsCrtHttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(3))
            .maxConcurrency(100)
            .build();
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public static CsvMapper csvMapper() {
        return CsvMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    }
}
