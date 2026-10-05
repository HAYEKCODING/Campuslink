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
public class ReportRequest {

    @NotNull(message = "L'identifiant du profil signale est obligatoire")
    private Long cibleId;

    private Long matchId;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 100)
    private String motif;

    @Size(max = 1000)
    private String description;
}
