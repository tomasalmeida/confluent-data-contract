package com.tomasalmeida.data.contract.globalrules;

import com.tomasalmeida.data.contract.Client;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class ClientProducerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProducerRunner.class);

    private final KafkaProducer<String, Client> clientKafkaProducer;

    public ClientProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put("clientId", "client-producer");
        clientKafkaProducer = new KafkaProducer<>(properties);
    }

    public void createEvents() {
        try {
            // valid users id and country code
            produceClient("u-abcde", "John", "Doe", "US");
            produceClient("u-12345", "Jane", "Doe", "UK");
            produceClient("u-a1b2c", "Joe", "Smith", "CA");
            //invalid user id
            produceClient("12345", "Pierre", "Dupont", "FR");
            produceClient("abcde", "Marie", "Dupont", "FR");
            produceClient("a1b2c", "Ludovic", "Dupont", "FR");
            produceClient("u-123", "Sebastien", "Dupont", "FR");
            produceClient("u-abc", "Severine", "Dupont", "FR");
            produceClient("u-a1b", "Audrey", "Dupont", "FR");
            produceClient("u-ABCDE", "Jean", "Dupont", "FR");
            //invalid country code
            produceClient("u-45678", "Antonio", "Garcia", "");
            produceClient("u-fghij", "Maria", "Garcia", "E");
            produceClient("u-f4g5h", "Manuel", "Garcia", "es");
        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }
    }

    private void produceClient(String id, String firstName, String lastName, String countryCode) throws InterruptedException {
        Client client = new Client(id, firstName, lastName, countryCode);
        try {
            LOGGER.info("Sending Client {}", client);
            ProducerRecord<String, Client> userRecord = new ProducerRecord<>("data.clients", client);
            clientKafkaProducer.send(new ProducerRecord<>("data.clients", client));
        } catch (Exception exception) {
            LOGGER.error("Exception with client [{}]", client, exception);
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }

    private void close() {
        clientKafkaProducer.close();
    }

    public static void main(final String[] args) throws IOException {
        ClientProducerRunner clientProducerRunner = new ClientProducerRunner();
        clientProducerRunner.createEvents();
        clientProducerRunner.close();
    }
}
