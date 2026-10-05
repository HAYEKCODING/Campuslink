package com.campuslink.dto.request;

import com.campuslink.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload commun à {@code POST /otp/send} et {@code POST /otp/resend} —
 * les deux opérations ont la même forme (voir {@code OtpController}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    private String email;

    @NotNull(message = "Le type de code OTP est obligatoire.")
    private OtpType type;

}
