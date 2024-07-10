package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import java.util.List;

public class Strings {
    private Strings() {
    }

    public static String mungeStringArray(List<String> strings) {
        return String.join("\n", strings);
    }
    
    public static String trimTrailingNewLine(String string) {
        if (string.endsWith("\n")) {
            return string.substring(0, string.length() - 1);
        } else {
            return string;
        }
    }
}
