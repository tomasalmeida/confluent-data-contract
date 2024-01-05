package com.tomasalmeida.data.contract.runner;

import com.tomasalmeida.data.contract.Contract;
import com.tomasalmeida.data.contract.User;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Properties;

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_CONTRACTS;
import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_USERS;

public class ProducerRunner extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerRunner.class);

    private final KafkaProducer<String, User> userProducer;
    private final KafkaProducer<String, Contract> contractProducer;

    public ProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        userProducer = new KafkaProducer<>(properties);
        contractProducer = new KafkaProducer<>(properties);
    }

    @Override
    public void run() {
        try {
            produceUser("Tomas", "Dias Almeida", "Tomas Almeida", 39);
            produceUser("Fernando", "Perez Machado", "", 53);
            produceUser("Amon", "Ra", "", 52);
            produceUser("La", "Fontaine", "", 49);
            produceUser("Young", "Sheldon Cooper", "", 7);
            userProducer.close();
            produceContract(1, "valid contract", LocalDate.of(2022, 02, 01),  LocalDate.of(2022, 12, 31));
            produceContract(2, "expired contract", LocalDate.of(2022, 01, 01),  LocalDate.of(2021, 12, 31));
            produceContract(3, "a", LocalDate.of(2022, 01, 01),  LocalDate.of(2022, 12, 31));
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

    private void produceContract(int id, String name, LocalDate creation, LocalDate expiration) throws InterruptedException {
        Contract contract = null;
        try {
            contract = new Contract (id, name, creation.toString(), expiration.toString());
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
        producerRunner.run();
    }
}
