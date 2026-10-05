package com.campuslink.realtime.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModerationActionRequest {

    @NotNull(message = "L'identifiant de l'utilisateur cible est obligatoire")
    private Long utilisateurCibleId;

    @Size(max = 500)
    private String motif;

    private Long reportId;
}
