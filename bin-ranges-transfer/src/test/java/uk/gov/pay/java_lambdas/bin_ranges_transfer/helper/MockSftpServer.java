package uk.gov.pay.java_lambdas.bin_ranges_transfer.helper;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MockSftpServer {

    private SshServer sshServer;
    private Path mockDir;
    public static final String TEST_SERVER_USERNAME = "test_user"; 

    public void startServer() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(0); // automatic port selection
        sshServer.setHost("localhost");
        Path hostKey = Path.of(ClassLoader.getSystemResource("ssh/test_hostkey.ser").getPath());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey));

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
            if (mockDir != null) {
                Files.walk(mockDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }

    private void configureFileSystem(SshServer sshServer) throws IOException {
        mockDir = Files.createTempDirectory("testDir");
        Path nestedDirs = mockDir.resolve("Streamline/Universal");
        Files.createDirectories(nestedDirs);

        List<String> filenames = Arrays.asList(
            "WP_341BIN_V03_20240212_001.CSV", "WP_341BIN_V04_20240212_001.CSV",
            "WP_CPC2_311FXR_20240207_001.CSV", "WP_CPC2_311FXR_20240208_001.CSV",
            "WP_CPC2_311FXR_20240209_001.CSV", "WP_CPC2_311FXR_20240212_001.CSV",
            "WP_CPC2_311FXR_20240213_001.CSV", "WP_CPCX_311FXR_20240207_001.CSV",
            "WP_CPCX_311FXR_20240208_001.CSV", "WP_CPCX_311FXR_20240209_001.CSV",
            "WP_CPCX_311FXR_20240212_001.CSV", "WP_CPCX_311FXR_20240213_001.CSV");

        filenames.forEach(file -> {
            Path simulatedFile = nestedDirs.resolve(file);
            try {
                Files.createFile(simulatedFile);
                Files.writeString(simulatedFile, "some test data");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory(mockDir);
        sshServer.setFileSystemFactory(fileSystemFactory);

    }
}
