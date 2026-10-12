package com.campuslink.realtime.service.impl;

import com.campuslink.config.ChatMediaProperties;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.dto.response.ChatMediaDownload;
import com.campuslink.realtime.dto.response.ChatMediaLibraryItemResponse;
import com.campuslink.realtime.dto.response.ChatMediaResponse;
import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.dto.response.ReadReceiptNotification;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.Message;
import com.campuslink.realtime.entity.enums.ChatMediaCategory;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.repository.MessageRepository;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.MessageService;
import com.campuslink.realtime.service.NotificationService;
import com.campuslink.realtime.service.ChatMediaFileValidator;
import com.campuslink.realtime.service.ChatMediaFileValidator.ValidatedChatMedia;
import com.campuslink.realtime.service.ChatMediaStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>]+", Pattern.CASE_INSENSITIVE);
    private static final String URL_TRAILING_PUNCTUATION = ".,!?;:)\\]}";

    private final MessageRepository messageRepository;
    private final MatchService matchService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMediaStorage chatMediaStorage;
    private final ChatMediaProperties chatMediaProperties;

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

        // Le destinataire ne reçoit le message qu'une fois sa persistance validée.
        Long destinataireId = match.autreUtilisateur(expediteurId);
        afterCommit(() ->
                messagingTemplate.convertAndSendToUser(destinataireId.toString(), "/queue/messages", response));

        notificationService.creerEtEnvoyer(
                destinataireId, NotificationType.MESSAGE, "Nouveau message recu", matchId);

        return response;
    }

    @Override
    @Transactional
    public MessageResponse envoyerMedia(Long expediteurId, Long matchId, MultipartFile file) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, expediteurId);
        verifierMatchActif(match);

        ValidatedChatMedia validated = ChatMediaFileValidator.validate(file, chatMediaProperties);
        String storageKey = chatMediaStorage.store(file, validated.extension());
        registerRollbackCleanup(storageKey);

        Message message = Message.builder()
                .matchId(matchId)
                .expediteurId(expediteurId)
                .contenu("")
                .mediaStorageKey(storageKey)
                .mediaOriginalName(validated.fileName())
                .mediaContentType(validated.contentType())
                .mediaSizeBytes(validated.sizeBytes())
                .build();
        message = messageRepository.saveAndFlush(message);
        MessageResponse response = toResponse(message);

        Long destinataireId = match.autreUtilisateur(expediteurId);
        afterCommit(() ->
                messagingTemplate.convertAndSendToUser(destinataireId.toString(), "/queue/messages", response));
        notificationService.creerEtEnvoyer(
                destinataireId, NotificationType.MESSAGE, "Nouvelle pièce jointe reçue", matchId);
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
    public Page<ChatMediaLibraryItemResponse> getMediaLibrary(Long userId, Long matchId,
                                                               ChatMediaCategory category, Pageable pageable) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, userId);
        return messageRepository.findMediaLibraryItems(matchId, category.name(), pageable)
                .map(message -> toLibraryItem(message, category));
    }

    @Override
    public ChatMediaDownload getMedia(Long userId, Long matchId, Long messageId) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, userId);
        Message message = messageRepository.findByIdAndMatchId(messageId, matchId)
                .filter(item -> item.getMediaStorageKey() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
        Resource resource = chatMediaStorage.load(message.getMediaStorageKey())
                .orElseThrow(() -> new ResourceNotFoundException("Fichier de la pièce jointe introuvable"));
        return new ChatMediaDownload(resource, message.getMediaOriginalName(),
                message.getMediaContentType(), message.getMediaSizeBytes());
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

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void verifierMatchActif(Match match) {
        if (match.getStatut() != MatchStatus.ACTIF) {
            throw new DuplicateResourceException("Ce match n'est plus actif : messagerie indisponible");
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            chatMediaStorage.delete(storageKey);
                        } catch (RuntimeException ex) {
                            log.error("Impossible de nettoyer la pièce jointe après annulation : {}", storageKey, ex);
                        }
                    }
                }
            });
        }
    }

    private ChatMediaLibraryItemResponse toLibraryItem(Message message, ChatMediaCategory category) {
        if (category == ChatMediaCategory.LINKS) {
            return ChatMediaLibraryItemResponse.builder()
                    .messageId(message.getId())
                    .senderId(message.getExpediteurId())
                    .dateEnvoi(message.getDateEnvoi())
                    .category(category.name())
                    .links(extractLinks(message.getContenu()))
                    .build();
        }
        return ChatMediaLibraryItemResponse.builder()
                .messageId(message.getId())
                .senderId(message.getExpediteurId())
                .dateEnvoi(message.getDateEnvoi())
                .category(category.name())
                .fileName(message.getMediaOriginalName())
                .contentType(message.getMediaContentType())
                .sizeBytes(message.getMediaSizeBytes())
                .links(List.of())
                .build();
    }

    private List<String> extractLinks(String content) {
        if (content == null || content.isBlank()) return List.of();
        Matcher matcher = URL_PATTERN.matcher(content);
        return matcher.results()
                .map(result -> trimLinkPunctuation(result.group()))
                .filter(link -> !link.isBlank())
                .distinct()
                .toList();
    }

    private String trimLinkPunctuation(String link) {
        int end = link.length();
        while (end > 0 && URL_TRAILING_PUNCTUATION.indexOf(link.charAt(end - 1)) >= 0) end--;
        return link.substring(0, end);
    }

    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .matchId(m.getMatchId())
                .expediteurId(m.getExpediteurId())
                .contenu(m.getContenu())
                .dateEnvoi(m.getDateEnvoi())
                .statutLecture(m.getStatutLecture())
                .media(m.getMediaStorageKey() == null ? null : ChatMediaResponse.builder()
                        .fileName(m.getMediaOriginalName())
                        .contentType(m.getMediaContentType())
                        .sizeBytes(m.getMediaSizeBytes())
                        .build())
                .build();
    }
}
