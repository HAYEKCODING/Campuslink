package com.campuslink.realtime.dto.response;

import com.campuslink.realtime.entity.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {

    private Long id;

    /** Identifiant realtime (likes / messages) de l'autre membre du match. */
    private Long autreUtilisateurId;

    /**
     * Identité d'affichage de l'autre membre, résolue via
     * {@code UserDirectoryPort} — c'est elle que le frontend utilise pour
     * afficher un nom, une photo et ouvrir la fiche profil.
     * Les champs sont {@code null} si le compte n'a pas encore de profil.
     */
    private UUID autreUtilisateurProfilId;
    private String autreUtilisateurNom;
    private String autreUtilisateurPhoto;

    private Instant dateMatch;
    private MatchStatus statut;
}
