package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractDateString;
import static uk.gov.pay.java_lambdas.bin_ranges_transfer.util.Utils.extractVersion;

public record BinRangeSftpDownload(String filePath, String fileName, Version version, String date, Long size) {
    
    public BinRangeSftpDownload(String filePath, String fileName, Long size) {
        this(filePath, fileName, extractVersion(fileName), extractDateString(fileName), size);
    }
    
    public String getS3Key() {
        return String.format("%s/%s", date, fileName);
    }
}
