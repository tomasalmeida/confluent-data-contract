# Confluent Data Contract

## Start the environment

```shell
    cd env
    docker-compose up -d
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.users
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.contracts 
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.generic-dlq
    cd ..
```

Control center is available under http://localhost:9021

## Register a plain vanilla schema

```shell
  # user subject
  jq -n --rawfile schema src/main/resources/schema/user.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.users-value/versions --json @- | jq
  # contract subject
  jq -n --rawfile schema src/main/resources/schema/contract.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/crm.contracts-value/versions --json @- | jq
```

## Enhancing the data contract with metadata

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

## Adding data quality rules to the data contract

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

## run producer

```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.runner.ProducerRunner
```

## run consumer

```shell
  mvn clean package
  java -classpath target/data-contract-example-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.data.contract.runner.ConsumerRunner
```

## check DLQ

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