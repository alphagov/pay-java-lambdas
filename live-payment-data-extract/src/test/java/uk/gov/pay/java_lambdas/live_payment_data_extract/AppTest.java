package uk.gov.pay.java_lambdas.live_payment_data_extract;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.EVENT_TICKER_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.PERFORMANCE_REPORT_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.SERVICES_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.TIME_SERIES_REPORT_ENDPOINT;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
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
import uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants;

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

    @RegisterExtension
    static WireMockExtension ledgerMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .dynamicPort())
        .build();

    @RegisterExtension
    static WireMockExtension adminUsersMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .dynamicPort())
        .build();

    @Mock
    private Context mockContext;
    @Mock
    private S3AsyncClient mockS3AsyncClient;
    @Mock
    private S3Client mockS3Client;
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    @Mock
    private SelectObjectContentResponse selectObjectContentResponse;
    @Mock
    private SdkPublisher<SelectObjectContentEventStream> mockSdkPublisher;
    @Mock
    private RecordsEvent mockRecordsEvent;

    private MockedStatic<DependencyFactory> mockDependencyFactory;
    private MockedStatic<Constants> mockConstants;

    @BeforeEach
    void setUp() throws IOException {
        mockConstants = mockStatic(Constants.class, CALLS_REAL_METHODS);
        mockDependencyFactory = mockStatic(DependencyFactory.class, CALLS_REAL_METHODS);
        
        mockConstants.when(Constants::getLedgerUrl).thenReturn(ledgerMock.baseUrl());
        mockConstants.when(Constants::getAdminUsersUrl).thenReturn(adminUsersMock.baseUrl());
        
        mockDependencyFactory.when(DependencyFactory::s3AsyncClient).thenReturn(mockS3AsyncClient);
        mockDependencyFactory.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
//        mockDependencyFactory.when(DependencyFactory::dynamoDbClient).thenReturn(mockDynamoDbClient); TODO: this is commented out to allow for local host dynamodb testing, tests might fail in CI
        
        when(mockContext.getFunctionName()).thenReturn(this.getClass().getName());
        when(mockContext.getFunctionVersion()).thenReturn("TEST");
        
        ledgerMock.stubFor(get(urlPathEqualTo(EVENT_TICKER_ENDPOINT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFromFile("src/test/resources/eventsFromLedger.json"))));

        ledgerMock.stubFor(get(urlPathEqualTo(PERFORMANCE_REPORT_ENDPOINT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFromFile("src/test/resources/performanceReportFromLedger.json"))));

        ledgerMock.stubFor(get(urlPathEqualTo(TIME_SERIES_REPORT_ENDPOINT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFromFile("src/test/resources/timeSeriesReportFromLedger.json"))));

        adminUsersMock.stubFor(get(urlPathEqualTo(SERVICES_ENDPOINT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFromFile("src/test/resources/servicesFromAdminUsers.json"))));
    }

    @AfterEach
    void tearDown() {
        mockDependencyFactory.close();
        reset();
    }

    @Test
    void handleRequest_shouldReturnConstantValue() throws IOException {
        String latestTimeStampResponse = Instant.now().minusSeconds(120L).toString();

        RequestBody expectedServicesRequestBody = requestBody(loadFromFile("src/test/resources/expected/services.csv"));
        RequestBody expectedEventsRequestBody = requestBody(loadFromFile("src/test/resources/expected/events.csv"));

        mockSelectObjectResponse(latestTimeStampResponse);

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(getObjectResponse(loadFromFile("src/test/resources/eventsFromS3.csv")));

        App function = new App();
        function.handleRequest("echo", mockContext);

        verify(mockS3Client, atMost(1)).putObject(any(PutObjectRequest.class), eq(expectedServicesRequestBody));
        verify(mockS3Client, atMost(1)).putObject(any(PutObjectRequest.class), eq(expectedEventsRequestBody));
    }

    // --- PRIVATE ---

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
