package com.perf.framework;

public class JMeterFrameworkException extends RuntimeException {
    public JMeterFrameworkException(String message) {
        super(message);
    }

    public JMeterFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
