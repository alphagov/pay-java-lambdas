package uk.gov.pay.java_lambdas.common.bin_ranges.helper;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class LocalSftpServer {
    private SshServer sshServer;
    private Path testDirectory;
    public static final String TEST_SERVER_USERNAME = "test_user"; 
    public static final String V03_FILENAME = "WP_341BIN_V03_20240212_001.CSV";
    public static final String V04_FILENAME = "WP_341BIN_V04_20240212_001.CSV";

    public void startServer() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(0); // automatic port selection
        sshServer.setHost("localhost");
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setPublickeyAuthenticator((username, key, session) -> TEST_SERVER_USERNAME.equals(username));
        configureFileSystem(sshServer);
        SftpSubsystemFactory sftpSubsystemFactory = new SftpSubsystemFactory.Builder().build();
        sshServer.setSubsystemFactories(Collections.singletonList(sftpSubsystemFactory));
        sshServer.start();
    }

    public int getPort() {
        return sshServer.getPort();
    }
    
    public String getHost() {
        return  sshServer.getHost();
    }

    public void stopServer() throws IOException {
        if (sshServer != null) {
            sshServer.stop(true);
            if (testDirectory != null) {
                Files.walk(testDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }

    private void configureFileSystem(SshServer sshServer) throws IOException {
        testDirectory = Files.createTempDirectory("testDir");
        Path nestedDirs = testDirectory.resolve("Streamline/Universal");
        Files.createDirectories(nestedDirs);

        List<String> filenames = Arrays.asList(
            V03_FILENAME, V04_FILENAME,
            "WP_CPC2_311FXR_20240207_001.CSV", "WP_CPC2_311FXR_20240208_001.CSV",
            "WP_CPC2_311FXR_20240209_001.CSV", "WP_CPC2_311FXR_20240212_001.CSV",
            "WP_CPC2_311FXR_20240213_001.CSV", "WP_CPCX_311FXR_20240207_001.CSV",
            "WP_CPCX_311FXR_20240208_001.CSV", "WP_CPCX_311FXR_20240209_001.CSV",
            "WP_CPCX_311FXR_20240212_001.CSV", "WP_CPCX_311FXR_20240213_001.CSV"
        );

        filenames.forEach(file -> {
            Path simulatedFile = nestedDirs.resolve(file);
            try {
                Files.createFile(simulatedFile);
                if (file.equals(V03_FILENAME)) {
                    Files.writeString(simulatedFile, loadData("/sftp-server-files/version_03.csv", "\n", getClass()));
                } else if (file.equals(V04_FILENAME)) {
                    Files.writeString(simulatedFile, loadData("/sftp-server-files/version_04.csv","\n", getClass()));
                } else {
                    Files.writeString(simulatedFile, "some test data");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory(testDirectory);
        sshServer.setFileSystemFactory(fileSystemFactory);

    }

    public static String loadData(String resource, CharSequence delimiter, Class clazz) throws IOException {
        InputStream inputStream = clazz.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new FileNotFoundException("resource not found");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(delimiter));
        }
    }
}
