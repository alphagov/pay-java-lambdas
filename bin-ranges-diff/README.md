# App

This module contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2)
dependencies.

The function handler returns the outcome of diffing the currently promoted BIN ranges against a candidate data set, loaded from S3.

### Key Environment Vars:

| Variable Name              | Example Value                                   | Required |
|----------------------------|-------------------------------------------------|----------|
| AWS_ACCOUNT_NAME           | deploy                                          | YES      |
| AWS_REGION                 | eu-west-1                                       | YES      |
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

Managed via Terraform

- Run `mvn clean verify`
- Copy the built `uber` jar from the `target` directory to the [`functions` directory](https://github.com/alphagov/pay-infra/tree/master/provisioning/terraform/modules/pay_bin_ranges_automation/functions) in the terraform module
- Run `terraform apply` for the relevant deployment


