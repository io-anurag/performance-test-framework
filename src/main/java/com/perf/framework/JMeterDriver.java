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

public class JMeterDriver {
    private static final Logger log = LoggerFactory.getLogger(JMeterDriver.class);
    private StandardJMeterEngine jmeter;

    public JMeterDriver() {
        log.info("Initializing JMeterDriver...");
        this.jmeter = new StandardJMeterEngine();
        initJMeter();
        log.info("JMeterDriver Initialized.");
    }

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
                    Files.writeString(propsFile, "jmeter.save.saveservice.output_format=csv\n");
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
        JMeterUtils.initLocale();
        log.info("JMeter properties loaded from: {}", propsFile.getAbsolutePath());
    }

    public void runTest(HashTree testPlanTree) {
        log.info("Starting test execution...");

        // Add a summariser
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (!summariserName.isEmpty()) {
            summer = new Summariser(summariserName);
        }

        // Store execution results into a .jtl file (csv)
        String logFile = "test_result.jtl";
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(logFile);

        testPlanTree.add(testPlanTree.getArray()[0], logger);

        jmeter.configure(testPlanTree);
        jmeter.run();
        log.info("Test execution completed.");
    }
}
