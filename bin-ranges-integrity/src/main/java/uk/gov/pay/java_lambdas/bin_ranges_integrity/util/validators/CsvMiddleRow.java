package uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({
    "headerRecord",
    "binLowerRange",
    "binUpperRange",
    "productType",
    "cardSchemeBrandName",
    "issuerName",
    "issuerCountryCode",
    "issuerCountryCodeNumeric",
    "issuerCountryName",
    "cardClass",
    "cardholderCurrencyCode",
    "dccFlag",
    "schemeProduct",
    "anonPrepaidMarker",
    "acceptsGamingOCTPayments",
    "tokenisedFile",
    "panLength",
    "fastFundsIndicator",
    "reserved19",
    "reserved20",
    "reserved21",
    "reserved22",
    "reserved23",
    "reserved24",
    "reserved25"
})
public class CsvMiddleRow extends CsvRow {
    @Pattern(regexp = "^01$", message = "Column A must have the value 01")
    private String headerRecord;
    
    @Digits(integer = 11, fraction = 0, message = "Lower BIN Range must be an 11 digit number")
    private int binLowerRange;
    
    @Digits(integer = 11, fraction = 0, message = "Upper BIN Range must be an 11 digit number")
    private int binUpperRange;
    
    @Pattern(regexp = "^CN|CP$", message = "Product type is expected to be CN or CP")
    private String productType;
    
    @Size(min = 2, max = 30, message = "Card Scheme Brand Name should be between 2 and 30 chars long")
    private String cardSchemeBrandName;

    @Size(max = 80, message = "Issuer Name should be between 2 and 30 chars long")
    private String issuerName;
    
    @Size(min = 3, max = 3, message = "Cardholder Currency Code should be exactly 3 chars long")
    private String issuerCountryCode;
    
    private String issuerCountryCodeNumeric;
    private String issuerCountryName;
    private String cardClass;
    private String cardholderCurrencyCode;
    
    @Pattern(regexp = "^[NEAU]?$", message = "Anonymous Pre-Paid Flag can only contain N, E, A, or U")
    private String dccFlag;
    
    private String schemeProduct;
    private String anonPrepaidMarker;
    
    @Pattern(regexp = "^[YN]?$", message = "Gaming OCT Payments can only contain Y, N, or be blank")
    private String acceptsGamingOCTPayments;

    @Pattern(regexp = "^Y?$", message = "Tokenised Flag can only contain Y or be blank")
    private String tokenisedFile;
    
    @Size(max = 19, message = "Pan length is greater than 19")
    private String panLength;
    
    @Pattern(regexp = "^[DNYC]?$", message = "Fast Funds Indicator can only contain D, N, Y, C, or be blank")
    private String fastFundsIndicator;
    private String reserved19;
    private String reserved20;
    private String reserved21;
    private String reserved22;
    private String reserved23;
    private String reserved24;
    private String reserved25;
}
