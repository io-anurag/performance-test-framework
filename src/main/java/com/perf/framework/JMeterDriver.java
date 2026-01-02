package com.perf.framework;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Driver for configuring and executing Apache JMeter test plans
 * programmatically.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Ensure JMeter is bootstrapped even when no JMETER_HOME is provided (e.g.,
 * CI/IDE runs).</li>
 * <li>Load JMeter properties and locale before engine creation to avoid
 * initialization warnings.</li>
 * <li>Run a supplied JMeter HashTree and persist results to a JTL file.</li>
 * </ul>
 *
 * <p>
 * This class intentionally creates a minimal, temporary JMeter home with a
 * baseline
 * jmeter.properties when the jmeter.home system property is missing so tests
 * can run without
 * a full JMeter installation.
 * </p>
 */
public class JMeterDriver {
    private static final Logger log = LoggerFactory.getLogger(JMeterDriver.class);
    private static final AtomicBoolean JTL_INITIALIZED = new AtomicBoolean(false);
    private StandardJMeterEngine jmeter;

    /**
     * Constructs the driver and initializes JMeter before creating the engine.
     *
     * <p>
     * Initialization order matters: JMeter properties are loaded prior to engine
     * construction to prevent null appProperties and other bootstrap issues.
     * </p>
     */
    public JMeterDriver() {
        log.info("Initializing JMeterDriver...");
        initJMeter(); // Initialize JMeter properties BEFORE creating engine
        this.jmeter = new StandardJMeterEngine();
        log.info("JMeterDriver Initialized.");
    }

    /**
     * Initializes JMeter configuration and environment.
     *
     * <p>
     * If {@code jmeter.home} is not defined, a temporary directory is created with
     * a minimal
     * {@code bin/jmeter.properties} to allow the engine to start cleanly in
     * ephemeral environments.
     * Ensures properties and locale are loaded to avoid initialization warnings.
     * </p>
     *
     * @throws JMeterFrameworkException if a temporary JMeter home cannot be created
     */
    private void initJMeter() {
        // We need to set JMeter home so it can find properties
        String jmeterHome = System.getProperty("jmeter.home");
        if (jmeterHome == null) {
            try {
                Path tempHome = Files.createTempDirectory("jmeter-home");
                jmeterHome = tempHome.toAbsolutePath().toString();
                System.setProperty("jmeter.home", jmeterHome);
                log.info("Created temporary JMeter home: {}", jmeterHome);

                Path binDir = tempHome.resolve("bin");
                Files.createDirectories(binDir);
                Path propsFile = binDir.resolve("jmeter.properties");
                if (!Files.exists(propsFile)) {
                    String properties = """
                            # JMeter Properties
                            jmeter.save.saveservice.output_format=csv

                            # Engine properties to avoid null appProperties warnings
                            server.exitaftertest=false
                            jmeterengine.remote.system.exit=false
                            jmeterengine.stopfail.system.exit=true
                            jmeterengine.force.system.exit=false

                            # Additional recommended properties
                            jmeterengine.nongui.port=4445
                            jmeter.reportgenerator.apdex_satisfied_threshold=500
                            jmeter.reportgenerator.apdex_tolerated_threshold=1500
                            """;
                    Files.writeString(propsFile, properties);
                }
            } catch (IOException e) {
                throw new JMeterFrameworkException("Failed to create temp JMeter home", e);
            }
        }

        File home = new File(jmeterHome);
        File binDir = new File(home, "bin");
        File propsFile = new File(binDir, "jmeter.properties");

        JMeterUtils.setJMeterHome(jmeterHome);
        JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
        JMeterUtils.getJMeterProperties();
        JMeterUtils.initLocale();
        log.info("JMeter properties loaded from: {}", propsFile.getAbsolutePath());
    }

    /**
     * Executes the provided JMeter test plan tree.
     *
     * <p>
     * Adds an optional {@link org.apache.jmeter.reporters.Summariser} based on the
     * {@code summariser.name} property and a
     * {@link org.apache.jmeter.reporters.ResultCollector}
     * that writes CSV results to {@code logs/test_result.jtl}. The result collector
     * is attached
     * to the root of the supplied tree before configuring and running the engine.
     * </p>
     *
     * @param testPlanTree fully assembled JMeter test plan tree to execute
     * @param testPlanName logical name used for logging and reporting context
     */
    public void runTest(HashTree testPlanTree, String testPlanName) {
        log.info("Starting test execution for plan: {}", testPlanName);

        Path jtlPath = resolveJtlPath();
        prepareLogFile(jtlPath);
        ResultCollector logger = createResultCollector(jtlPath.toString());

        testPlanTree.add(testPlanTree.getArray()[0], logger);

        jmeter.configure(testPlanTree);
        jmeter.run();
        log.info("Test execution completed for plan: {}. Results written to {}", testPlanName,
                jtlPath.toAbsolutePath());
    }

    /**
     * Ensure a clean output destination for the JTL results.
     *
     * <p>
     * Deletes any pre-existing JTL file to avoid appending stale data and creates
     * the logs directory when missing so the result collector can write
     * successfully.
     * </p>
     *
     * @param jtlPath target results file to prepare
     */
    private void prepareLogFile(Path jtlPath) {
        // Delete the JTL only once per JVM to prevent mid-suite deletions in parallel runs
        if (JTL_INITIALIZED.compareAndSet(false, true)) {
            try {
                if (Files.deleteIfExists(jtlPath)) {
                    log.info("Deleted existing JTL file: {}", jtlPath.toAbsolutePath());
                }
            } catch (IOException e) {
                log.warn("Failed to delete existing JTL file: {}", jtlPath.toAbsolutePath(), e);
            }
        }

        try {
            Path parent = jtlPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("Created logs directory: {}", parent.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new JMeterFrameworkException("Failed to prepare logs directory for JTL output", e);
        }
    }

    /**
     * Resolve the JTL results path from configuration.
     *
     * <p>
     * Reads the "jtl.path" property from TestConfiguration and falls back to
     * "logs/test_result.jtl" when unset or blank.
     * </p>
     *
     * @return path to the JTL/CSV results file
     */
    private Path resolveJtlPath() {
        String configured = TestConfiguration.getProperty("jtl.path");
        String path = (configured != null && !configured.isBlank()) ? configured : "logs/test_result.jtl";
        return Paths.get(path);
    }

    /**
     * Create a ResultCollector configured with an optional Summariser and the
     * provided log path.
     *
     * <p>
     * The summariser name is sourced from the {@code summariser.name} property. If
     * present, a {@link Summariser} is attached; otherwise results are collected
     * without summarisation. The collector writes to the supplied CSV path.
     * </p>
     *
     * @param logFilePath path to the JTL/CSV results file
     * @return configured ResultCollector instance ready to attach to the test plan
     */
    private ResultCollector createResultCollector(String logFilePath) {
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (!summariserName.isEmpty()) {
            summer = new Summariser(summariserName);
        }

        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(logFilePath);
        return logger;
    }
}
