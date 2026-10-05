package com.campuslink.util;

import com.campuslink.enums.OtpType;

/**
 * Construit le contenu (sujet + corps HTML) des emails OTP envoyés par
 * {@link com.campuslink.service.EmailService}.
 *
 * <p>Implémenté comme un simple assembleur de chaînes de caractères (Java 17
 * text blocks) plutôt qu'avec un moteur de templating (Thymeleaf/FreeMarker) :
 * le besoin — un seul type de contenu, quelques variantes de copy selon
 * {@link OtpType} — ne justifie pas la dépendance supplémentaire.</p>
 */
public final class OtpEmailTemplateBuilder {

    private OtpEmailTemplateBuilder() {
        // Classe utilitaire : instanciation interdite
    }

    public static String subjectFor(OtpType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> "Vérifiez votre adresse email — CampusLink";
            case PASSWORD_RESET -> "Réinitialisation de votre mot de passe — CampusLink";
            case PHONE_VERIFICATION -> "Vérifiez votre numéro de téléphone — CampusLink";
            case TWO_FACTOR_AUTH -> "Votre code de connexion — CampusLink";
        };
    }

    /**
     * Construit le corps HTML complet de l'email.
     *
     * @param recipientName     prénom de l'utilisateur si connu, sinon son email
     * @param code               le code OTP en clair (jamais loggé ailleurs que dans cet email)
     * @param type                détermine le titre et le texte d'introduction
     * @param expirationMinutes    durée de validité affichée à l'utilisateur
     */
    public static String buildHtml(String recipientName, String code, OtpType type, int expirationMinutes) {
        String heading = headingFor(type);
        String intro = introFor(type);

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:Helvetica,Arial,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:32px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;">
                                    <tr>
                                        <td style="background-color:#1a1a2e;padding:24px 32px;">
                                            <span style="color:#ffffff;font-size:20px;font-weight:bold;">CampusLink</span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px;">
                                            <h1 style="margin:0 0 16px;font-size:20px;color:#1a1a2e;">%s</h1>
                                            <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#3d3d3a;">
                                                Bonjour %s,<br/>%s
                                            </p>
                                            <div style="text-align:center;margin:0 0 24px;">
                                                <span style="display:inline-block;padding:16px 32px;background-color:#f4f5f7;border-radius:6px;font-size:32px;font-weight:bold;letter-spacing:8px;color:#1a1a2e;">%s</span>
                                            </div>
                                            <p style="margin:0 0 8px;font-size:14px;color:#73726c;">
                                                Ce code expire dans <strong>%d minutes</strong>.
                                            </p>
                                            <p style="margin:24px 0 0;font-size:12px;color:#9c9a92;">
                                                Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(heading, escape(recipientName), intro, code, expirationMinutes);
    }

    private static String headingFor(OtpType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> "Vérifiez votre adresse email";
            case PASSWORD_RESET -> "Réinitialisez votre mot de passe";
            case PHONE_VERIFICATION -> "Vérifiez votre numéro de téléphone";
            case TWO_FACTOR_AUTH -> "Votre code de connexion";
        };
    }

    private static String introFor(OtpType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> "Utilisez le code ci-dessous pour confirmer votre adresse email sur CampusLink.";
            case PASSWORD_RESET -> "Utilisez le code ci-dessous pour réinitialiser votre mot de passe.";
            case PHONE_VERIFICATION -> "Utilisez le code ci-dessous pour confirmer votre numéro de téléphone.";
            case TWO_FACTOR_AUTH -> "Utilisez le code ci-dessous pour terminer votre connexion.";
        };
    }

    /**
     * Échappement HTML minimal du nom affiché (défense en profondeur : le prénom
     * provient d'une donnée utilisateur et se retrouve injecté tel quel dans le HTML).
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

}
