package uk.gov.pay.java_lambdas.integration.bin_ranges;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ResourceReaper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.Architecture;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.Environment;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCHLOGS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.TEST_SERVER_USERNAME;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.V03_FILENAME;
import static uk.gov.pay.java_lambdas.common.bin_ranges.helper.LocalSftpServer.loadData;

class BinRangesIngestIT {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final Logger logger = LoggerFactory.getLogger(BinRangesIngestIT.class);
    // don't forget to change the tag in GHA workflow if you update this 
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.3");
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
        .withServices(S3, SSM, LAMBDA, CLOUDWATCHLOGS)
        .withEnv("DEFAULT_REGION", Region.EU_WEST_1.toString())
//        .withEnv("DEBUG", "1")
//        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("localstack")))
        .withEnv("LAMBDA_DOCKER_FLAGS", testcontainersLabels())
        .withEnv("LAMBDA_IGNORE_ARCHITECTURE", "1")
        .withEnv("LAMBDA_RUNTIME_ENVIRONMENT_TIMEOUT", "60");

    private S3Client s3Client;
    private SsmClient ssmClient;
    private LambdaClient lambdaClient;
    private LambdaWaiter lambdaWaiter;
    private static final LocalSftpServer LOCAL_SFTP_SERVER;
    private static final String AWS_ACCOUNT_NAME = "test";
    private static final String PASSPHRASE_PARAMETER_NAME = "passphrase";
    private static final String PRIVATE_KEY_PARAMETER_NAME = "privateKey";
    private static final String STAGED_BUCKET_NAME = format("bin-ranges-staged-%s", AWS_ACCOUNT_NAME);
    private static final String PROMOTED_BUCKET_NAME = format("bin-ranges-promoted-%s", AWS_ACCOUNT_NAME);
    private static final Path PROMOTED_CSV_PATH = Paths.get("src/test/resources/promoted-ranges.csv");

    static {
        LOCAL_SFTP_SERVER = new LocalSftpServer();
        try {
            LOCAL_SFTP_SERVER.startServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Testcontainers.exposeHostPorts(LOCAL_SFTP_SERVER.getPort());
        LOCALSTACK.start();
    }

    @BeforeEach
    public void setup() throws IOException {
        lambdaClient = LambdaClient.builder()
            .endpointOverride(LOCALSTACK.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())
                )
            )
            .region(Region.of(LOCALSTACK.getRegion()))
            .build();

        lambdaWaiter = lambdaClient.waiter();

