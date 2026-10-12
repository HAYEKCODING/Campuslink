package com.campuslink.realtime.controller;

import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.realtime.dto.request.CallSignalRequest;
import com.campuslink.realtime.dto.response.CallSignalResponse;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Validated
@Slf4j
public class CallSignalingController {
    private static final int MAX_SIGNAL_BYTES = 128 * 1024;
    private static final long INVITE_TIMEOUT_SECONDS = 60;

    private final MatchService matchService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, ActiveCall> activeCalls = new ConcurrentHashMap<>();

    @MessageMapping("/calls.signal")
    public synchronized void signal(@Payload CallSignalRequest request, Principal principal) {
        if (principal == null || request == null || request.getMatchId() == null
                || request.getCallId() == null || !request.getCallId().matches(
                        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
                || request.getType() == null) {
            throw new ForbiddenException("Signal d'appel incomplet ou non authentifié.");
        }
        if (request.getPayload() != null && payloadSize(request) > MAX_SIGNAL_BYTES) {
            throw new IllegalArgumentException("Signal WebRTC trop volumineux.");
        }

        Long senderId = Long.valueOf(principal.getName());
        Match match = matchService.getMatchOuException(request.getMatchId());
        matchService.verifierParticipant(match, senderId);
        if (match.getStatut() != MatchStatus.ACTIF) {
            throw new DuplicateResourceException("Les appels sont indisponibles pour cette conversation.");
        }

        if (request.getType() == CallSignalRequest.CallSignalType.INVITE) {
            invite(request, match, senderId);
            return;
        }

        ActiveCall call = activeCalls.get(match.getId());
        if (call == null || !call.callId.equals(request.getCallId())) {
            sendError(senderId, request, "Cet appel n'est plus actif.");
            return;
        }

        Long recipientId = call.other(senderId);
        validateTransition(call, request, senderId);
        relay(request, senderId, recipientId);

        if (request.getType() == CallSignalRequest.CallSignalType.REJECT
                || request.getType() == CallSignalRequest.CallSignalType.CANCEL
                || request.getType() == CallSignalRequest.CallSignalType.END
                || request.getType() == CallSignalRequest.CallSignalType.MISSED) {
            activeCalls.remove(match.getId(), call);
        }
    }

    private void invite(CallSignalRequest request, Match match, Long senderId) {
        if (request.getMediaType() == null || request.getPayload() != null) {
            throw new IllegalArgumentException("Type d'appel invalide.");
        }
        Long recipientId = match.autreUtilisateur(senderId);
        ActiveCall existing = activeCalls.get(match.getId());
        if (existing != null && !existing.accepted
                && existing.createdAt.plusSeconds(INVITE_TIMEOUT_SECONDS).isBefore(Instant.now())) {
            activeCalls.remove(match.getId(), existing);
            existing = null;
        }
        if (existing != null) {
            sendError(senderId, request, "L'interlocuteur est déjà en appel.");
            return;
        }
        ActiveCall call = new ActiveCall(request.getCallId(), senderId, recipientId,
                request.getMediaType(), Instant.now());
        activeCalls.put(match.getId(), call);
        notificationService.creerEtEnvoyer(recipientId, NotificationType.APPEL,
                request.getMediaType() == CallSignalRequest.CallMediaType.VIDEO
                        ? "Appel vidéo entrant" : "Appel audio entrant",
                match.getId());
        relay(request, senderId, recipientId);
    }

    private void validateTransition(ActiveCall call, CallSignalRequest request, Long senderId) {
        boolean isCaller = call.callerId.equals(senderId);
        switch (request.getType()) {
            case ACCEPT, REJECT -> {
                if (isCaller || call.accepted) throw invalidTransition();
                if (request.getType() == CallSignalRequest.CallSignalType.ACCEPT) call.accepted = true;
            }
            case CANCEL, MISSED -> {
                if (!isCaller || call.accepted) throw invalidTransition();
            }
            case OFFER -> {
                if (!call.accepted || !isCaller || !hasString(request, "sdp")) throw invalidTransition();
            }
            case ANSWER -> {
                if (!call.accepted || isCaller || !hasString(request, "sdp")) throw invalidTransition();
            }
            case ICE -> {
                if (!call.accepted || request.getPayload() == null
                        || !request.getPayload().hasNonNull("candidate")) throw invalidTransition();
            }
            case END -> {
                if (!call.accepted) throw invalidTransition();
            }
            case INVITE, ERROR -> throw invalidTransition();
        }
    }

    private boolean hasString(CallSignalRequest request, String field) {
        return request.getPayload() != null && request.getPayload().path(field).isTextual()
                && !request.getPayload().path(field).asText().isBlank();
    }

    private int payloadSize(CallSignalRequest request) {
        try {
            return objectMapper.writeValueAsBytes(request.getPayload()).length;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Signal WebRTC invalide.", ex);
        }
    }

    private void relay(CallSignalRequest request, Long senderId, Long recipientId) {
        CallSignalResponse response = CallSignalResponse.builder()
                .matchId(request.getMatchId())
                .callId(request.getCallId())
                .fromUserId(senderId)
                .type(request.getType())
                .mediaType(request.getMediaType())
                .payload(request.getPayload())
                .build();
        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/calls", response);
    }

    private void sendError(Long userId, CallSignalRequest request, String message) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/calls",
                CallSignalResponse.builder()
                        .matchId(request.getMatchId())
                        .callId(request.getCallId())
                        .type(CallSignalRequest.CallSignalType.ERROR)
                        .payload(objectMapper.createObjectNode().put("error", message))
                        .build());
    }

    private IllegalArgumentException invalidTransition() {
        return new IllegalArgumentException("Transition d'appel non autorisée.");
    }

    private static final class ActiveCall {
        private final String callId;
        private final Long callerId;
        private final Long recipientId;
        private final Instant createdAt;
        private boolean accepted;

        private ActiveCall(String callId, Long callerId, Long recipientId,
                           CallSignalRequest.CallMediaType mediaType, Instant createdAt) {
            this.callId = callId;
            this.callerId = callerId;
            this.recipientId = recipientId;
            this.createdAt = createdAt;
        }

        private Long other(Long userId) {
            if (callerId.equals(userId)) return recipientId;
            if (recipientId.equals(userId)) return callerId;
            throw new ForbiddenException("Utilisateur non autorisé dans cet appel.");
        }
    }
}
