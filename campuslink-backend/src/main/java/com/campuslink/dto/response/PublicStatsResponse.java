package com.campuslink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Statistiques publiques de la landing page — {@code GET /stats/public}.
 *
 * <p>{@code memberCount} alimente le compteur « +N étudiants déjà
 * inscrits » ; {@code avatarPhotos} quelques avatars réels pour
 * l'illustrer (jamais plus de quelques-uns : la requête est limitée
 * côté serveur).</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatsResponse {

    private long memberCount;
    private List<String> avatarPhotos;

}
