package com.tomasalmeida.data.contract.readwrite;

import com.tomasalmeida.data.contract.Contract;
import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Properties;

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_CONTRACTS;
import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_USERS;

public class ProducerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);

    private final KafkaProducer<String, User> userProducer;
    private final KafkaProducer<String, Contract> contractProducer;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put("clientId", "user-producer");
        userProducer = new KafkaProducer<>(properties);
        properties.put("clientId", "contract-producer");
        contractProducer = new KafkaProducer<>(properties);
    }

    public void createEvents() {
        try {
            produceUser("Tomas", "Dias Almeida", "Tomas Almeida", 39);
            produceUser("Fernando", "Perez Machado", "", 53);
            produceUser("Amon", "Ra", "", 52);
            produceUser("La", "Fontaine", "", 49);
            produceUser("Young", "Sheldon Cooper", "", 7);
            userProducer.close();
            produceContract(1, "valid contract", LocalDate.of(2030, 12, 31));
            produceContract(2, "expired contract", LocalDate.of(2021, 12, 31));
            produceContract(3, "a", LocalDate.of(2122, 12, 31));
            contractProducer.close();
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }

    }

    private void produceUser(String firstName, String lastName, String fullName, int age) throws InterruptedException {
        try {
            User user = new User(firstName, lastName, fullName, age);
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
        try {
            Contract contract = new Contract(id, name, expiration.toString());
            LOGGER.info("Sending contract {}", contract);
            ProducerRecord<String, Contract> contractRecord = new ProducerRecord<>(TOPIC_CONTRACTS, contract);
            contractProducer.send(contractRecord);
        } catch (SerializationException serializationException) {
            LOGGER.error("Unable to serialize contract: {}", serializationException.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);

    }

    public static void main(final String[] args) throws IOException {
        ProducerRunner producerRunner = new ProducerRunner();
        producerRunner.createEvents();
    }
}
