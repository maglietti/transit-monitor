package com.example.transit.service;

import com.example.transit.util.TerminalUtil;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Service for centralizing configuration loading and management.
 * This class handles loading environment variables, validating configuration,
 * and constructing consistent feed URLs across all examples and the main
 * application.
 */
public class ConfigService {
    // Required configuration keys
    private static final String API_TOKEN_KEY = "API_TOKEN";
    private static final String BASE_URL_KEY = "GTFS_BASE_URL";
    private static final String AGENCY_KEY = "GTFS_AGENCY";

    // Configuration values
    private final String apiToken;
    private final String baseUrl;
    private final String agency;
    private final String feedUrl;

    // Singleton instance
    private static ConfigService instance;

    /**
     * Private constructor that loads configuration from .env file.
     * Use getInstance() to access the singleton instance.
     */
    private ConfigService() {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Get required config values
        this.apiToken = dotenv.get(API_TOKEN_KEY);
        this.baseUrl = dotenv.get(BASE_URL_KEY);
        this.agency = dotenv.get(AGENCY_KEY);

        // Pre-build the feed URL
        if (isValid()) {
            this.feedUrl = String.format("%s?api_key=%s&agency=%s", baseUrl, apiToken, agency);
        } else {
            this.feedUrl = null;
        }
    }

    /**
     * Gets the singleton instance of ConfigurationService.
     * This ensures all components use the same configuration.
     *
     * @return The ConfigurationService singleton instance
     */
    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Checks if all required configuration values are present.
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        return apiToken != null && !apiToken.isEmpty() &&
                baseUrl != null && !baseUrl.isEmpty() &&
                agency != null && !agency.isEmpty();
    }

    /**
     * Validates the configuration and prints error messages if invalid.
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateConfiguration() {
        if (!isValid()) {
            System.err.println(TerminalUtil.ANSI_RED + "ERROR: Missing configuration. Please check your .env file."
                    + TerminalUtil.ANSI_RESET);
            System.err.println(TerminalUtil.ANSI_YELLOW + "Required variables: " + API_TOKEN_KEY + ", " + BASE_URL_KEY
                    + ", " + AGENCY_KEY + TerminalUtil.ANSI_RESET);
            return false;
        }
        return true;
    }

    /**
     * Gets the feed URL constructed from configuration values.
     * Returns null if configuration is invalid.
     *
     * @return The complete feed URL
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * Gets the feed URL with the API token hidden for logging purposes.
     *
     * @return The feed URL with API token masked
     */
    public String getRedactedFeedUrl() {
        if (feedUrl == null) {
            return null;
        }
        return feedUrl.replaceAll(apiToken, "[API_TOKEN]");
    }

    /**
     * Gets the API token.
     *
     * @return The API token
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Gets the base URL.
     *
     * @return The base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the agency code.
     *
     * @return The agency code
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Resets the singleton instance for testing purposes.
     * This should only be used in test cases.
     */
    public static void reset() {
        instance = null;
    }
}