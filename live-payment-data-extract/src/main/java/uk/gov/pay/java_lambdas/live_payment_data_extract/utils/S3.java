package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ExpressionType;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RecordsEvent;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import uk.gov.pay.java_lambdas.live_payment_data_extract.DependencyFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class S3 {

    private S3() {
    }

    private static final Logger logger = LoggerFactory.getLogger(S3.class);
    private static final S3AsyncClient s3AsyncClient = DependencyFactory.s3AsyncClient();
    private static final S3Client s3Client = DependencyFactory.s3Client();

    public static Optional<String> getStringFromS3(String bucketName, String key, String delimiter) throws IOException {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        logger.debug("Retrieving data from S3: {}/{}", getObjectRequest.bucket(), getObjectRequest.key());

        try (ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getObjectRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectInputStream, StandardCharsets.UTF_8))) {
            logger.info("Received data from S3 [bucket: {}, key: {}, content length: {}]",
                getObjectRequest.bucket(),
                getObjectRequest.key(),
                s3ObjectInputStream.response().contentLength());
            return Optional.of(reader.lines().collect(Collectors.joining(delimiter)));
        } catch (NoSuchKeyException e) {
            logger.warn("Not found [bucket: {}, key: {}]", getObjectRequest.bucket(), getObjectRequest.key());
            return Optional.empty();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void putStringToS3(String bucketName, String key, String content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        RequestBody requestBody = RequestBody.fromString(content);
        requestBody.optionalContentLength().ifPresentOrElse(
            value -> {
                logger.info("Sent data to S3 [bucket: {}, key: {}, content length: {}]",
                    putObjectRequest.bucket(),
                    putObjectRequest.key(),
                    value);
                s3Client.putObject(putObjectRequest, requestBody);
            },
            () -> logger.error("Attempted to put empty file")
        );
    }

    public static Instant getLastEventTSFromS3(String bucketName, String key) throws ExecutionException, InterruptedException, IOException {
        String query = "SELECT _2 FROM S3Object s ORDER BY s._2 DESC LIMIT 1";

        SelectObjectContentRequest selectObjectContentRequest = SelectObjectContentRequest.builder()
            .bucket(bucketName)
            .key(key)
            .expression(query)
            .expressionType(ExpressionType.SQL)
            .inputSerialization(input -> input.csv(SdkBuilder::build))
            .outputSerialization(output -> output.csv(SdkBuilder::build))
            .build();

        var handler = new Handler();

        logger.debug("Executing operation: {}", query);
        s3AsyncClient.selectObjectContent(selectObjectContentRequest, handler).get();

        RecordsEvent response = (RecordsEvent) handler.receivedEvents.stream()
            .filter(e -> e.sdkEventType() == SelectObjectContentEventStream.EventType.RECORDS)
            .findFirst()
            .orElse(null);

        if (response != null) {
            return Instant.parse(response.payload().asUtf8String());
        } else {
            throw new IOException("Problem retrieving last timestamp from S3");
        }
    }

    private static class Handler implements SelectObjectContentResponseHandler {
        private final List<SelectObjectContentEventStream> receivedEvents = new ArrayList<>();

        @Override
        public void responseReceived(SelectObjectContentResponse response) {
            // empty
        }

        @Override
        public void onEventStream(SdkPublisher<SelectObjectContentEventStream> publisher) {
            publisher.subscribe(receivedEvents::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            logger.error("S3 select operation encountered a problem: {}", throwable.getMessage());
        }

        @Override
        public void complete() {
            logger.debug("S3 select operation completed");
        }
    }
}
