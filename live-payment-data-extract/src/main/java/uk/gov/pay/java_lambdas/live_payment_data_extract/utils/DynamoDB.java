package uk.gov.pay.java_lambdas.live_payment_data_extract.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import uk.gov.pay.java_lambdas.live_payment_data_extract.DependencyFactory;

import java.util.HashMap;
import java.util.Map;

public class DynamoDB {
    
    private DynamoDB() {
    }
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDB.class);
    private static final DynamoDbClient dynamoDbClient = DependencyFactory.dynamoDbClient();

    public static void get() {

        // TODO remove hardcoded examples
        String tableName = "service";
        String gatewayAccountId = "7548";
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("gateway_account_id", AttributeValue.builder().s(gatewayAccountId).build());

        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()) {
                Map<String, AttributeValue> item = response.item();
                logger.info("Retrieved item: {}", item);
            } else {
                logger.warn("Item not found");
            }
        } catch (Exception e) {
            logger.error("Error retrieving item", e);
        }
    }
    
    private void put() {
        
    }
}
