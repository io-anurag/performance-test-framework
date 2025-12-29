package com.perf.framework;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Driver for configuring and executing Apache JMeter test plans programmatically.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Ensure JMeter is bootstrapped even when no JMETER_HOME is provided (e.g., CI/IDE runs).</li>
 *   <li>Load JMeter properties and locale before engine creation to avoid initialization warnings.</li>
 *   <li>Run a supplied JMeter HashTree and persist results to a JTL file.</li>
 * </ul>
 *
 * <p>This class intentionally creates a minimal, temporary JMeter home with a baseline
 * jmeter.properties when the jmeter.home system property is missing so tests can run without
 * a full JMeter installation.</p>
 */
public class JMeterDriver {
    private static final Logger log = LoggerFactory.getLogger(JMeterDriver.class);
    private StandardJMeterEngine jmeter;

    /**
     * Constructs the driver and initializes JMeter before creating the engine.
     *
     * <p>Initialization order matters: JMeter properties are loaded prior to engine
     * construction to prevent null appProperties and other bootstrap issues.</p>
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
     * <p>If {@code jmeter.home} is not defined, a temporary directory is created with a minimal
     * {@code bin/jmeter.properties} to allow the engine to start cleanly in ephemeral environments.
     * Ensures properties and locale are loaded to avoid initialization warnings.</p>
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

                // Create minimal bin/jmeter.properties
                Path binDir = tempHome.resolve("bin");
                Files.createDirectories(binDir);
                Path propsFile = binDir.resolve("jmeter.properties");
                if (!Files.exists(propsFile)) {
                    // Create comprehensive jmeter.properties to avoid initialization warnings
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

        // Initialize JMeter properties to avoid null appProperties warnings
        // This ensures appProperties is properly set before any property access
        JMeterUtils.getJMeterProperties();
        JMeterUtils.initLocale();

        log.info("JMeter properties loaded from: {}", propsFile.getAbsolutePath());
    }

    /**
     * Executes the provided JMeter test plan tree.
     *
     * <p>Adds an optional {@link org.apache.jmeter.reporters.Summariser} based on the
     * {@code summariser.name} property and a {@link org.apache.jmeter.reporters.ResultCollector}
     * that writes CSV results to {@code logs/test_result.jtl}. The result collector is attached
     * to the root of the supplied tree before configuring and running the engine.</p>
     *
     * @param testPlanTree fully assembled JMeter test plan tree to execute
     */
    public void runTest(HashTree testPlanTree) {
        log.info("Starting test execution...");

        // Add a summariser
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (!summariserName.isEmpty()) {
            summer = new Summariser(summariserName);
        }

        // Store execution results into a .jtl file (csv)
        String logFile = "logs/test_result.jtl";
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(logFile);

        testPlanTree.add(testPlanTree.getArray()[0], logger);

        jmeter.configure(testPlanTree);
        jmeter.run();
        log.info("Test execution completed.");
    }
}
