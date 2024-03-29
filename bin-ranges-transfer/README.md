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
| WORLDPAY_FILE_VERSION      | V03                                             | YES      |
| LOG_LEVEL                  | DEBUG                                           | NO       |

## Prerequisites

- Java 21
- Apache Maven

## Development

Dependencies are managed through the `DependencyFactory` class using the Static Factory pattern.

#### Building the project

```
mvn clean package
```

#### Testing it locally

```
mvn clean test
```

## Deployment

Managed via Terraform, coming soon


