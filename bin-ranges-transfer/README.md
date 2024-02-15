# App

This module contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2)
dependencies.

The function handler returns the outcome of streaming BIN ranges data from Worldpay to Pay S3.

### Key Environment Vars:

| Variable Name              | Example Value                                   | Required |
|----------------------------|-------------------------------------------------|----------|
| AWS_ACCOUNT_NAME           | deploy                                          | YES      |
| AWS_REGION                 | eu-west-1                                       | YES      |
| PASSPHRASE_PARAMETER_NAME  | deploy_worldpay_secure_file_gateway.passphrase  | YES      |
| PRIVATE_KEY_PARAMETER_NAME | deploy_worldpay_secure_file_gateway.private-key | YES      |
| S3_BUCKET_NAME             | bin-ranges-staging                              | YES      |

## Prerequisites

- Java 21
- Apache Maven

## Development

Dependencies are managed through the `DependencyFactory` class using the Static Factory pattern.

#### Building the project

```
mvn clean install
```

#### Testing it locally

```
mvn test
```

## Deployment

Managed via Terraform, coming soon


