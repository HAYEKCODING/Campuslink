package com.campuslink.util;

/**
 * Masque partiellement une adresse email pour affichage côté client
 * (ex. dans {@code OtpResponse}), sans jamais l'exposer en clair.
 */
public final class EmailMasker {

    private EmailMasker() {
        // Classe utilitaire : instanciation interdite
    }

    /**
     * Transforme {@code "etudiant@campuslink.io"} en {@code "e******t@campuslink.io"}.
     * Pour les parties locales très courtes (≤ 2 caractères), masque tout sauf le premier.
     */
    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*".repeat(Math.max(1, localPart.length() - 1)) + domainPart;
        }

        String masked = localPart.charAt(0)
                + "*".repeat(localPart.length() - 2)
                + localPart.charAt(localPart.length() - 1);

        return masked + domainPart;
    }

}
