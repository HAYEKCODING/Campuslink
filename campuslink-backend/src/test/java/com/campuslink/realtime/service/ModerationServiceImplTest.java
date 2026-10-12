package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.request.ModerationActionRequest;
import com.campuslink.realtime.entity.ModerationLog;
import com.campuslink.realtime.entity.enums.ModerationActionType;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.integration.UserModerationPort;
import com.campuslink.realtime.repository.ModerationLogRepository;
import com.campuslink.realtime.service.impl.ModerationServiceImpl;
import com.campuslink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests de {@link ModerationServiceImpl} : le journal doit tracer l'action
 * avec la bonne cible, et une cible inconnue doit produire un 404 explicite
 * plutôt qu'une violation de contrainte FK en base (qui remonterait en 500).
 */
@ExtendWith(MockitoExtension.class)
class ModerationServiceImplTest {

    @Mock
    private ModerationLogRepository moderationLogRepository;

    @Mock
    private UserModerationPort userModerationPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private static final Long MODERATEUR = 1L;
    private static final Long CIBLE = 2L;

    private ModerationActionRequest request(Long cibleId) {
        ModerationActionRequest request = new ModerationActionRequest();
        request.setUtilisateurCibleId(cibleId);
        request.setMotif("Comportement inapproprie");
        return request;
    }

    @Test
    void avertir_traceLeJournalSansChangerLeStatut() {
        when(userRepository.existsByLegacyId(CIBLE)).thenReturn(true);
        when(moderationLogRepository.save(any(ModerationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerationLog log = moderationService.avertir(MODERATEUR, request(CIBLE));

        assertThat(log.getUtilisateurCibleId()).isEqualTo(CIBLE);
        assertThat(log.getModerateurId()).isEqualTo(MODERATEUR);
        assertThat(log.getAction()).isEqualTo(ModerationActionType.AVERTISSEMENT);

        // Un avertissement ne touche jamais au compte.
        verifyNoInteractions(userModerationPort);
    }

    @Test
    void avertir_leve404_quandCibleInconnue() {
        when(userRepository.existsByLegacyId(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> moderationService.avertir(MODERATEUR, request(999L)));

        verify(moderationLogRepository, never()).save(any());
    }

    @Test
    void suspendre_delegueAuPortPuisJournalise() {
        when(userRepository.existsByLegacyId(CIBLE)).thenReturn(true);
        when(moderationLogRepository.save(any(ModerationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerationLog log = moderationService.suspendre(MODERATEUR, request(CIBLE));

        verify(userModerationPort).suspendre(eq(CIBLE), anyString());
        assertThat(log.getAction()).isEqualTo(ModerationActionType.SUSPENSION);
    }

    @Test
    void historiqueRetourneLesJournalDeLaCible() {
        moderationService.historiquePourUtilisateur(CIBLE);

        verify(moderationLogRepository).findAllByUtilisateurCibleIdOrderByDateActionDesc(CIBLE);
    }
}
