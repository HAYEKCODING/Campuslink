package com.campuslink.realtime.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class MessageRequest {

    @NotNull(message = "L'identifiant du match est obligatoire")
    private Long matchId;

    @NotBlank(message = "Le message ne peut pas etre vide")
    @Size(max = 2000, message = "Le message ne doit pas depasser 2000 caracteres")
    private String contenu;
}
