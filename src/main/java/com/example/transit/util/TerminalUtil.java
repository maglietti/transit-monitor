package com.example.transit.util;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for terminal output and formatting.
 * Provides ANSI color codes and basic terminal operations.
 */
public class TerminalUtil {
    // Terminal colors - ANSI escape codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    // Formatters
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Clears the terminal screen.
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Prints a centered box with a title.
     */
    public static void printCenteredBox(String title, int width) {
        int boxWidth = Math.min(width, 80);
        int padding = (boxWidth - title.length()) / 2;

        System.out.println(ANSI_BLUE + "╔" + "═".repeat(boxWidth - 2) + "╗" + ANSI_RESET);
        System.out.println(ANSI_BLUE + "║" + " ".repeat(padding) +
                ANSI_BOLD + title + ANSI_RESET + ANSI_BLUE +
                " ".repeat(boxWidth - title.length() - padding - 2) + "║" + ANSI_RESET);
        System.out.println(ANSI_BLUE + "╚" + "═".repeat(boxWidth - 2) + "╝" + ANSI_RESET);
    }

    /**
     * Formats a duration in seconds to a human-readable format.
     */
    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Gets the terminal width if possible, otherwise returns default width.
     */
    public static int getTerminalWidth() {
        try {
            return Integer.parseInt(System.getenv("COLUMNS"));
        } catch (Exception e) {
            return 80; // Default width
        }
    }
}