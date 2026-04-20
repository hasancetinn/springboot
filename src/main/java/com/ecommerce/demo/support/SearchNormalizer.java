package com.ecommerce.demo.support;

import java.util.Locale;

public final class SearchNormalizer {

    private SearchNormalizer() {}

    /** Normalizes search input for consistent DB queries and cache keys. */
    public static String normalize(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }
}
