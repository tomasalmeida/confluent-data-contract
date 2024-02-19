# Confluent Data Contract

## Start the environment

```shell
    cd env
    docker-compose up -d
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.users
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.contracts 
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.generic-dlq
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic warehouse.products
    cd ..
```

Control center is available under http://localhost:9021

## Read Write rules

### Register a plain vanilla schema

```shell
  # user subject
  jq -n --rawfile schema src/main/resources/schema/user.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.users-value/versions --json @- | jq
  # contract subject
  jq -n --rawfile schema src/main/resources/schema/contract.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.contracts-value/versions --json @- | jq
```

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

### run producer

```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.readwrite.ProducerRunner
```

### run consumer

```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.readwrite.ConsumerRunner
```

### check DLQ

```shell
  kafka-console-consumer --bootstrap-server localhost:29092 \
    --property schema.registry.url=http://localhost:8081 \
    --property print.timestamp=true \
    --property print.offset=true \
    --property print.partition=true \
    --property print.headers=true \
    --property print.key=true \
    --property print.value=true \
    --topic crm.generic-dlq \
    --from-beginning
```

## Migration rules

### Register the v1 and v2 schemas

```shell
  # register v1 product
  jq -n --rawfile schema src/main/resources/schema/product_v1.avsc '{schema: $schema, metadata: { properties: { app_version: 1 }}}' | \
  curl --silent http://localhost:8081/subjects/warehouse.products-value/versions --json @- | jq
  # register v2 product
  jq -n --rawfile schema src/main/resources/schema/product_v2.avsc '{schema: $schema, metadata: { properties: { app_version: 2 }}}' | \
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
  jq -n --rawfile schema src/main/resources/schema/product_v2.avsc '{schema: $schema, metadata: { properties: { app_version: 2 }}}' | \
  curl --silent http://localhost:8081/subjects/warehouse.products-value/versions --json @- | jq
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

Now, we can run our producers:
```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ProducerRunner
```

### run consumers

```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.migration.ConsumerRunner
```


# Shutdown

```shell
    cd env
    docker-compose down -v
    cd ..
```

References:
- https://github.com/mcascallares/confluent-data-contracts
- https://github.com/rayokota/demo-data-contracts
- https://yokota.blog/2023/10/01/understanding-cel-in-data-contract-rules/
- https://www.confluent.io/en-gb/blog/data-contracts-confluent-schema-registry/
- https://docs.confluent.io/cloud/current/sr/fundamentals/data-contracts.html
- Basics:
  - https://docs.confluent.io/platform/current/installation/configuration/consumer-configs.html
  - https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html