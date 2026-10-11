package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.LikeResponse;
import com.campuslink.realtime.entity.Like;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.repository.LikeRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.realtime.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;
    @Mock
    private MatchService matchService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LikeServiceImpl likeService;

    private static final Long EMETTEUR = 1L;
    private static final Long CIBLE = 2L;

    @BeforeEach
    void setUp() {
        // lenient() : certains tests lèvent avant tout appel à save() (like de soi-même,
        // like déjà existant) — le stubbing serait alors déclaré inutile par Mockito.
        lenient().when(userRepository.existsByLegacyId(CIBLE)).thenReturn(true);
        lenient().when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(100L);
            like.setDateAction(Instant.now());
            return like;
        });
    }

    @Test
    void liker_creeLikeSansMatch_quandPasDeLikeReciproque() {
        when(likeRepository.existsByEmetteurIdAndCibleId(EMETTEUR, CIBLE)).thenReturn(false);
        when(likeRepository.existsByEmetteurIdAndCibleId(CIBLE, EMETTEUR)).thenReturn(false);

        LikeResponse response = likeService.liker(EMETTEUR, CIBLE);

        assertThat(response.isMatchCree()).isFalse();
        verify(matchService, never()).creerSiAbsent(any(), any());
        verify(notificationService).creerEtEnvoyer(eq(CIBLE), eq(NotificationType.LIKE), any(), eq(EMETTEUR));
        verify(notificationService, never()).creerEtEnvoyer(any(), eq(NotificationType.MATCH), any(), any());
    }

    @Test
    void liker_creeMatch_quandLikeReciproqueExiste() {
        when(likeRepository.existsByEmetteurIdAndCibleId(EMETTEUR, CIBLE)).thenReturn(false);
        when(likeRepository.existsByEmetteurIdAndCibleId(CIBLE, EMETTEUR)).thenReturn(true);

        Match match = Match.builder().id(50L).utilisateur1Id(EMETTEUR).utilisateur2Id(CIBLE).build();
        when(matchService.creerSiAbsent(EMETTEUR, CIBLE)).thenReturn(match);

        LikeResponse response = likeService.liker(EMETTEUR, CIBLE);

        assertThat(response.isMatchCree()).isTrue();
        verify(matchService).creerSiAbsent(EMETTEUR, CIBLE);
        verify(notificationService).creerEtEnvoyer(eq(EMETTEUR), eq(NotificationType.MATCH), any(), eq(50L));
        verify(notificationService).creerEtEnvoyer(eq(CIBLE), eq(NotificationType.MATCH), any(), eq(50L));
    }

    @Test
    void liker_leveException_quandLikeDeSoiMeme() {
        assertThrows(DuplicateResourceException.class, () -> likeService.liker(EMETTEUR, EMETTEUR));
        verifyNoInteractions(likeRepository);
    }

    @Test
    void liker_leveException_quandDejaLike() {
        when(likeRepository.existsByEmetteurIdAndCibleId(EMETTEUR, CIBLE)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> likeService.liker(EMETTEUR, CIBLE));
        verify(likeRepository, never()).save(any());
    }

    @Test
    void liker_leve404_quandCibleInconnue() {
        // Sans cette vérification, l'insertion violerait la FK fk_like_cible
        // en base → 500 au lieu d'une erreur métier explicite (404).
        when(userRepository.existsByLegacyId(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> likeService.liker(EMETTEUR, 999L));
        verify(likeRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }
}
