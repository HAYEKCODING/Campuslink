package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.dto.response.ReadReceiptNotification;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.Message;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.repository.MessageRepository;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.MessageService;
import com.campuslink.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MatchService matchService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public MessageResponse envoyer(Long expediteurId, Long matchId, String contenu) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, expediteurId);

        if (match.getStatut() != MatchStatus.ACTIF) {
            throw new DuplicateResourceException("Ce match n'est plus actif : messagerie indisponible");
        }

        Message message = Message.builder()
                .matchId(matchId)
                .expediteurId(expediteurId)
                .contenu(contenu)
                .build();
        message = messageRepository.save(message);

        MessageResponse response = toResponse(message);

        // Diffusion temps reel au destinataire (l'autre membre du match)
        Long destinataireId = match.autreUtilisateur(expediteurId);
        messagingTemplate.convertAndSendToUser(destinataireId.toString(), "/queue/messages", response);

        notificationService.creerEtEnvoyer(
                destinataireId, NotificationType.MESSAGE, "Nouveau message recu", matchId);

        return response;
    }

    @Override
    public Page<MessageResponse> getHistorique(Long userId, Long matchId, Pageable pageable) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, userId);
        return messageRepository.findAllByMatchIdOrderByDateEnvoiDesc(matchId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void marquerCommeLu(Long lecteurId, Long matchId) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, lecteurId);

        int nbMisAJour = messageRepository.marquerCommeLu(matchId, lecteurId, Instant.now());
        if (nbMisAJour > 0) {
            Long expediteurId = match.autreUtilisateur(lecteurId);
            ReadReceiptNotification receipt = ReadReceiptNotification.builder()
                    .matchId(matchId)
                    .lecteurId(lecteurId)
                    .build();
            messagingTemplate.convertAndSendToUser(expediteurId.toString(), "/queue/read-receipts", receipt);
        }
    }

    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .matchId(m.getMatchId())
                .expediteurId(m.getExpediteurId())
                .contenu(m.getContenu())
                .dateEnvoi(m.getDateEnvoi())
                .statutLecture(m.getStatutLecture())
                .build();
    }
}
