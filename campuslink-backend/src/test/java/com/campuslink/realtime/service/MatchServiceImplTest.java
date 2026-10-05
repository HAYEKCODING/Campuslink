package com.campuslink.realtime.service;

import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.integration.UserDirectoryPort;
import com.campuslink.realtime.repository.MatchRepository;
import com.campuslink.realtime.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserDirectoryPort userDirectoryPort;

    @InjectMocks
    private MatchServiceImpl matchService;

    @Test
    void creerSiAbsent_reutiliseMatchExistant() {
        Match existant = Match.builder().id(1L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchRepository.findEntreUtilisateurs(1L, 2L)).thenReturn(Optional.of(existant));

        Match resultat = matchService.creerSiAbsent(1L, 2L);

        assertThat(resultat).isEqualTo(existant);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void creerSiAbsent_normaliseOrdreUtilisateurs() {
        when(matchRepository.findEntreUtilisateurs(5L, 2L)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        Match resultat = matchService.creerSiAbsent(5L, 2L);

        assertThat(resultat.getUtilisateur1Id()).isEqualTo(2L);
        assertThat(resultat.getUtilisateur2Id()).isEqualTo(5L);
    }

    @Test
    void unmatch_leveExceptionSiUtilisateurNonParticipant() {
        Match match = Match.builder().id(1L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(ForbiddenException.class, () -> matchService.unmatch(99L, 1L));
    }

    @Test
    void unmatch_passeLeStatutARompu() {
        Match match = Match.builder().id(1L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        matchService.unmatch(1L, 1L);

        assertThat(match.getStatut()).isEqualTo(MatchStatus.ROMPU);
        assertThat(match.getDateRupture()).isNotNull();
    }

    @Test
    void getMatchOuException_leveResourceNotFound() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> matchService.getMatchOuException(999L));
    }

    @Test
    void listerMatchsActifs_enrichitLaReponseAvecLIdentiteDeLInterlocuteur() {
        UUID profilId = UUID.randomUUID();
        Match match = Match.builder().id(7L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchRepository.findAllByUtilisateurEtStatut(1L, MatchStatus.ACTIF))
                .thenReturn(java.util.List.of(match));
        when(userDirectoryPort.trouverApercu(2L)).thenReturn(Optional.of(
                new UserDirectoryPort.ApercuUtilisateur(2L, profilId, "Awa Kone", "https://img/awa.jpg")));

        var resultat = matchService.listerMatchsActifs(1L);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getAutreUtilisateurNom()).isEqualTo("Awa Kone");
        assertThat(resultat.get(0).getAutreUtilisateurPhoto()).isEqualTo("https://img/awa.jpg");
        assertThat(resultat.get(0).getAutreUtilisateurProfilId()).isEqualTo(profilId);
    }

    @Test
    void listerMatchsActifs_repondSansIdentiteQuandLeProfilEstAbsent() {
        Match match = Match.builder().id(7L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchRepository.findAllByUtilisateurEtStatut(1L, MatchStatus.ACTIF))
                .thenReturn(java.util.List.of(match));
        when(userDirectoryPort.trouverApercu(2L)).thenReturn(Optional.empty());

        var resultat = matchService.listerMatchsActifs(1L);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getAutreUtilisateurNom()).isNull();
        assertThat(resultat.get(0).getAutreUtilisateurId()).isEqualTo(2L);
    }
}
