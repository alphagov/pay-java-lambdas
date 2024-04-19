package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize.DeserializerModifierWithValidation.deserializeAndValidateModule;

class IssuerBINDetailRecordTest {

    private CsvMapper mapper;
    private CsvSchema schema;

    @BeforeEach
    public void setUp() {
        mapper = CsvMapper.builder()
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
        mapper.registerModule(deserializeAndValidateModule());
        schema = mapper.schemaFor(IssuerBINDetailRecord.class).withoutQuoteChar().withoutHeader();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BIN_V03_REDACTED_1.zip", "BIN_V03_REDACTED_2.zip", "BIN_V04_REDACTED_1.zip"})
    void issuerBINDetailRecord_shouldDeserialize_andValidate_fromCSV(String filename) throws IOException {

        List<IssuerBINDetailRecord> records = Collections.synchronizedList(new ArrayList<>());
        
        try (InputStream inputStream = new FileInputStream(Paths.get("src/test/resources/test-data", filename).toString());
             ZipInputStream zipInputStream = new ZipInputStream(inputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream))) {

            zipInputStream.getNextEntry();
            List<String> lines = reader.lines().collect(Collectors.toList());
            lines.removeFirst(); // strip header record
            lines.removeLast(); // strip trailer record

            lines.parallelStream().forEach(line -> {
                try {
                    records.add(mapper.readerFor(IssuerBINDetailRecord.class)
                            .with(schema)
                            .readValue(line));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });

            assertFalse(records.isEmpty());
            assertEquals(lines.size(), records.size());
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/bad_ranges.csv", numLinesToSkip = 1)
    void issuerBINDetailRecord_shouldFailValidation_ifRequiredField_isMissing(String expected, String input) throws IOException {

        Exception exception = assertThrows(ConstraintViolationException.class, () -> {
            var record = mapper.readerFor(IssuerBINDetailRecord.class)
                .with(schema)
                .readValue(input);
            System.out.println(record);
        });

        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expected));
    }

    @Test
    void issuerBINDetailRecord_shouldFailDeserialize_ifUnknownCardClassEnumValue_isEncountered() throws IOException {
        Exception exception = assertThrows(InvalidFormatException.class, () -> mapper.readerFor(IssuerBINDetailRecord.class)
                .with(schema)
                .readValue("01,999999999999999998,999999999999999999,CN,MASTERCARD CREDIT,APERTURE SCIENCE INC.,GBR,826,UNITED KINGDOM,Z,GBP,DCC allowed,AC000,N,N,,16,N,,,,,,"));

        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains("Cannot deserialize value of type `uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.CardClass`"));
        assertTrue(actualMessage.contains("not one of the values accepted for Enum class: [P, R, C, D, H]"));
    }
}
