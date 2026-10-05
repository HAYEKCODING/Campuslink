package com.campuslink.dto.request;

import com.campuslink.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload de vérification — {@code POST /otp/verify}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    private String email;

    @NotBlank(message = "Le code est obligatoire.")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Le code doit être numérique (4 à 8 chiffres).")
    private String code;

    @NotNull(message = "Le type de code OTP est obligatoire.")
    private OtpType type;

}
