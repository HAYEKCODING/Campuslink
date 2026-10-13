package com.campuslink.realtime.controller;

import com.campuslink.exception.ForbiddenException;
import com.campuslink.realtime.dto.request.CallSignalRequest;
import com.campuslink.realtime.dto.request.CallSignalRequest.CallMediaType;
import com.campuslink.realtime.dto.request.CallSignalRequest.CallSignalType;
import com.campuslink.realtime.dto.response.CallSignalResponse;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CallSignalingControllerTest {
    private static final String CALL_ID = "7ce2e4a3-e579-49b0-84bb-cd41b04c4e43";

    private final MatchService matchService = mock(MatchService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final CallSignalingController controller = new CallSignalingController(
            matchService, notificationService, messagingTemplate, new ObjectMapper());
    private final Match match = Match.builder().id(12L).utilisateur1Id(1L).utilisateur2Id(2L)
            .statut(MatchStatus.ACTIF).build();

    @Test
    void relaisInvitationAuthentifieeEtCreeUneNotification() {
        when(matchService.getMatchOuException(12L)).thenReturn(match);

        controller.signal(request(CallSignalType.INVITE, CallMediaType.VIDEO, null), principal(1L));

        verify(matchService).verifierParticipant(match, 1L);
        verify(notificationService).creerEtEnvoyer(2L, NotificationType.APPEL, "Appel vidéo entrant", 12L);
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getCallId().equals(CALL_ID)
                        && event.getFromUserId().equals(1L)
                        && event.getType() == CallSignalType.INVITE
                        && event.getMediaType() == CallMediaType.VIDEO));
    }

    @Test
    void relaieInvitationMemeSiLaCreationDeNotificationEchoue() {
        when(matchService.getMatchOuException(12L)).thenReturn(match);
        doThrow(new IllegalStateException("notification unavailable"))
                .when(notificationService).creerEtEnvoyer(2L, NotificationType.APPEL, "Appel audio entrant", 12L);

        controller.signal(request(CallSignalType.INVITE, CallMediaType.AUDIO, null), principal(1L));

        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getCallId().equals(CALL_ID) && event.getType() == CallSignalType.INVITE));
    }

    @Test
    void refuseUnUtilisateurQuiNestPasParticipantAvantDeDiffuserLeSignal() {
        when(matchService.getMatchOuException(12L)).thenReturn(match);
        doThrow(new ForbiddenException("not a participant"))
                .when(matchService).verifierParticipant(match, 99L);

        assertThrows(ForbiddenException.class,
                () -> controller.signal(request(CallSignalType.INVITE, CallMediaType.AUDIO, null), principal(99L)));

        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    void transmetLaNegociationSeulementApresAcceptationEtTermineLaSession() {
        when(matchService.getMatchOuException(12L)).thenReturn(match);
        controller.signal(request(CallSignalType.INVITE, CallMediaType.AUDIO, null), principal(1L));
        controller.signal(request(CallSignalType.ACCEPT, null, null), principal(2L));
        controller.signal(request(CallSignalType.OFFER, null, Map.of(
                "type", "offer", "sdp", "v=0\r\n")), principal(1L));
        controller.signal(request(CallSignalType.END, null, null), principal(2L));
        controller.signal(request(CallSignalType.ICE, null, Map.of("candidate", "candidate:data")), principal(1L));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getType() == CallSignalType.ACCEPT && event.getFromUserId().equals(2L)));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getType() == CallSignalType.OFFER && event.getFromUserId().equals(1L)));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getType() == CallSignalType.END && event.getFromUserId().equals(2L)));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("2"), eq("/queue/calls"), argThat((CallSignalResponse event) ->
                event.getType() == CallSignalType.ICE));
    }

    @Test
    void refuseUneNegociationWebRtcAvantAcceptation() {
        when(matchService.getMatchOuException(12L)).thenReturn(match);
        controller.signal(request(CallSignalType.INVITE, CallMediaType.AUDIO, null), principal(1L));

        assertThrows(IllegalArgumentException.class,
                () -> controller.signal(request(CallSignalType.OFFER, null, Map.of("sdp", "v=0")), principal(1L)));
    }

    private CallSignalRequest request(CallSignalType type, CallMediaType mediaType, Object payload) {
        CallSignalRequest request = new CallSignalRequest();
        request.setMatchId(12L);
        request.setCallId(CALL_ID);
        request.setType(type);
        request.setMediaType(mediaType);
        if (payload != null) request.setPayload(new ObjectMapper().valueToTree(payload));
        return request;
    }

    private Principal principal(Long userId) {
        return userId::toString;
    }
}
