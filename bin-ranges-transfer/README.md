# App

This module contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.

The function handler returns the outcome of streaming BIN ranges data from Worldpay to Pay S3.

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


