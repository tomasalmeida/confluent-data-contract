package com.tomasalmeida.data.contract.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesLoader {

    public static final String TOPIC_PRODUCTS = "warehouse.products";

    private static final String CONFIG_PATH = "src/main/resources/%s";

    public static Properties load(final String fileName) throws IOException {

        final String configFile = String.format(CONFIG_PATH, fileName);

        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (final InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }
}
