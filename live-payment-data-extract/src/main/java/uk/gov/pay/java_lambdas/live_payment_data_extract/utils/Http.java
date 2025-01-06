package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Http {
    
    private Http() {
    }
    
    private static final Logger logger = LoggerFactory.getLogger(Http.class);
    
    public static HttpExecuteRequest getRequest (URI uri, Map<String, Object> queryParameters) {
        SdkHttpRequest.Builder requestBuilder = SdkHttpRequest.builder()
            .uri(uri)
            .method(SdkHttpMethod.GET);

        for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
            String key = entry.getKey();
            Object o = entry.getValue();

            switch (o) {
                case String value -> requestBuilder.appendRawQueryParameter(key, value);
                case String[] value -> {
                    for (String v : value) {
                        requestBuilder.appendRawQueryParameter(key, v);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported query parameter type: " + o);
            }
        }

        var request = HttpExecuteRequest.builder()
            .request(requestBuilder.build())
            .build();
        
        logger.debug("GET request to {}", request.httpRequest().getUri());
        return request;
    }

    public static <T> T handleResponse(HttpExecuteResponse res, Function<String, T> bodyProcessor) throws IOException {
        if (res.httpResponse().isSuccessful() && res.responseBody().isPresent()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.responseBody().get(), StandardCharsets.UTF_8))) {
                String bodyContent = reader.lines().collect(Collectors.joining());
                return bodyProcessor.apply(bodyContent);
            }
        } else {
            throw new IOException("Problem processing response: " + res.httpResponse());
        }
    }
}
