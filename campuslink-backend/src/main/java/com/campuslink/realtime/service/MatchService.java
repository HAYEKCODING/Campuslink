package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.MatchResponse;
import com.campuslink.realtime.entity.Match;

import java.util.List;
import java.util.Optional;

public interface MatchService {

    /** Cree le match s'il n'existe pas deja entre les deux utilisateurs. Retourne le match (existant ou cree). */
    Match creerSiAbsent(Long userA, Long userB);

    Optional<Match> trouverEntre(Long userA, Long userB);

    List<MatchResponse> listerMatchsActifs(Long userId);

    List<MatchResponse> listerHistorique(Long userId);

    void unmatch(Long userId, Long matchId);

    Match getMatchOuException(Long matchId);

    /** Verifie que l'utilisateur fait bien partie du match, leve ForbiddenException sinon. */
    void verifierParticipant(Match match, Long userId);
}
