package com.campuslink.constant;

/**
 * Constantes générales, transverses à l'ensemble de l'application.
 */
public final class AppConstants {

    private AppConstants() {
        // Classe utilitaire : instanciation interdite
    }

    public static final String API_BASE_PATH = "/v1";

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

}
