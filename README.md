# Confluent Data Contract

## Pre-requisites

Have the following tools installed:
- Docker
- Docker Compose
- Maven
- Java 17
- jq

## Start the environment and compile the code

```shell
    cd env
    docker-compose up -d
    cd ..
    # compile the project and move to the app folder
    mvn clean package
```

Control center is available under http://localhost:9021

## DEMO 1: Read Write rules (validation and transformation)

Creating the needed topics:

```shell
  # create topics
  cd env/
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.users
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.contracts 
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.generic-dlq
  cd ..
  cd readwrite-rules-app
```

### Register plain vanilla schemas

```shell
  # user subject
  jq -n --rawfile schema src/main/resources/schema/user.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.users-value/versions --json @- | jq
  # contract subject
  jq -n --rawfile schema src/main/resources/schema/contract.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.contracts-value/versions --json @- | jq
```
> [!NOTE]
> * We have created two schemas, one for users and one for contracts. 
> * They are plain vanilla schemas, with no metadata or rules.

### Enhancing the data contract with metadata

```shell
    # user metadata
    curl -s http://localhost:8081/subjects/crm.users-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data "@src/main/resources/schema/user-metadata.json" | jq
    # contract metadata
    curl -s http://localhost:8081/subjects/crm.contracts-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data "@src/main/resources/schema/contract-metadata.json" | jq
```

> [!NOTE]
> * We have added metadata to the schemas, indicating if they are GDPR sensitive, the owner and owner e-mail of these subjects.
> * As you can see the metadata is not validated, it is just stored in the schema registry.
> * Another important point is the id is increased by one, indicating a new version of the subject.

### Adding data quality rules to the data contract

```shell
    # users rules
    curl http://localhost:8081/subjects/crm.users-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data @src/main/resources/schema/user-ruleset.json | jq
    # contracts rules
    curl http://localhost:8081/subjects/crm.contracts-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data @src/main/resources/schema/contract-ruleset.json | jq
```

> [!NOTE]
> * We have added rules to the schemas, indicating, for example:
>   * The user schema has a rule that the user must be older than 18 years old
>   * The contract schema has a rule that the contract must have a valid date.
> * As before, the id is increased by one, indicating a new version of the subject.

### Run producer 

Check events being created or refused due to the condition rules.

```shell
  java -classpath target/readwrite-rules-app-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.readwrite.ProducerRunner 
```
> [!TIP]
> * Check the logs to see the events being produced and the ones that were not accepted by the rules.
> * The producer produces 8 events, 3 of them will be accepted and 5 will be refused.
>   * In some cases, the refused events will be sent to the DLQ topic (check the rules to see which ones).

### Run consumer

Check events being consumed and transformed during consumption.

```shell
  java -classpath target/readwrite-rules-app-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.readwrite.ConsumerRunner
```

> [!IMPORTANT]
> One of the users is transformed during consumption. The fullName is created by concatenating the firstName and lastName.

### check DLQ

Check the DLQ topic to see the events that were not accepted by the rules and their headers.

```shell
  kafka-console-consumer --bootstrap-server localhost:29092 \
    --property schema.registry.url=http://localhost:8081 \
    --property print.timestamp=false \
    --property print.offset=false \
    --property print.partition=false \
    --property print.headers=true \
    --property print.key=true \
    --property print.value=true \
    --topic crm.generic-dlq \
    --from-beginning
```



## DEMO 2: Migration rules

Create the resources

```shell
    cd env
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic warehouse.products
    cd ..
    mvn clean package
```





### Register the v1 and v2 schemas

```shell
  # register v1 product
  jq -n --rawfile schema  migration-app-v1/src/main/resources/schema/product.avsc '{schema: $schema, metadata: { properties: { app_version: 1 }}}' | \
  curl --silent http://localhost:8081/subjects/warehouse.products-value/versions --json @- | jq
  # register v2 product
  jq -n --rawfile schema  migration-app-v2/src/main/resources/schema/product.avsc '{schema: $schema, metadata: { properties: { app_version: 2 }}}' | \
  curl --silent http://localhost:8081/subjects/warehouse.products-value/versions --json @- | jq
```

V2 insert will fail, as the default compatibility is reviewed and the one we are trying to create breaks it. 

```shell
{
  "error_code": 409,
  "message": "Schema being registered is incompatible with an earlier schema for subject \"warehouse.products-value\"
  ...
```

