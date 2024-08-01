package uk.gov.pay.java_lambdas.live_payment_data_extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RecordsEvent;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class AppTest {

    @Mock
    private Context mockContext;
    @Mock
    private SdkHttpClient mockHttpClient;
    @Mock
    private ExecutableHttpRequest mockHttpRequest;
    @Mock
    private S3AsyncClient mockS3AsyncClient;
    @Mock
    private S3Client mockS3Client;
    @Mock
    private SelectObjectContentResponse selectObjectContentResponse;
    @Mock
    private SdkPublisher<SelectObjectContentEventStream> mockSdkPublisher;
    @Mock
    private RecordsEvent mockRecordsEvent;

    private MockedStatic<DependencyFactory> mockDependencyFactory;

    @BeforeEach
    void setUp() {
        mockDependencyFactory = mockStatic(DependencyFactory.class, CALLS_REAL_METHODS);
        mockDependencyFactory.when(DependencyFactory::httpClient).thenReturn(mockHttpClient);
        mockDependencyFactory.when(DependencyFactory::s3AsyncClient).thenReturn(mockS3AsyncClient);
        mockDependencyFactory.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        when(mockContext.getFunctionName()).thenReturn(this.getClass().getName());
        when(mockContext.getFunctionVersion()).thenReturn("TEST");
    }

    @AfterEach
    void tearDown() {
        mockDependencyFactory.close();
        reset();
    }

    @Test
    void handleRequest_shouldReturnConstantValue() throws IOException {
        String latestTimeStampResponse = Instant.now().minusSeconds(120L).toString();
        String servicesResponse = loadFromFile("src/test/resources/servicesFromLedger.json");
        String eventsResponse = loadFromFile("src/test/resources/eventsFromLedger.json");

        RequestBody expectedServicesRequestBody = requestBody(loadFromFile("src/test/resources/expected/services.csv"));
        RequestBody expectedEventsRequestBody = requestBody(loadFromFile("src/test/resources/expected/events.csv"));
        
        mockSelectObjectResponse(latestTimeStampResponse);
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(getObjectResponse(loadFromFile("src/test/resources/eventsFromS3.csv")));
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(mockHttpRequest);
        when(mockHttpRequest.call()).thenReturn(mockHttpResponse(servicesResponse))
            .thenAnswer(invocation -> mockHttpResponse(eventsResponse)
        );
        
        App function = new App();
        function.handleRequest("echo", mockContext);

        verify(mockS3Client, atMost(1)).putObject(any(PutObjectRequest.class), eq(expectedServicesRequestBody));
        verify(mockS3Client, atMost(1)).putObject(any(PutObjectRequest.class), eq(expectedEventsRequestBody));
    }

    // --- PRIVATE ---

    private HttpExecuteResponse mockHttpResponse(String jsonResponse) {
        return HttpExecuteResponse.builder()
            .response(SdkHttpResponse.builder().statusCode(200).build())
            .responseBody(AbortableInputStream.create(new ByteArrayInputStream(jsonResponse.getBytes())))
            .build();
    }

    private void mockSelectObjectResponse(String latestTimeStampResponse) {
        when(mockS3AsyncClient.selectObjectContent(any(SelectObjectContentRequest.class), any(SelectObjectContentResponseHandler.class)))
            .thenAnswer(invocation -> {
                SelectObjectContentResponseHandler handler = invocation.getArgument(1);
                handler.responseReceived(selectObjectContentResponse);
                handler.onEventStream(mockSdkPublisher);
                handler.complete();
                return CompletableFuture.completedFuture(null);
            });

        doAnswer(invocation -> {
            Consumer<SelectObjectContentEventStream> consumer = invocation.getArgument(0);
            consumer.accept(mockRecordsEvent);
            return null;
        }).when(mockSdkPublisher).subscribe(any(Consumer.class));

        when(mockRecordsEvent.sdkEventType()).thenReturn(SelectObjectContentEventStream.EventType.RECORDS);
        when(mockRecordsEvent.payload()).thenReturn(SdkBytes.fromUtf8String(latestTimeStampResponse));
    }

    private ResponseInputStream<GetObjectResponse> getObjectResponse(String data) {
        InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        return new ResponseInputStream<>(GetObjectResponse.builder()
            .contentLength((long) data.getBytes().length)
            .build(), is);
    }
    
    private RequestBody requestBody(String content) {
        return RequestBody.fromString(content);
    }

    private String loadFromFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }
}
