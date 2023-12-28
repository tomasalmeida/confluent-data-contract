# Confluent Data Contract

## Start the environment

```shell
    cd env
    docker-compose up -d
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic users 
    docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic users-dlq
    cd ..
```

Control center is available under http://localhost:9021

## Register a plain vanilla schema

```shell
  jq -n --rawfile schema src/main/resources/schema/user.avsc '{schema: $schema}' | \
  curl --silent http://localhost:8081/subjects/users-value/versions --json @- | \
  jq
```

## Enhancing the data contract with metadata

```shell
    curl -s http://localhost:8081/subjects/users-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data "@src/main/resources/schema/metadata.json" | jq
```

## Adding data quality rules to the data contract

```shell
    curl http://localhost:8081/subjects/users-value/versions \
      --header "Content-Type: application/json" --header "Accept: application/json" \
      --data @src/main/resources/schema/ruleset.json | jq
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


References:
- https://github.com/mcascallares/confluent-data-contracts
- https://github.com/rayokota/demo-data-contracts
- https://yokota.blog/2023/10/01/understanding-cel-in-data-contract-rules/
- https://www.confluent.io/en-gb/blog/data-contracts-confluent-schema-registry/
- https://docs.confluent.io/cloud/current/sr/fundamentals/data-contracts.html
- Basics:
  - https://docs.confluent.io/platform/current/installation/configuration/consumer-configs.html
  - https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html