# pay-java-lambdas
GOV.UK Pay Java Lambdas

Maven multi-module Java21 monorepo for GOV.UK Pay Lambdas 

See individual modules for additional information

### Generate a new Lambda scaffold

https://github.com/aws/aws-sdk-java-v2/blob/master/archetypes/archetype-lambda/README.md

```bash
mvn archetype:generate \
    -DarchetypeGroupId=software.amazon.awssdk \
    -DarchetypeArtifactId=archetype-lambda \
    -DarchetypeVersion=2.23.17 \
    -DgroupId=uk.gov.pay \
    -DartifactId=my-lambda
```

## Licence

[MIT License](LICENSE)

## Vulnerability Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. Please refer to our [vulnerability disclosure policy](https://www.gov.uk/help/report-vulnerability) and our [security.txt](https://vdp.cabinetoffice.gov.uk/.well-known/security.txt) file for details.
