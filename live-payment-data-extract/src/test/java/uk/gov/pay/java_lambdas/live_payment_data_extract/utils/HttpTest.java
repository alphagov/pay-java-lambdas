package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpTest {

    @Test
    void handleResponse_SuccessfulResponse() throws IOException {
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class);
        SdkHttpResponse mockHttpResponse = mock(SdkHttpResponse.class);
        
        String testBody = "{\"name\":\"Test\"}";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testBody.getBytes(StandardCharsets.UTF_8));

        when(mockResponse.httpResponse()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.responseBody()).thenReturn(Optional.of(AbortableInputStream.create(inputStream)));

        Function<String, TestObject> bodyProcessor = body -> new TestObject(body);

        TestObject result = Http.handleResponse(mockResponse, bodyProcessor);

        assertNotNull(result);
        assertEquals(testBody, result.content);
        
        verify(mockResponse, times(1)).httpResponse();
        verify(mockResponse, times(2)).responseBody();
    }

    @Test
    void handleResponse_UnsuccessfulResponse() {
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class);
        SdkHttpResponse mockHttpResponse = mock(SdkHttpResponse.class);

        when(mockResponse.httpResponse()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.isSuccessful()).thenReturn(false);

        Function<String, TestObject> bodyProcessor = body -> new TestObject(body);

        assertThrows(IOException.class, () -> {
            Http.handleResponse(mockResponse, bodyProcessor);
        });
    }

    private static class TestObject {
        String content;
        TestObject(String content) {
            this.content = content;
        }
    }
}
