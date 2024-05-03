# End-to-end testing

This module runs end-to-end tests for serverless code, the built uber jars are loaded into a running Localstack container and invoked as Lambda functions in sequence

## Prerequisites

- Java 21
- Apache Maven
- Docker

#### Running it locally

```
mvn clean verify -Dskip.surefire=true --projects e2e --also-make
```


