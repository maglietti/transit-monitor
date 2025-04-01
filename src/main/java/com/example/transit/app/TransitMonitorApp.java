package com.example.transit.app;

import com.example.transit.config.ConfigService;
import com.example.transit.config.IgniteConnectionManager;
import com.example.transit.config.VehiclePositionTableManager;
import com.example.transit.service.DataIngestionService;
import com.example.transit.service.GtfsService;
import com.example.transit.service.MonitorService;
import com.example.transit.service.ReportingService;
import com.example.transit.util.TerminalUtil;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main application for transit monitoring system.
 * Controls data ingestion, monitoring, and dashboard display.
 */
public class TransitMonitorApp {

    private static final Logger logger = LogManager.getLogger(TransitMonitorApp.class);

    // Configuration constants
    private static final int INGESTION_INTERVAL = 30;  // seconds
    private static final int MONITORING_INTERVAL = 60; // seconds
    private static final int DASHBOARD_REFRESH = 10;   // seconds

    // View constants
    private static final int VIEW_SUMMARY = 0;
    private static final int VIEW_ALERTS = 1;
    private static final int VIEW_DETAILS = 2;
    private static final int TOTAL_VIEWS = 3;

    /**
     * Main method to run the transit monitoring application.
     */
    public static void main(String[] args) {

        // Handle JUL logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        // Get configuration
        ConfigService config = ConfigService.getInstance();
        if (config.validateConfiguration()) {
            return;
        }

        // Setup UI
        TerminalUtil.clearScreen();
        printWelcomeBanner();
        showStartupAnimation();

        // Use try-with-resources for automatic resource management
        try (IgniteConnectionManager connectionManager = new IgniteConnectionManager()) {
            // Setup services
            GtfsService gtfsService = new GtfsService(config.getFeedUrl());
            DataIngestionService ingestionService = new DataIngestionService(gtfsService, connectionManager)
                    .withBatchSize(100);
            MonitorService monitorService = new MonitorService(connectionManager);
            ReportingService reportingService = new ReportingService(connectionManager.getClient());

            // Set quiet mode to suppress individual alert output
            monitorService.setQuietMode(true);

            // Setup database
            System.out.println("Setting up database schema...");
            VehiclePositionTableManager tableManager = new VehiclePositionTableManager(connectionManager);
            if (!tableManager.createSchema()) {
                System.err.println("Schema creation failed. Aborting.");
                return;
            }

            // Start services
            System.out.println("Starting data ingestion (interval: " + INGESTION_INTERVAL + "s)");
            ingestionService.start(INGESTION_INTERVAL);

            System.out.println("Starting monitoring (interval: " + MONITORING_INTERVAL + "s)");
            monitorService.startMonitoring(MONITORING_INTERVAL);

            // Start dashboard
            AtomicInteger currentView = new AtomicInteger(0);
            ScheduledExecutorService dashboardScheduler = startDashboard(currentView, reportingService,
                    ingestionService, monitorService);

            // Show running message
            System.out.println("\n" + TerminalUtil.ANSI_BOLD + "═".repeat(60) + TerminalUtil.ANSI_RESET);
            System.out.println(TerminalUtil.ANSI_GREEN + "Transit monitoring system is running" + TerminalUtil.ANSI_RESET);
            System.out.println(TerminalUtil.ANSI_BLUE + "Press ENTER to exit" + TerminalUtil.ANSI_RESET);
            System.out.println(TerminalUtil.ANSI_BOLD + "═".repeat(60) + TerminalUtil.ANSI_RESET + "\n");

            // Wait for user input to exit
            try {
                new Scanner(System.in).nextLine();
            } catch (Exception e) {
                try {
                    Thread.sleep(60000); // Wait 1 minute if input doesn't work
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            // Stop all services
            System.out.println("Stopping Transit Monitoring System");
            showShutdownAnimation();
            shutdownScheduler(dashboardScheduler);
            monitorService.stopMonitoring();
            ingestionService.stop();

            System.out.println(TerminalUtil.ANSI_GREEN + "System stopped" + TerminalUtil.ANSI_RESET);

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
        }
    }

    /**
     * Start the dashboard display scheduler.
     */
    private static ScheduledExecutorService startDashboard(AtomicInteger currentView,
                                                           ReportingService reportingService,
                                                           DataIngestionService ingestionService,
                                                           MonitorService monitorService) {
        System.out.println("Starting dashboard (refresh: " + DASHBOARD_REFRESH + "s)");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Rotate through views
                displayDashboard(currentView.get(), reportingService, ingestionService, monitorService);
                currentView.set((currentView.get() + 1) % TOTAL_VIEWS);
            } catch (Exception e) {
                System.err.println("Dashboard error: " + e.getMessage());
            }
        }, DASHBOARD_REFRESH, DASHBOARD_REFRESH, TimeUnit.SECONDS);

