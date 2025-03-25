package com.example.transit.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for terminal output and formatting.
 * This class provides common terminal operations like:
 * - ANSI color codes for text formatting
 * - Screen clearing and animation
 * - Logging utilities with timestamp
 * - Text formatting utilities
 */
public class TerminalUtil {
    // Terminal colors - ANSI escape codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_BRIGHT_BLUE = "\u001B[94m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    // Date and time formatters
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Clears the terminal screen
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Shows a simple startup animation using ASCII art
     */
    public static void showStartupAnimation() {
        String[] frames = {
                "Starting Transit Monitor [    ]",
                "Starting Transit Monitor [=   ]",
                "Starting Transit Monitor [==  ]",
                "Starting Transit Monitor [=== ]",
                "Starting Transit Monitor [====]"
        };

        try {
            for (String frame : frames) {
                System.out.print("\r" + ANSI_CYAN + frame + ANSI_RESET);
                Thread.sleep(200);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shows a simple shutdown animation
     */
    public static void showShutdownAnimation() {
        String[] frames = {
                "Stopping system [====]",
                "Stopping system [=== ]",
                "Stopping system [==  ]",
                "Stopping system [=   ]",
                "Stopping system [    ]"
        };

        try {
            for (String frame : frames) {
                System.out.print("\r" + ANSI_YELLOW + frame + ANSI_RESET);
                Thread.sleep(200);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the current terminal width if possible
     */
    public static int getTerminalWidth() {
        try {
            return Integer.parseInt(System.getenv("COLUMNS"));
        } catch (Exception e) {
            return 80; // Default width
        }
    }

    /**
     * Prints a centered box with a title
     */
    public static void printCenteredBox(String title, int width) {
        int boxWidth = Math.min(width, 80);
        int padding = (boxWidth - title.length()) / 2;

        System.out.println(ANSI_BRIGHT_BLUE + "╔" + "═".repeat(boxWidth - 2) + "╗" + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLUE + "║" + " ".repeat(padding) +
                ANSI_BOLD + title + ANSI_RESET + ANSI_BRIGHT_BLUE +
                " ".repeat(boxWidth - title.length() - padding - 2) + "║" + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLUE + "╚" + "═".repeat(boxWidth - 2) + "╝" + ANSI_RESET);
    }

    /**
     * Prints the welcome banner
     */
    public static void printWelcomeBanner() {
        System.out.println(ANSI_CYAN + ANSI_RESET + "╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                TRANSIT MONITORING SYSTEM                     ║");
        System.out.println("║                      v1.0.0                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Formats a duration in seconds to a human-readable format
     */
    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Formats a threshold name for display
     */
    public static String formatThresholdName(String key) {
        // Convert camelCase to Title Case with spaces
        String result = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    /**
     * Logs an informational message with timestamp.
     */
    public static void logInfo(String message) {
        System.out.println(
                "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " + ANSI_GREEN + "INFO: " + ANSI_RESET + message);
    }

    /**
     * Logs an error message with timestamp.
     */
    public static void logError(String message) {
        System.err.println(
                "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " + ANSI_RED + "ERROR: " + ANSI_RESET + message);
    }
}