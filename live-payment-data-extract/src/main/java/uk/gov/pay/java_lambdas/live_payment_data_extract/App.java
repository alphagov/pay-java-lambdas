package uk.gov.pay.java_lambdas.live_payment_data_extract;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.Pair;
import uk.gov.pay.java_lambdas.live_payment_data_extract.exception.PayNetworkException;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers.Service;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder.EventTicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.ADMINUSERS_URL;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.DATA_BUCKET;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.LEDGER_URL;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.UPDATE_SERVICES;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.USER_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.createIntervals;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.getDateString;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.getLastEventTSFromS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.getStringFromS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.putStringToS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.mungeStringArray;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.trimTrailingNewLine;

public class App implements RequestHandler<Object, Object> {
    private final SdkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CsvMapper csvMapper;
    private final CsvSchema eventTickerCsvSchema;
    private final CsvSchema serviceCsvSchema;

    private static final String TODAY = getDateString(Instant.now());
    private static final String EVENTS_CSV_FILE_NAME = format("%s-events.csv", TODAY);
    private static final String SERVICES_CSV_FILE_NAME = format("%s-services.csv", TODAY);
    private static final String TOTALS_CSV_FILE_NAME = format("%s-totals.csv", TODAY);
    private static final String GRAPH_TOTALS_CSV_FILE_NAME = format("%s-graph-totals.csv", TODAY);
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public App() {
        httpClient = DependencyFactory.httpClient();
        objectMapper = DependencyFactory.objectMapper();
        csvMapper = DependencyFactory.csvMapper();
        eventTickerCsvSchema = csvMapper.schemaFor(EventTicker.class).withoutHeader();
        serviceCsvSchema = csvMapper.schemaFor(Service.class).withoutHeader();
    }
    
    /*
    scheduled in 5 min intervals
    every 24 hours, update services
     */

    @Override
    public Object handleRequest(final Object input, final Context context) {
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        try {
            if (UPDATE_SERVICES) {
                logger.info("Updating services");
                retrieveServices();
            }
            retrieveEvents();
            retrieveTransactionTotals();
            retrieveGraphData();
        } catch (IOException | ExecutionException e) {
            throw new PayNetworkException(format("Problem extracting data: %s", e.getMessage()));
        } catch (InterruptedException e) {
            logger.error("Interrupted while extracting data: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        return input;
    }

    public void retrieveEvents() throws IOException, ExecutionException, InterruptedException {
        logger.debug("Retrieving events");

        Instant now = Instant.now();
        List<String> eventsCsvData = new ArrayList<>();
        Optional<String> serialisedEventsLog = getStringFromS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME, "\n");
        AtomicBoolean useTimeFromFile = new AtomicBoolean(false);

        /* 
        if previous events are available on S3, we use the last event timestamp recorded as our starting point when
        generating intervals for querying the ledger event ticker
        */
        serialisedEventsLog.ifPresentOrElse(
            events -> {
                eventsCsvData.add(trimTrailingNewLine(events));
                logger.info("Events log loaded for {}", TODAY);
                useTimeFromFile.set(true);
            },
            () -> logger.info("No previous events found for {}, defaulting to 5 minutes ago", TODAY)
        );

        List<Pair<String, String>> intervals = createIntervals(
            useTimeFromFile.get() ?
                getLastEventTSFromS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME) :
                now.minus(5L, ChronoUnit.MINUTES),
            now,
            Duration.of(30, ChronoUnit.SECONDS)
        );

        intervals.forEach(i -> {
            try {
                HttpExecuteRequest request = HttpExecuteRequest.builder()
                    .request(SdkHttpRequest.builder()
                        .uri(URI.create(LEDGER_URL))
                        .method(SdkHttpMethod.GET)
                        .appendRawQueryParameter("from_date", i.left())
                        .appendRawQueryParameter("to_date", i.right())
                        .appendRawQueryParameter("event_types", USER_APPROVED_FOR_CAPTURE)
                        .appendRawQueryParameter("event_types", USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL)
                        .build())
                    .build();

                logHttpCalls(request.httpRequest().getUri().toString());

                HttpExecuteResponse res = httpClient.prepareRequest(request)
                    .call();

                if (res.httpResponse().isSuccessful() && res.responseBody().isPresent()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.responseBody().get(), StandardCharsets.UTF_8));
                    ) {
                        String bodyContent = reader.lines().collect(Collectors.joining());
                        List<EventTicker> events = mapJsonArrayToList(bodyContent, objectMapper, EventTicker.class);
                        eventsCsvData.add(serializeToCsv(events, eventTickerCsvSchema, csvMapper));
                    }
                } else {
                    throw new IOException("Unsuccessful request to Ledger");
                }
            } catch (IOException e) {
                throw new PayNetworkException(e.getMessage());
            }
        });
        putStringToS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME, mungeStringArray(eventsCsvData));
        logger.debug("Finished retrieving events");
    }

    public void retrieveServices() throws IOException {

        try {
            HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(SdkHttpRequest.builder()
                    .uri(URI.create(ADMINUSERS_URL))
                    .method(SdkHttpMethod.GET)
                    .build())
                .build();

            logHttpCalls(request.httpRequest().getUri().toString());

            HttpExecuteResponse res = httpClient.prepareRequest(request)
                .call();

            if (res.httpResponse().isSuccessful() && res.responseBody().isPresent()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.responseBody().get(), StandardCharsets.UTF_8));
                ) {
                    String bodyContent = reader.lines().collect(Collectors.joining());
                    List<Service> services = mapJsonArrayToList(bodyContent, objectMapper, Service.class);
                    putStringToS3(DATA_BUCKET, SERVICES_CSV_FILE_NAME, serializeToCsv(services, serviceCsvSchema, csvMapper));
                }
            } else {
                throw new IOException("Unsuccessful request to AdminUsers");
            }
        } catch (IOException e) {
            throw new PayNetworkException(e.getMessage());
        }
        logger.debug("Finished retrieving services");
    }

    public void retrieveTransactionTotals() {
        // TODO
    }

    public void retrieveGraphData() {
        // TODO
    }


    private <T> List<T> mapJsonArrayToList(String jsonArray, ObjectMapper objectMapper, Class<T> clazz) throws JsonProcessingException {
        List<T> mappedObjects = objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        logger.info("Retrieved {} count: {}", clazz.getSimpleName(), mappedObjects.size());
        return mappedObjects;
    }

    private <T> String serializeToCsv(List<T> records, CsvSchema schema, CsvMapper csvMapper) throws IOException {
        ObjectWriter writer = csvMapper.writer(schema);
        String csvOutput = writer.writeValueAsString(records);
        return trimTrailingNewLine(csvOutput);
    }

    private void logHttpCalls(String uri) {
        logger.debug("Calling {}", uri);
    }
}

