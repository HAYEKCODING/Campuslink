package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.dto.response.ChatMediaDownload;
import com.campuslink.realtime.dto.response.ChatMediaLibraryItemResponse;
import com.campuslink.realtime.entity.enums.ChatMediaCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MessageService {

    /** Envoie un message dans un match : verifie que l'expediteur en fait partie, persiste,
     *  diffuse en temps reel au destinataire et notifie. */
    MessageResponse envoyer(Long expediteurId, Long matchId, String contenu);

    MessageResponse envoyerMedia(Long expediteurId, Long matchId, MultipartFile file);

    Page<MessageResponse> getHistorique(Long userId, Long matchId, Pageable pageable);

    Page<ChatMediaLibraryItemResponse> getMediaLibrary(Long userId, Long matchId,
                                                        ChatMediaCategory category, Pageable pageable);

    ChatMediaDownload getMedia(Long userId, Long matchId, Long messageId);

    /** Marque comme lus tous les messages non lus d'un match envoyes par l'AUTRE utilisateur,
     *  et diffuse l'evenement de lecture en temps reel a l'expediteur d'origine. */
    void marquerCommeLu(Long lecteurId, Long matchId);
}
