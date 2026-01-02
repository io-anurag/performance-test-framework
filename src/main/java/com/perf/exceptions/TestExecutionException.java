package com.perf.exceptions;

/**
 * Exception thrown when a JMeter test execution fails or encounters errors during runtime.
 *
 * <p>This exception is specifically used to signal test execution failures, including
 * assertion failures, unexpected errors during test runs, and validation issues. It extends
 * {@link JMeterFrameworkException} to maintain consistency with the framework's exception
 * hierarchy.</p>
 */
public class TestExecutionException extends JMeterFrameworkException {

    private final String testPlanName;

    /**
     * Creates an exception with a descriptive message and test plan name.
     *
     * @param message      human-readable description of the failure
     * @param testPlanName the name of the test plan that failed
     */
    public TestExecutionException(String message, String testPlanName) {
        super(message);
        this.testPlanName = testPlanName;
    }

    /**
     * Creates an exception with a descriptive message, root cause, and test plan name.
     *
     * @param message      human-readable description of the failure
     * @param testPlanName the name of the test plan that failed
     * @param cause        the underlying cause of the failure
     */
    public TestExecutionException(String message, String testPlanName, Throwable cause) {
        super(message, cause);
        this.testPlanName = testPlanName;
    }

    public TestExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.testPlanName = null;
    }

    /**
     * Returns the name of the test plan associated with this failure.
     *
     * @return the test plan name, or null if not set
     */
    public String getTestPlanName() {
        return testPlanName;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (testPlanName != null ? " [Test Plan: " + testPlanName + "]" : "");
    }
}
