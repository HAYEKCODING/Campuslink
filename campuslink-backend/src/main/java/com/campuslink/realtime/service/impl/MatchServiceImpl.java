package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.MatchResponse;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.integration.UserDirectoryPort;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.repository.MatchRepository;
import com.campuslink.realtime.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final UserDirectoryPort userDirectoryPort;

    @Override
    @Transactional
    public Match creerSiAbsent(Long userA, Long userB) {
        return trouverEntre(userA, userB).orElseGet(() -> {
            // Convention utilisateur1Id < utilisateur2Id pour eviter les doublons (a,b) / (b,a)
            Long u1 = Math.min(userA, userB);
            Long u2 = Math.max(userA, userB);
            Match match = Match.builder()
                    .utilisateur1Id(u1)
                    .utilisateur2Id(u2)
                    .statut(MatchStatus.ACTIF)
                    .build();
            return matchRepository.save(match);
        });
    }

    @Override
    public Optional<Match> trouverEntre(Long userA, Long userB) {
        return matchRepository.findEntreUtilisateurs(userA, userB);
    }

    @Override
    public List<MatchResponse> listerMatchsActifs(Long userId) {
        return matchRepository.findAllByUtilisateurEtStatut(userId, MatchStatus.ACTIF).stream()
                .map(m -> toResponse(m, userId))
                .toList();
    }

    @Override
    public List<MatchResponse> listerHistorique(Long userId) {
        return matchRepository.findHistoriqueByUtilisateur(userId).stream()
                .map(m -> toResponse(m, userId))
                .toList();
    }

    @Override
    @Transactional
    public void unmatch(Long userId, Long matchId) {
        Match match = getMatchOuException(matchId);
        verifierParticipant(match, userId);
        match.setStatut(MatchStatus.ROMPU);
        match.setDateRupture(Instant.now());
        matchRepository.save(match);
    }

    @Override
    public Match getMatchOuException(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable : " + matchId));
    }

    @Override
    public void verifierParticipant(Match match, Long userId) {
        if (!match.concerne(userId)) {
            throw new ForbiddenException("Vous ne faites pas partie de ce match");
        }
    }

    private MatchResponse toResponse(Match match, Long userId) {
        Long autreUtilisateurId = match.autreUtilisateur(userId);
        UserDirectoryPort.ApercuUtilisateur apercu =
                userDirectoryPort.trouverApercu(autreUtilisateurId).orElse(null);

        return MatchResponse.builder()
                .id(match.getId())
                .autreUtilisateurId(autreUtilisateurId)
                .autreUtilisateurProfilId(apercu != null ? apercu.profilId() : null)
                .autreUtilisateurNom(apercu != null ? apercu.nom() : null)
                .autreUtilisateurPhoto(apercu != null ? apercu.photoUrl() : null)
                .dateMatch(match.getDateMatch())
                .statut(match.getStatut())
                .build();
    }
}
