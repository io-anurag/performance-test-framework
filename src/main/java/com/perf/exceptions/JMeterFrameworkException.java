package com.perf.exceptions;

/**
 * Unchecked exception representing framework-level failures in the JMeter integration.
 *
 * <p>Intended for wrapping lower-level exceptions (e.g., I/O or configuration errors)
 * encountered during JMeter bootstrap or execution so callers can handle a single
 * runtime exception type.</p>
 */
public class JMeterFrameworkException extends RuntimeException {

    /**
     * Creates an exception with a descriptive message.
     *
     * @param message human-readable description of the failure
     */
    public JMeterFrameworkException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a descriptive message and root cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying cause of the failure
     */
    public JMeterFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