        s3Client = S3Client
            .builder()
            .endpointOverride(LOCALSTACK.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())
                )
            )
            .region(Region.of(LOCALSTACK.getRegion()))
            .build();

        ssmClient = SsmClient
            .builder()
            .endpointOverride(LOCALSTACK.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())
                )
            )
            .region(Region.of(LOCALSTACK.getRegion()))
            .build();

        ssmClient.putParameter(PutParameterRequest.builder()
            .name(PASSPHRASE_PARAMETER_NAME)
            .value(loadData("/ssh-config/test_passphrase", "", getClass()))
            .type(ParameterType.SECURE_STRING)
            .build());

        ssmClient.putParameter(PutParameterRequest.builder()
            .name(PRIVATE_KEY_PARAMETER_NAME)
            .value(loadData("/ssh-config/test_private_key.rsa", "\n", getClass()))
            .type(ParameterType.SECURE_STRING)
            .build());

        s3Client.createBucket(CreateBucketRequest.builder().bucket(STAGED_BUCKET_NAME)
            .build());

        s3Client.createBucket(CreateBucketRequest.builder().bucket(PROMOTED_BUCKET_NAME)
            .build());

        s3Client.putObject(PutObjectRequest.builder()
            .bucket(PROMOTED_BUCKET_NAME)
            .key("latest/worldpay-v3.csv")
            .build(), PROMOTED_CSV_PATH);

        List<CompletableFuture<Void>> futures = Stream.of(
                "integrity",
                "promotion",
                "diff",
                "transfer"
            )
            .map(l -> {
                try {
                    return createAndActivateLambda(l);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    void shouldHandle() throws IOException {
        assertEquals(1, s3Client.listObjects(ListObjectsRequest.builder()
            .bucket(PROMOTED_BUCKET_NAME).build()).contents().size());
        assertEquals(0, s3Client.listObjects(ListObjectsRequest.builder()
            .bucket(STAGED_BUCKET_NAME).build()).contents().size());
        logger.debug("invoking bin-ranges-transfer");
        var transferFunctionInvokeRes = lambdaClient.invoke(InvokeRequest.builder()
            .functionName("bin-ranges-transfer")
            .build()
        );

        JSONObject transferFunctionResponseCandidate = commonAssertions(transferFunctionInvokeRes.payload());
        SdkBytes diffFunctionRequestPayload = SdkBytes.fromUtf8String(transferFunctionResponseCandidate.toString());

        logger.debug("invoking bin-ranges-diff");
        var diffFunctionInvokeRes = lambdaClient.invoke(InvokeRequest.builder()
            .functionName("bin-ranges-diff")
            .payload(diffFunctionRequestPayload)
            .build()
        );

        JSONObject diffFunctionResponseCandidate = commonAssertions(diffFunctionInvokeRes.payload());
        SdkBytes integrityFunctionRequestPayload = SdkBytes.fromUtf8String(diffFunctionResponseCandidate.toString());

        logger.debug("invoking bin-ranges-integrity");
        var integrityFunctionInvokeRes = lambdaClient.invoke(InvokeRequest.builder()
            .functionName("bin-ranges-integrity")
            .payload(integrityFunctionRequestPayload)
            .build()
        );

        JSONObject integrityFunctionResponseCandidate = commonAssertions(integrityFunctionInvokeRes.payload());
        SdkBytes promotionFunctionRequestPayload = SdkBytes.fromUtf8String(integrityFunctionResponseCandidate.toString());

        logger.debug("invoking bin-ranges-promotion");
        var promotionFunctionInvokeRes = lambdaClient.invoke(InvokeRequest.builder()
            .functionName("bin-ranges-promotion")
            .payload(promotionFunctionRequestPayload)
            .build()
        );

        boolean promotionFunctionCandidate = Boolean.parseBoolean(new String(promotionFunctionInvokeRes.payload().asByteArray()));
        assertTrue(promotionFunctionCandidate);
        assertEquals(2, s3Client.listObjects(ListObjectsRequest.builder()
            .bucket(PROMOTED_BUCKET_NAME).build()).contents().size());
    }
    
    // --- PRIVATE ---

    private JSONObject commonAssertions(SdkBytes responsePayload) {
        JSONObject underTest = new JSONObject(new String(responsePayload.asByteArray()));
        assertTrue((Boolean) underTest.get("proceed"));
        assertTrue(((String) underTest.get("s3Key")).contains(V03_FILENAME));
        return underTest;
    }

    private static String testcontainersLabels() {
        return Stream.concat(
                Stream
                    .of(DockerClientFactory.DEFAULT_LABELS.entrySet().stream(),
                        ResourceReaper.instance().getLabels().entrySet().stream())
                    .flatMap(Function.identity())
                    .map(entry -> String.format("-l %s=%s", entry.getKey(), entry.getValue())),
                Stream.of("--add-host host.testcontainers.internal:host-gateway")) // important to allow lambdas to connect out to local sftp
            .collect(Collectors.joining(" "));
    }

    private CompletableFuture<Void> createAndActivateLambda(String function) throws FileNotFoundException {
        String functionName = format("bin-ranges-%s", function);

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
            .functionName(functionName)
            .build();

        InputStream is = new FileInputStream(Paths.get(format("target/jars/%s-1.0-SNAPSHOT-uber.jar", functionName)).toString());
        SdkBytes fileToUpload = SdkBytes.fromInputStream(is);

        FunctionCode functionCode = FunctionCode.builder()
            .zipFile(fileToUpload)
            .build();

        // env vars for all functions
        Environment functionEnv = Environment.builder()
            .variables(Map.ofEntries(
                Map.entry("AWS_ACCESS_KEY_ID", LOCALSTACK.getAccessKey()),
                Map.entry("AWS_SECRET_ACCESS_KEY", LOCALSTACK.getSecretKey()),
                Map.entry("AWS_ACCOUNT_NAME", AWS_ACCOUNT_NAME),
                Map.entry("AWS_REGION", "eu-west-1"),
                Map.entry("LOG_LEVEL", "DEBUG"),
                Map.entry("PASSPHRASE_PARAMETER_NAME", PASSPHRASE_PARAMETER_NAME),
                Map.entry("PRIVATE_KEY_PARAMETER_NAME", PRIVATE_KEY_PARAMETER_NAME),
                Map.entry("SFTP_HOST", "host.testcontainers.internal"),
                Map.entry("SFTP_PORT", String.valueOf(LOCAL_SFTP_SERVER.getPort())),
                Map.entry("SFTP_USERNAME", TEST_SERVER_USERNAME),
                Map.entry("LOCALSTACK_ENABLED", "TRUE")
            )).build();

        CreateFunctionRequest createFunctionRequest = CreateFunctionRequest.builder()
            .functionName(functionName)
            .environment(functionEnv)
            .code(functionCode)
            .handler(format("uk.gov.pay.java_lambdas.bin_ranges_%s.App::handleRequest", function))
            .architectures(Architecture.ARM64)
            .runtime(Runtime.JAVA21)
            .timeout(300)
            .role("arn:aws:iam::000000000000:role/lambda-role")
            .build();

        return CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName("createAndActivateLambdas-" + function);
            lambdaClient.createFunction(createFunctionRequest);
            WaiterResponse<GetFunctionResponse> existsResponse = lambdaWaiter.waitUntilFunctionExists(getFunctionRequest);
            existsResponse.matched().response().ifPresent(response -> logger.debug("LAMBDA CREATED: {}", response.configuration().functionName()));
            WaiterResponse<GetFunctionResponse> activeResponse = lambdaWaiter.waitUntilFunctionActiveV2(getFunctionRequest);
            activeResponse.matched().response().ifPresent(response -> logger.debug("LAMBDA READY: {}", response.configuration().functionName()));
        }, executor);
    }
}
