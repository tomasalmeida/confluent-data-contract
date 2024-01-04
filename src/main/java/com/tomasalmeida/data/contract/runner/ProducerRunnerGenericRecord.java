package com.tomasalmeida.data.contract.runner;

import com.tomasalmeida.data.contract.Contract;
import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Properties;

public class ProducerRunnerGenericRecord extends Thread {
    private static final String TOPIC_USERS = "crm.users";
    private static final String TOPIC_CONTRACTS = "crm.contracts";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunnerGenericRecord.class);

    private final KafkaProducer<String, User> userProducer;
    private final KafkaProducer<String, Object> contractProducer;

    public ProducerRunnerGenericRecord() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put(KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        userProducer = new KafkaProducer<>(properties);
        contractProducer = new KafkaProducer<>(properties);
    }

    @Override
    public void run() {
        try {
//            produceUser("Tomas", "Dias Almeida", "Tomas Almeida", 39);
//            produceUser("Fernando", "Perez Machado", "", 53);
//            produceUser("Amon", "Ra", "", 52);
//            produceUser("La", "Fontaine", "", 49);
//            produceUser("Young", "Sheldon Cooper", "", 7);
            userProducer.close();
            produceContract(1, "valid contract", LocalDate.of(2030, 12, 31));
//            produceContract(2, "expired contract", LocalDate.of(2021, 12, 31));
//            produceContract(3, "a", LocalDate.of(2122, 12, 31));
            contractProducer.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }

    }

    private void produceUser(String firstName, String lastName, String fullName, int age) throws InterruptedException {
        User user = null;
        try {
            user = new User(firstName, lastName, fullName, age);
            LOGGER.info("Sending user {}", user);
            ProducerRecord<String, User> userRecord = new ProducerRecord<>(TOPIC_USERS, user);
            userProducer.send(userRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize user: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);

    }

    private void produceContract(int id, String name, LocalDate expiration) throws InterruptedException {
        Contract contract = null;
        String contractSchema = "{\n" +
                "  \"name\": \"Contract\",\n" +
                "  \"namespace\": \"com.tomasalmeida.data.contract\",\n" +
                "  \"type\": \"record\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"id\",\n" +
                "      \"type\": \"int\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"name\",\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"expiration\",\n" +
                "      \"type\": {\n" +
                "        \"type\": \"string\",\n" +
                "        \"logicalType\": \"date\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Schema.Parser parser = new Schema.Parser();
        Schema parsedContractSchema = parser.parse(contractSchema);
        GenericRecord avroRecord = new GenericData.Record(parsedContractSchema);
        avroRecord.put("id", id);
        avroRecord.put("name", name);
        avroRecord.put("expiration", expiration.toString());
        ProducerRecord<String, Object> contractRecord = new ProducerRecord<>(TOPIC_CONTRACTS, avroRecord);

        try {
            contract = new Contract (id, name, expiration.toString());
            LOGGER.info("Sending contract {}", contractRecord);
//            ProducerRecord<String, Object> contractRecord = new ProducerRecord<>(TOPIC_CONTRACTS, contract);
            contractProducer.send(contractRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize contract: {}", serializationException.getCause().getMessage());
            LOGGER.error("a", serializationException);
        }
        LOGGER.info("================");
        Thread.sleep(1000);

    }

    public static void main(final String[] args) throws IOException {
        ProducerRunnerGenericRecord producerRunner = new ProducerRunnerGenericRecord();
        producerRunner.run();
    }
}
