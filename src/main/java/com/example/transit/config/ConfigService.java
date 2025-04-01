package com.example.transit.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Service for loading and validating configuration from environment variables.
 */
public class ConfigService {
    // Configuration keys
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
     * Gets the singleton instance.
     */
    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Checks if configuration is valid.
     */
    public boolean isValid() {
        return apiToken != null && !apiToken.isEmpty() &&
                baseUrl != null && !baseUrl.isEmpty() &&
                agency != null && !agency.isEmpty();
    }

    /**
     * Validates configuration and prints errors if invalid.
     */
    public boolean validateConfiguration() {
        if (!isValid()) {
            System.err.println("ERROR: Missing configuration. Please check your .env file.");
            System.err.println("Required variables: " + API_TOKEN_KEY + ", " + BASE_URL_KEY + ", " + AGENCY_KEY);
            return true;
        }
        return false;
    }

    /**
     * Gets the complete feed URL.
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * Gets the feed URL with API token masked.
     */
    public String getRedactedFeedUrl() {
        if (feedUrl == null) {
            return null;
        }
        return feedUrl.replaceAll(apiToken, "[API_TOKEN]");
    }

    /**
     * Gets the API token.
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Gets the base URL.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the agency code.
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Resets the singleton instance (for testing).
     */
    public static void reset() {
        instance = null;
    }
}