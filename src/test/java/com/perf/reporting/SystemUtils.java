package com.perf.reporting;

/**
 * System utility methods for cross-platform operations.
 */
final class SystemUtils {

    private SystemUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return true if running on Windows, false otherwise
     */
    static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }

    /**
     * Executes a shell command using the appropriate shell for the current OS.
     *
     * @param command the command to execute
     * @throws Exception if command execution fails
     */
    static void executeShellCommand(String command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (isWindows()) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        processBuilder.start().waitFor();
    }
}
