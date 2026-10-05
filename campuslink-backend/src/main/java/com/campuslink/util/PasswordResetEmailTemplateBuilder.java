package com.campuslink.util;

/**
 * Construit le contenu (sujet + corps HTML) de l'email de réinitialisation de
 * mot de passe envoyé par {@link com.campuslink.service.EmailService}.
 *
 * <p>Même approche que {@link OtpEmailTemplateBuilder} (text blocks Java 17,
 * pas de moteur de templating) — voir sa Javadoc pour la justification.</p>
 */
public final class PasswordResetEmailTemplateBuilder {

    private PasswordResetEmailTemplateBuilder() {
        // Classe utilitaire : instanciation interdite
    }

    public static String subject() {
        return "Réinitialisation de votre mot de passe — CampusLink";
    }

    /**
     * @param recipientName     prénom de l'utilisateur si connu, sinon son email
     * @param resetLink          URL complète (avec token) vers laquelle le bouton pointe
     * @param expirationMinutes    durée de validité affichée à l'utilisateur
     */
    public static String buildHtml(String recipientName, String resetLink, int expirationMinutes) {
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
                                            <h1 style="margin:0 0 16px;font-size:20px;color:#1a1a2e;">Réinitialisez votre mot de passe</h1>
                                            <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#3d3d3a;">
                                                Bonjour %s,<br/>
                                                Une demande de réinitialisation de mot de passe a été effectuée pour votre compte CampusLink. Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.
                                            </p>
                                            <div style="text-align:center;margin:0 0 24px;">
                                                <a href="%s" style="display:inline-block;padding:14px 32px;background-color:#1a1a2e;color:#ffffff;text-decoration:none;border-radius:6px;font-size:15px;font-weight:bold;">
                                                    Réinitialiser mon mot de passe
                                                </a>
                                            </div>
                                            <p style="margin:0 0 8px;font-size:14px;color:#73726c;">
                                                Ce lien expire dans <strong>%d minutes</strong>.
                                            </p>
                                            <p style="margin:8px 0 0;font-size:12px;color:#9c9a92;word-break:break-all;">
                                                Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur : %s
                                            </p>
                                            <p style="margin:24px 0 0;font-size:12px;color:#9c9a92;">
                                                Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email : votre mot de passe restera inchangé.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(escape(recipientName), resetLink, expirationMinutes, resetLink);
    }

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
