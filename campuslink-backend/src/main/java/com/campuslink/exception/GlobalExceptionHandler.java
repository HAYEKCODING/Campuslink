package com.campuslink.exception;

import com.campuslink.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Gestionnaire global des exceptions de l'API.
 *
 * <p>Centralise la traduction des exceptions applicatives et techniques
 * en réponses HTTP homogènes, au format {@link ErrorResponse}.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex,
                                                            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex,
                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex,
                                                                     HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getFieldErrors().entrySet().stream()
                .map(entry -> ErrorResponse.FieldErrorDetail.builder()
                        .field(entry.getKey())
                        .message(entry.getValue())
                        .build())
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors.isEmpty() ? null : fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex,
                                                            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleEmailDelivery(EmailDeliveryException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /**
     * Échec d'une opération Cloudinary (upload, suppression) — service externe
     * indisponible ou en erreur, distinct d'une faute du client.
     */
    @ExceptionHandler(MediaUploadException.class)
    public ResponseEntity<ErrorResponse> handleMediaUpload(MediaUploadException ex,
                                                             HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    /**
     * Le fichier envoyé dépasse la limite acceptée par le serveur (Spring
     * rejette la requête avant même qu'elle n'atteigne le controller) — voir
     * {@code spring.servlet.multipart.max-file-size}.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                      HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Le fichier dépasse la taille maximale autorisée par le serveur.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accès refusé : droits insuffisants.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Identifiants invalides.", request);
    }

    /**
     * Levée par Spring Security lorsque {@code UserDetails.isEnabled()} renvoie
     * {@code false} (compte banni ou désactivé) — voir {@code UserPrincipal}.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex,
                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Ce compte est désactivé ou banni.", request);
    }

    /**
     * Levée par Spring Security lorsque {@code UserDetails.isAccountNonLocked()}
     * renvoie {@code false} (compte suspendu) — voir {@code UserPrincipal}.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex,
                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Ce compte est temporairement suspendu.", request);
    }

    /**
     * Gère les erreurs de validation des DTO annotés avec {@code @Valid}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message("Une ou plusieurs erreurs de validation ont été détectées.")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Le paramètre {@code sort} d'une requête paginée référence un champ qui
     * n'existe pas sur l'entité interrogée (ex. {@code sort=nomInexistant})
     * — survient lors de la construction de la requête, avant toute exécution SQL.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Critère de tri invalide : '" + ex.getPropertyName() + "' n'existe pas.", request);
    }

    /**
     * Un {@code @RequestParam} obligatoire (ex. {@code publicId} sur les
     * endpoints du module média) est absent de la requête — distinct d'un
     * paramètre présent mais invalide (voir {@link ConstraintViolationException}).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Le paramètre '" + ex.getParameterName() + "' est obligatoire.", request);
    }

    /**
     * Gère les erreurs de validation portant sur des {@code @RequestParam}
     * ou {@code @PathVariable} directement annotés (contrôleur {@code @Validated}),
     * par opposition à {@link MethodArgumentNotValidException} qui couvre les
     * DTO annotés {@code @Valid}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                     HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message("Une ou plusieurs erreurs de validation ont été détectées.")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Corps de requête illisible : JSON malformé, fin de flux tronquée ou
     * encodage non UTF-8. C'est une erreur **client** (400) — sans ce handler,
     * l'exception remonte au filet générique et produit un 500 trompeur.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Corps de requête illisible : JSON invalide ou encodage attendu en UTF-8.", request);
    }

    /**
     * Un paramètre de chemin ou de requête ne peut pas être converti vers le
     * type attendu — ex. {@code /profiles/search?gender=SALTIQUE} (enum
     * {@code Gender} invalide) ou {@code /reference/123}. Sans ce handler,
     * l'exception retomberait sur le filet générique et produirait un 500
     * trompeur pour ce qui est une faute client.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Paramètre '" + ex.getName() + "' invalide : '" + ex.getValue() + "'.", request);
    }

    /**
     * Chemin sans aucun mapping : Spring lève {@link NoResourceFoundException}
     * (« No static resource … ») qui, sans ce handler, tombait dans le filet
     * générique → 500 + stack trace ERROR pour une faute client (test de
     * simulation : GET /api/nope/inconnu authentifié renvoyait 500).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND,
                "Ressource introuvable : " + request.getRequestURI(), request);
    }

    /** Variante « aucune route ne correspond » (config à throwExceptionIfNoHandlerFound). */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND,
                "Ressource introuvable : " + request.getRequestURI(), request);
    }

    /** Méthode HTTP non supportée par la route (ex. DELETE sur /auth/login) → 405, pas 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "Méthode " + ex.getMethod() + " non supportée par cette ressource.", request);
    }

    /** Content-Type non traitable par la route (ex. PUT JSON sur /media/upload) → 415, pas 500. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Type de contenu non pris en charge.", request);
    }

    /**
     * Filet de sécurité : toute exception non gérée explicitement retombe ici (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                  HttpServletRequest request) {
        log.error("Erreur interne non gérée sur {} : {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue. Veuillez réessayer ultérieurement.", request);
    }

    /**
     * Champs dont la valeur saisie ne doit jamais être réaffichée dans une
     * réponse d'erreur : mot de passe, jeton, code OTP, email... L'erreur
     * indique quel champ est invalide, pas ce qui a été saisi — sinon la
     * valeur atterrit dans les réponses client, les logs et l'APM
     * ("sensitive data exposure").
     */
    private static final Pattern SENSITIVE_FIELD_PATTERN =
            Pattern.compile("password|pwd|token|secret|otp|code|credential|email", Pattern.CASE_INSENSITIVE);

    /**
     * Remplace la valeur rejetée par {@code "***"} si le champ est sensible ;
     * les autres champs gardent leur valeur pour aider le client à diagnostiquer.
     */
    private static Object sanitizeRejectedValue(String fieldName, Object rejectedValue) {
        if (rejectedValue == null) {
            return null;
        }
        if (fieldName != null && SENSITIVE_FIELD_PATTERN.matcher(fieldName).find()) {
            return "***";
        }
        return rejectedValue;
    }

    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return ErrorResponse.FieldErrorDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(sanitizeRejectedValue(fieldError.getField(),
                        fieldError.getRejectedValue()))
                .build();
    }

    /**
     * Extrait uniquement le dernier segment du chemin de propriété
     * (ex. {@code delete.publicId} → {@code publicId}) pour un nom de champ
     * exploitable côté client, sans le nom de la méthode du controller.
     */
    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String fieldName = propertyPath.contains(".")
                ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                : propertyPath;

        return ErrorResponse.FieldErrorDetail.builder()
                .field(fieldName)
                .message(violation.getMessage())
                .rejectedValue(sanitizeRejectedValue(fieldName,
                        violation.getInvalidValue()))
                .build();
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.name())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

}
