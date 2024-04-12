package com.tomasalmeida.data.contract.globalrules;

import com.tomasalmeida.data.contract.Client;
import com.tomasalmeida.data.contract.Item;
import com.tomasalmeida.data.contract.Order;
import com.tomasalmeida.data.contract.Product;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class ClientProducerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProducerRunner.class);

    private final KafkaProducer<String, Client> clientKafkaProducer;
    private final KafkaProducer<String, Product> productKafkaProducer;
    private final KafkaProducer<Object, Order> orderKafkaProducer;

    public ClientProducerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        properties.put("clientId", "client-producer");
        clientKafkaProducer = new KafkaProducer<>(properties);
        productKafkaProducer = new KafkaProducer<>(properties);
        orderKafkaProducer = new KafkaProducer<>(properties);
    }

    public void createEvents() {
        try {
            LOGGER.info(">>> CLIENT CREATION <<<");
            // valid client id and country code
            produceClient("u-12345", "Jane", "Doe", "uk");
            produceClient("u-a1b2c", "John", "Doe", "UK");
            //invalid client id
            produceClient("12345", "Pierre", "Dupont", "fr");
            produceClient("abcde", "Marie", "Dupont", "fr");
            //invalid country code
            produceClient("u-45678", "Antonio", "Garcia", "");
            produceClient("u-fghij", "Maria", "Garcia", "E");
            clientKafkaProducer.close();
            LOGGER.info("Press any key to continue");
            System.in.read();

            LOGGER.info(">>> PRODUCT CREATION <<<");
            // valid product id and country code
            produceProduct("FOO-123456", "foo Food", "PT");
            produceProduct("BAR-123456", "bar Drink", "es");
            //invalid product id
            produceProduct("123456", "foo Food", "PT");
            produceProduct("BAR", "bar Drink", "es");
            //invalid country code
            produceProduct("FOO-123456", "foo Food", "P");
            produceProduct("BAR-123456", "bar Drink", "");
            productKafkaProducer.close();
            LOGGER.info("Press any key to continue");
            System.in.read();

            LOGGER.info(">>> ORDER CREATION <<<");
            //valid order
            produceOrder(1, "u-12345", "FOO-123456", 2);
            produceOrder(2, "u-a1b2c", "BAR-123456", 1);
            //invalid user id
            produceOrder(3, "12345", "FOO-123456", 2);
            produceOrder(4, "abcde", "BAR-123456", 1);
            //invalid product id
            produceOrder(5, "u-12345", "123456", 2);
            produceOrder(6, "u-a1b2c", "BAR", 1);
            orderKafkaProducer.close();
            LOGGER.info("Press any key to continue");
            System.in.read();

        } catch (Exception e) {
            LOGGER.error("Ops", e);
        }
    }

    private void produceClient(String id, String firstName, String lastName, String countryCode) throws InterruptedException {
        Client client = new Client(id, firstName, lastName, countryCode);
        try {
            LOGGER.info("Sending Client {}", client);
            clientKafkaProducer.send(new ProducerRecord<>("data.clients", client));
        } catch (Exception exception) {
            LOGGER.error("Failed with message: {}", exception.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }

    private void produceProduct(String id, String name, String countryCode) throws InterruptedException {
        Product product = new Product(id, name, "categoryDefault", countryCode);
        try {
            LOGGER.info("Sending product {}", product);
            productKafkaProducer.send(new ProducerRecord<>("data.products", product));
        } catch (Exception exception) {
            LOGGER.error("Failed with message: {}", exception.getCause().getMessage());
        }
        LOGGER.info("================");
        Thread.sleep(1000);
    }

    private void produceOrder(Integer orderId, String clientId, String productId, int quantity) throws InterruptedException {
        Order order = new Order(orderId, clientId, List.of(new Item(productId, quantity)));
        try {
            LOGGER.info("Sending order {}", order);
            orderKafkaProducer.send(new ProducerRecord<>("data.orders", order));
        } catch (Exception exception) {
            LOGGER.error("Failed with message: {}", exception.getCause().getMessage());
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
