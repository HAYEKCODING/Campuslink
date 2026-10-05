package com.campuslink.service.impl;

import com.campuslink.entity.Profile;
import com.campuslink.entity.User;
import com.campuslink.enums.OtpType;
import com.campuslink.exception.EmailDeliveryException;
import com.campuslink.service.EmailService;
import com.campuslink.util.OtpEmailTemplateBuilder;
import com.campuslink.util.PasswordResetEmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implémentation de {@link EmailService} basée sur {@link JavaMailSender}
 * (Spring Mail), avec contenu HTML généré par {@link OtpEmailTemplateBuilder}
 * et {@link PasswordResetEmailTemplateBuilder}.
 *
 * <p>Toute erreur d'envoi (SMTP injoignable, timeout, identifiants invalides)
 * est traduite en {@link EmailDeliveryException} — une exception explicite
 * plutôt qu'un échec silencieux, pour que l'appelant puisse décider comment
 * réagir (voir {@code AuthServiceImpl.register}, qui journalise sans faire
 * échouer l'inscription).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    public void sendOtpCode(User user, String code, OtpType type, int expirationMinutes) {
        String recipientName = resolveRecipientName(user);
        String subject = OtpEmailTemplateBuilder.subjectFor(type);
        String htmlBody = OtpEmailTemplateBuilder.buildHtml(recipientName, code, type, expirationMinutes);

        send(user.getEmail(), subject, htmlBody, "OTP (" + type + ")");
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetLink, int expirationMinutes) {
        String recipientName = resolveRecipientName(user);
        String subject = PasswordResetEmailTemplateBuilder.subject();
        String htmlBody = PasswordResetEmailTemplateBuilder.buildHtml(recipientName, resetLink, expirationMinutes);

        send(user.getEmail(), subject, htmlBody, "réinitialisation de mot de passe");
    }

    /**
     * Point d'envoi unique, factorisé pour éviter de dupliquer la construction
     * du {@link MimeMessage} et la gestion d'erreur entre les deux méthodes publiques.
     */
    private void send(String toAddress, String subject, String htmlBody, String logLabel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email {} envoyé à {}", logLabel, toAddress);

        } catch (MessagingException | MailException ex) {
            log.error("Échec de l'envoi de l'email {} à {} : {}", logLabel, toAddress, ex.getMessage());
            throw new EmailDeliveryException(
                    "Impossible d'envoyer l'email pour le moment. Veuillez réessayer ultérieurement.", ex);
        }
    }

    /**
     * Utilise le prénom du profil s'il est disponible, sinon retombe sur l'email —
     * le profil peut être absent à ce stade (ex. juste après l'inscription, avant
     * que la transaction ne soit pleinement propagée selon l'ordre d'appel).
     */
    private String resolveRecipientName(User user) {
        Profile profile = user.getProfile();
        if (profile != null && StringUtils.hasText(profile.getFirstName())) {
            return profile.getFirstName();
        }
        return user.getEmail();
    }

}