We need to enable the compatibility group using the Let's tell the Schema Registry to use the app_version field to enable Compatibility Groups:

```shell
curl http://localhost:8081/config/warehouse.products-value \
  -X PUT --json '{ "compatibilityGroup": "app_version" }'
```

We try again

```shell
  # register v2 product
  jq -n --rawfile schema  migration-app-v2/src/main/resources/schema/product.avsc '{schema: $schema, metadata: { properties: { app_version: 2 }}}' | \
  curl --silent http://localhost:8081/subjects/warehouse.products-value/versions --json @- | jq
```

Register the rules

```shell
  curl http://localhost:8081/subjects/warehouse.products-value/versions \
    --json @migration-app-v2/src/main/resources/schema/product-migration-rules.json | jq
```

### Producing data with version 1 and version 2 in parallel

#### ⚠️ There is an important point, we need to indicate the app_version in our producer, so we need to add the lines

```java
  // indicates the app_version=1
  properties.put(KafkaAvroDeserializerConfig.USE_LATEST_WITH_METADATA, "app_version=1");
  productProducerV1 = new KafkaProducer<>(properties);
  
  // indicates the app_version=2
  properties.put(KafkaAvroDeserializerConfig.USE_LATEST_WITH_METADATA, "app_version=2");
  productProducerV2 = new KafkaProducer<>(properties);
```

#### Running producer v1 (exclusive shell)

```shell
  cd migration-app-v1
  java -classpath target/migration-app-v1-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ProducerRunner
```

#### Running producer v2 (exclusive shell)

```shell
  cd migration-app-v2
  java -classpath target/migration-app-v2-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ProducerRunner
```


### run consumers

#### Running producer v1 (exclusive shell)

```shell
  cd migration-app-v1
  java -classpath target/migration-app-v1-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ConsumerRunner
```

#### Running producer v2 (exclusive shell)  

```shell
  cd migration-app-v2
  java -classpath target/migration-app-v2-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ConsumerRunner
```

## DEMO 3: Data Contract Global Rules

Creating the needed topics and compiling the project

```shell
  # create topics
  cd env/
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.clients
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.orders 
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.products
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.stockitems  
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.dlq.invalid.clients
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic data.dlq.invalid.products
  cd ..
  # compile the project and move to the app folder
  mvn clean package
  cd global-rules-app
```

### Enhancing the schemas with the global rule defaultRuleset

This step needs to be done before creating the 

```shell
  curl -s http://localhost:8081/config \
  -X PUT \
  --header "Content-Type: application/json" \
  --data @src/main/resources/schema/global-ruleset_v1.json | jq
```

### Register 1 plain vanilla schemas

```shell
  # client subject
  jq -n --rawfile schema src/main/resources/schema/client.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/data.clients-value/versions --json @- | jq
```  

### Update rules with overrideRuleSet

```shell
  curl -s http://localhost:8081/config \
  -X PUT \
  --header "Content-Type: application/json" \
  --data @src/main/resources/schema/global-ruleset_v2.json | jq
```

### Register / update  plain vanilla schemas

```shell
  # client subject
  jq -n --rawfile schema src/main/resources/schema/client.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/data.clients-value/versions --json @- | jq
  # orders subject
  jq -n --rawfile schema src/main/resources/schema/order.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/data.orders-value/versions --json @- | jq
  # products subject
  jq -n --rawfile schema src/main/resources/schema/product.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/data.products-value/versions --json @- | jq
```

All the schemas are registered with the defaultRuleSet and overrideRuleSet.

### Testing the rules

```shell
## run Client producer
java -classpath target/global-rules-app-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.globalrules.ClientProducerRunner
```

## Shutdown

1. Stop the consumers and producers
2. Stop the environment

```shell
    cd env
    docker-compose down -v
    cd ..
```

References:
- Code
  - https://github.com/mcascallares/confluent-data-contracts
  - https://github.com/rayokota/demo-data-contracts
  - https://github.com/randomravings/confluent-data-contracts
  - https://github.com/gphilipp/migration-rules-demo
- Docs and Blogs: 
  - https://yokota.blog/2023/10/01/understanding-cel-in-data-contract-rules/
  - https://www.confluent.io/en-gb/blog/data-contracts-confluent-schema-registry/
  - https://docs.confluent.io/cloud/current/sr/fundamentals/data-contracts.html
- Basics:
  - https://docs.confluent.io/platform/current/installation/configuration/consumer-configs.html
  - https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html