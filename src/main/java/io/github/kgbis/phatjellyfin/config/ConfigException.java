package io.github.kgbis.phatjellyfin.config;

public class ConfigException extends RuntimeException {

    public static final String NO_CONFIG_FILE_MSG = "Configuration file was not found. Default configuration has been created.";

    public ConfigException(String message) {
        super(message);
    }
}
