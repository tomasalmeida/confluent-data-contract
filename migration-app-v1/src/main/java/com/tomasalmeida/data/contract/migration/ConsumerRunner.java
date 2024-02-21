package com.tomasalmeida.data.contract.migration;

import com.tomasalmeida.data.contract.Product;
import com.tomasalmeida.data.contract.common.PropertiesLoader;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static com.tomasalmeida.data.contract.common.PropertiesLoader.TOPIC_PRODUCTS;

public class ConsumerRunner {
    public static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunner.class);

    private final KafkaConsumer<String, Product> consumer;

    public ConsumerRunner() throws IOException {
        Properties properties = PropertiesLoader.load("client.properties");
        properties.put(KafkaAvroDeserializerConfig.USE_LATEST_WITH_METADATA, "app_version=1");
        consumer = new KafkaConsumer<>(properties);
    }

    public void runProductConsumerV1() {
        try (consumer) {
            consumer.subscribe(Collections.singletonList(TOPIC_PRODUCTS));
            LOGGER.info("Starting Product consumer V1...");
            while (true) {
                ConsumerRecords<String, Product> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, Product> record : records) {
                    LOGGER.info("Product with V1 schema: {}", record.value());
                }
            }
        } catch (WakeupException e) {
            // ignore for shutdown
        } catch (Exception e) {
            LOGGER.error("Error in User Consumer v1", e);
        }
    }

    private void wakeUp() {
        consumer.wakeup();
    }

    public static void main(final String[] args) throws IOException {
        ConsumerRunner consumerRunner = new ConsumerRunner();

        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerRunner.wakeUp();

            // join the main thread to give time to consumer to close correctly
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                LOGGER.error("Oh man...", e);
            }
        }));

        consumerRunner.runProductConsumerV1();
   }
}
