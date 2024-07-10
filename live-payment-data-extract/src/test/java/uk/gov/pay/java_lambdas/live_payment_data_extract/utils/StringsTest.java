package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.mungeStringArray;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.trimTrailingNewLine;

class StringsTest {

    @Test
    void shouldCombineStringArray() {
        var strings = List.of("I'm a string", "I'm a string", "I'm a string");

        var result = mungeStringArray(strings);
        assertEquals("""
                I'm a string
                I'm a string
                I'm a string""", result);
    }
    
    @Test
    void shouldRemoveTrailingNewLine() {
        var testCase = "line\nline\nline\nline\n";
        var result = trimTrailingNewLine(testCase);
        
        assertEquals("""
                line
                line
                line
                line
                """, testCase);
        assertEquals("""
                line
                line
                line
                line""", result);
        assertNotEquals("""
                line
                line
                line
                line
                """, result); // note text block closing quotes on new line
    }

}
