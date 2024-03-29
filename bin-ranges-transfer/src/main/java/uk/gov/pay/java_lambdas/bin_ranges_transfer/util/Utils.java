package uk.gov.pay.java_lambdas.bin_ranges_transfer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import uk.gov.pay.java_lambdas.bin_ranges_transfer.model.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern VERSION_PATTERN = Pattern.compile("V\\d{2}");

    private Utils() {
    }

    public static Path createTempPrivateKeyFile(String privateKey) throws IOException {
        if (privateKey.isBlank()) {
            throw new IllegalArgumentException("problem loading private key");
        }
        Path tempKeyFile = Files.createTempFile("privateKey", ".tmp");
        Files.write(tempKeyFile, privateKey.getBytes());
        tempKeyFile.toFile().deleteOnExit(); // bin this when jvm exits
        return tempKeyFile;
    }

    public static GetParameterRequest parameterRequestWithDecryption(String parameterName) {
        return GetParameterRequest.builder()
            .name(parameterName)
            .withDecryption(true)
            .build();
    }

    public static String extractDateString(String fileName) throws DateTimeParseException {
        Matcher matcher = DATE_PATTERN.matcher(fileName);

        if (matcher.find()) {
            String dateString = matcher.group();
            logger.debug("Date pattern [{}] found in file name: {}", dateString, fileName);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(dateString, formatter);
            
            logger.debug("Date validated: {}", date);
            return date.toString();
        } else {
            throw new IllegalArgumentException(String.format("No date string found in file name: %s", fileName));
        }
    }
    
    public static Version extractVersion(String fileName) throws IllegalArgumentException {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);

        if (matcher.find()) {
            String versionString = matcher.group();
            logger.debug("Version pattern [{}] found in file name: {}", versionString, fileName);
            return Version.fromString(versionString);
        } else {
            throw new IllegalArgumentException(String.format("No version string found in file name: %s", fileName));
        }
    }
}