        return scheduler;
    }

    /**
     * Display the dashboard with the specified view.
     */
    private static void displayDashboard(int viewType, ReportingService reportingService,
                                         DataIngestionService ingestionService,
                                         MonitorService monitorService) {
        int terminalWidth = TerminalUtil.getTerminalWidth();
        TerminalUtil.clearScreen();

        // Display header
        reportingService.printDashboardHeader(terminalWidth);

        // Display view title
        String viewTitle = reportingService.getViewTitle(viewType);
        System.out.println(TerminalUtil.ANSI_BOLD + TerminalUtil.ANSI_YELLOW + viewTitle + TerminalUtil.ANSI_RESET);
        System.out.println(TerminalUtil.ANSI_CYAN + "─".repeat(Math.min(terminalWidth, 80)) + TerminalUtil.ANSI_RESET);

        // Display view content
        switch (viewType) {
            case VIEW_SUMMARY:
                displaySummaryView(reportingService, ingestionService);
                break;
            case VIEW_ALERTS:
                displayAlertsView(reportingService, monitorService);
                break;
            case VIEW_DETAILS:
                displayDetailsView(reportingService, ingestionService);
                break;
        }

        // Display footer
        reportingService.printDashboardFooter(DASHBOARD_REFRESH);
    }

    /**
     * Display the summary view.
     */
    private static void displaySummaryView(ReportingService reportingService, DataIngestionService ingestionService) {
        // Active vehicles section
        System.out.println(TerminalUtil.ANSI_BOLD + "ACTIVE VEHICLES BY ROUTE" + TerminalUtil.ANSI_RESET);
        reportingService.displayActiveVehicles();

        // Vehicle status section
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BOLD + "VEHICLE STATUS DISTRIBUTION" + TerminalUtil.ANSI_RESET);
        reportingService.displayVehicleStatuses();

        // Ingestion status section
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BOLD + "DATA INGESTION STATUS" + TerminalUtil.ANSI_RESET);
        reportingService.displayIngestionStatus(ingestionService.getStatistics());
    }

    /**
     * Display the alerts view.
     */
    private static void displayAlertsView(ReportingService reportingService, MonitorService monitorService) {
        // Recent alerts section
        System.out.println(TerminalUtil.ANSI_BOLD + "RECENT SERVICE ALERTS" + TerminalUtil.ANSI_RESET);
        reportingService.displayRecentAlerts(monitorService.getRecentAlerts());

        // Alert statistics section
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BOLD + "ALERT STATISTICS" + TerminalUtil.ANSI_RESET);
        reportingService.displayAlertStatistics(monitorService.getAlertCounts());
    }

    /**
     * Display the details view.
     */
    private static void displayDetailsView(ReportingService reportingService, DataIngestionService ingestionService) {
        // System statistics section
        System.out.println(TerminalUtil.ANSI_BOLD + "SYSTEM STATISTICS" + TerminalUtil.ANSI_RESET);
        reportingService.displaySystemStatistics();

        // Monitoring thresholds section
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BOLD + "MONITORING THRESHOLDS" + TerminalUtil.ANSI_RESET);
        reportingService.displayMonitoringThresholds();

        // Connection status section
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BOLD + "CONNECTION STATUS" + TerminalUtil.ANSI_RESET);
        reportingService.displayConnectionStatus(ingestionService.getStatistics());
    }

    /**
     * Helper to safely shutdown a scheduler.
     */
    private static void shutdownScheduler(ScheduledExecutorService scheduler) {
        if (scheduler != null) {
            scheduler.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a bit for existing tasks to terminate
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    // Cancel currently executing tasks
                    scheduler.shutdownNow();

                    // Wait a bit for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Scheduler did not terminate cleanly");
                    }
                }
            } catch (InterruptedException e) {
                // Re-cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Prints the welcome banner.
     */
    private static void printWelcomeBanner() {
        System.out.println(TerminalUtil.ANSI_CYAN + "╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                TRANSIT MONITORING SYSTEM                     ║");
        System.out.println("║                      v1.0.0                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝" + TerminalUtil.ANSI_RESET);
    }

    /**
     * Shows a simple startup animation using ASCII art.
     */
    private static void showStartupAnimation() {
        String[] frames = {
                "Starting Transit Monitor [    ]",
                "Starting Transit Monitor [=   ]",
                "Starting Transit Monitor [==  ]",
                "Starting Transit Monitor [=== ]",
                "Starting Transit Monitor [====]"
        };

        try {
            for (String frame : frames) {
                System.out.print("\r" + TerminalUtil.ANSI_CYAN + frame + TerminalUtil.ANSI_RESET);
                Thread.sleep(200);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shows a simple shutdown animation.
     */
    private static void showShutdownAnimation() {
        String[] frames = {
                "Stopping system [====]",
                "Stopping system [=== ]",
                "Stopping system [==  ]",
                "Stopping system [=   ]",
                "Stopping system [    ]"
        };

        try {
            for (String frame : frames) {
                System.out.print("\r" + TerminalUtil.ANSI_YELLOW + frame + TerminalUtil.ANSI_RESET);
                Thread.sleep(200);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}