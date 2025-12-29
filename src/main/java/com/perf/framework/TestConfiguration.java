package com.perf.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized configuration loader for test properties.
 *
 * <p>Loads a single {@code config.properties} from the classpath once at class-load time and
 * exposes convenience accessors for typed retrieval with optional defaults.</p>
 */
public class TestConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TestConfiguration.class);

    /**
     * Prevents instantiation of this utility class.
     */
    private TestConfiguration() {
        throw new IllegalStateException("Utility class");
    }

    private static final Properties properties = new Properties();

    // Load properties once from classpath config.properties
    static {
        try (InputStream input = TestConfiguration.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Sorry, unable to find config.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            logger.error(ex.getCause().getMessage(), ex);
        }
    }

    /**
     * Returns a property value or {@code null} if absent.
     *
     * @param key property key
     * @return property value, or {@code null} when not defined
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Returns a property value, falling back to a default when missing.
     *
     * @param key          property key
     * @param defaultValue value to return when the property is absent
     * @return the configured value or {@code defaultValue}
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Returns a property parsed as an integer.
     *
     * @param key property key
     * @return integer value of the property
     * @throws NumberFormatException if the property is missing or not a valid integer
     */
    public static int getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * Returns a property parsed as an integer with a default fallback.
     *
     * @param key          property key
     * @param defaultValue value to return when the property is absent
     * @return parsed integer value or {@code defaultValue} if the key is missing
     * @throws NumberFormatException if the property exists but is not a valid integer
     */
    public static int getIntProperty(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null)
            return defaultValue;
        return Integer.parseInt(val);
    }
}
