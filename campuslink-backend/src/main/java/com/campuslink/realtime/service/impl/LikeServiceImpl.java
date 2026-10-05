package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.LikeResponse;
import com.campuslink.realtime.entity.Like;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.repository.LikeRepository;
import com.campuslink.realtime.service.LikeService;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique metier du Like :
 *  1. Empeche un utilisateur de se liker lui-meme ou de liker deux fois le meme profil.
 *  2. Enregistre le Like.
 *  3. Verifie si la cible a deja like l'emetteur -> si oui, cree automatiquement un Match
 *     (via MatchService) et notifie les deux utilisateurs.
 *  4. Notifie systematiquement la cible qu'elle a recu un nouveau like.
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final MatchService matchService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public LikeResponse liker(Long emetteurId, Long cibleId) {
        if (emetteurId.equals(cibleId)) {
            throw new DuplicateResourceException("Impossible de se liker soi-meme");
        }
        if (likeRepository.existsByEmetteurIdAndCibleId(emetteurId, cibleId)) {
            throw new DuplicateResourceException("Vous avez deja like ce profil");
        }

        Like like = Like.builder()
                .emetteurId(emetteurId)
                .cibleId(cibleId)
                .build();
        like = likeRepository.save(like);

        // Notifier la cible qu'elle a recu un like
        notificationService.creerEtEnvoyer(
                cibleId, NotificationType.LIKE, "Quelqu'un a aime votre profil", emetteurId);

        boolean matchCree = false;
        if (likeRepository.existsByEmetteurIdAndCibleId(cibleId, emetteurId)) {
            // La cible avait deja like l'emetteur -> match mutuel
            Match match = matchService.creerSiAbsent(emetteurId, cibleId);
            matchCree = true;

            notificationService.creerEtEnvoyer(
                    emetteurId, NotificationType.MATCH, "Vous avez un nouveau match !", match.getId());
            notificationService.creerEtEnvoyer(
                    cibleId, NotificationType.MATCH, "Vous avez un nouveau match !", match.getId());
        }

        return LikeResponse.builder()
                .id(like.getId())
                .emetteurId(like.getEmetteurId())
                .cibleId(like.getCibleId())
                .dateAction(like.getDateAction())
                .matchCree(matchCree)
                .build();
    }

    @Override
    @Transactional
    public void retirerLike(Long emetteurId, Long cibleId) {
        likeRepository.deleteByEmetteurIdAndCibleId(emetteurId, cibleId);
    }

    @Override
    public List<LikeResponse> listerMesLikes(Long emetteurId) {
        return likeRepository.findAllByEmetteurId(emetteurId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<LikeResponse> listerLikesRecus(Long userId) {
        return likeRepository.findAllByCibleId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private LikeResponse toResponse(Like like) {
        return LikeResponse.builder()
                .id(like.getId())
                .emetteurId(like.getEmetteurId())
                .cibleId(like.getCibleId())
                .dateAction(like.getDateAction())
                .matchCree(false)
                .build();
    }
}
