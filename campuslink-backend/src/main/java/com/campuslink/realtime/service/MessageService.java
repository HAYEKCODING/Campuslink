package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    /** Envoie un message dans un match : verifie que l'expediteur en fait partie, persiste,
     *  diffuse en temps reel au destinataire et notifie. */
    MessageResponse envoyer(Long expediteurId, Long matchId, String contenu);

    Page<MessageResponse> getHistorique(Long userId, Long matchId, Pageable pageable);

    /** Marque comme lus tous les messages non lus d'un match envoyes par l'AUTRE utilisateur,
     *  et diffuse l'evenement de lecture en temps reel a l'expediteur d'origine. */
    void marquerCommeLu(Long lecteurId, Long matchId);
}
