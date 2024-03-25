package uk.gov.pay.java_lambdas.bin_ranges_integrity;


import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.pay.java_lambdas.bin_ranges_integrity.config.Constants;
import uk.gov.pay.java_lambdas.common.bin_ranges.dto.Candidate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTest {
    @Mock
    private Context context;
    @Mock
    private S3Client mockS3Client;
    @Mock
    private Logger mockLogger;
    private MockedStatic<Constants> constantsMockedStatic;
    private MockedStatic<DependencyFactory> dependencyFactoryMockedStatic;

    @BeforeEach
    public void setUp() {
        constantsMockedStatic = mockStatic(Constants.class);
        dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class);
        dependencyFactoryMockedStatic.when(DependencyFactory::s3Client).thenReturn(mockS3Client);
        dependencyFactoryMockedStatic.when(DependencyFactory::logger).thenReturn(mockLogger);
        when(context.getFunctionName()).thenReturn(this.getClass().getName());
        when(context.getFunctionVersion()).thenReturn("TEST");
    }

    @AfterEach
    public void tearDown() {
        constantsMockedStatic.close();
        dependencyFactoryMockedStatic.close();
    }

    @ParameterizedTest
    @CsvSource({
        "baseline,true,<null>,-1", 
        "massive,false,'Contents of candidate is too large, candidate is [653] lines long, promoted is [33] lines long',-1",
        "tiny,false,'Contents of candidate is too short, candidate is [4] lines long, promoted is [33] lines long',-1",
        "acceptably-large,true,<null>,-1",
        "acceptably-small,true,<null>,-1",
        "just-too-large,false,'Contents of candidate is too large, candidate is [35] lines long, promoted is [33] lines long',-1",
        "just-too-small,false,'Contents of candidate is too short, candidate is [31] lines long, promoted is [33] lines long',-1",
        "malformed-missing-binrange-start,false,'Expected bin lower range information to be 18 digits long but [] was [0] digits long',24",
        "malformed-missing-binrange-end,false,'Expected bin upper range information to be 18 digits long but [] was [0] digits long',27",
        "missing-first-row,false,'Expected the first row to have a 00 header record as per the documentation, actual value was [01].',1",
        "missing-last-row,false,'Expected the last row to have a 99 header record as per the documentation, actual value was [01].',32",
        "wrong-header,false,'Expected bin data rows to have a 01 header record as per the documentation, actual value was [04].',18",
        "bin-range-wrong-order,false,'Expected bin start to be a lower number than bin end.  Start: [410873333999999999], End: [410873333000000000].',5",
        "wrong_product_type,false,'Product type is expected to be CN or CP but the value was [CA].',18",
        "wrong_short_scheme_brand_name,false,'Card Scheme Brand Name should be at least 2 chars long, [X] was provided.',14",
        "wrong_long_scheme_brand_name,false,'Card Scheme Brand Name no more than [30] chars long, [ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ] was provided with length of [52].',14",
        "wrong_short_issuer_name,false,'Issuer Name should be at least 2 chars long, [A] was provided.',7",
        "wrong_long_issuer_name,false,'Issuer Name should be at no more than [80] chars long, [ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ] was provided with length of [130].',7",
        "wrong_short_country_code,false,'Issuer country code should be exactly three digits long but [AB] was provided.',19",
        "wrong_long_country_code,false,'Issuer country code should be exactly three digits long but [ABCD] was provided.',19",
        "wrong_short_currency_code,false,'Cardholder Currency Code should be exactly 3 chars long, [XY] was provided.',8",
        "wrong_long_currency_code,false,'Cardholder Currency Code should be exactly 3 chars long, [WXYZ] was provided.',8",
        "wrong_dcc,false,'DCC Flag can only be empty or exactly [DCC allowed], [DC2 allowed] was provided.',32",
        "wrong_anonymous_marker,false,'Anonymous Pre-Paid Flag can only contain N, E, A, or U. [X] was provided.',23",
        "wrong_gaming_oct,false,'Gaming OCT Payments can only contain Y, N, or be blank. [Z] was provided.',28",
        "wrong_token_flag,false,'Tokenised Flag can only contain Y or be blank. [Z] was provided.',16",
        "wrong_long_pan,false,'Pan length is greater than 19, [20] was provided.',20",
        "wrong_fast_funds,false,'Fast Funds Indicator can only contain D, N, Y, C, or be blank. [A] was provided.',20",
    })

    void handleRequest_baselineShouldProceed(String input, boolean expectedResult, String errorMessage, int lineNumber) throws IOException {
        String baselineDataPath = Paths.get("src/test/resources/testData/candidate_baseline.csv").toString();
        String candidateDataPath = Paths.get("src/test/resources/testData", format("candidate_%s.csv", input)).toString();

        try (FileInputStream baselineFIS = new FileInputStream(baselineDataPath);
        FileInputStream candidateFIS = new FileInputStream(candidateDataPath)) {

            ResponseInputStream<GetObjectResponse> baselineResponseInputStream = new ResponseInputStream<>(GetObjectResponse.builder().build(), baselineFIS);
            ResponseInputStream<GetObjectResponse> candidateResponseInputStream = new ResponseInputStream<>(GetObjectResponse.builder().build(), candidateFIS); 

            when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(candidateResponseInputStream)
                .thenReturn(baselineResponseInputStream);

            App function = new App();
            Candidate candidate = new Candidate("/an/s3/key.csv", true);
            Candidate result = function.handleRequest(candidate, context);
            assertEquals(expectedResult, result.proceed());
            
//            if (!"<null>".equals(errorMessage)) {
//                verify(mockLogger).error(errorMessage);
//            }
            
//            if (lineNumber != -1) {
//                verify(mockLogger).info("Failed on line [{}]", lineNumber);
//            }

            verify(mockS3Client, atMost(0)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
    }
}
