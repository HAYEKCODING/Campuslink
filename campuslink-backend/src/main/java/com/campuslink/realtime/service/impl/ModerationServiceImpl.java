package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.request.ModerationActionRequest;
import com.campuslink.realtime.entity.ModerationLog;
import com.campuslink.realtime.entity.enums.ModerationActionType;
import com.campuslink.realtime.integration.UserModerationPort;
import com.campuslink.realtime.repository.ModerationLogRepository;
import com.campuslink.realtime.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Le module Moderation ne modifie JAMAIS directement l'entite User (propriete du Dev B).
 * Toute action (suspension, bannissement...) est deleguee a UserModerationPort, dont
 * l'implementation reelle est fournie par le module Backend Core. Ici, on se contente
 * de tracer l'action dans le journal (moderation_logs) et de declencher l'appel.
 */
@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private final ModerationLogRepository moderationLogRepository;
    private final UserModerationPort userModerationPort;

    @Override
    @Transactional
    public ModerationLog avertir(Long moderateurId, ModerationActionRequest request) {
        // L'avertissement ne change pas le statut du compte : uniquement trace dans le journal.
        return logger(moderateurId, request, ModerationActionType.AVERTISSEMENT);
    }

    @Override
    @Transactional
    public ModerationLog suspendre(Long moderateurId, ModerationActionRequest request) {
        userModerationPort.suspendre(request.getUtilisateurCibleId(), request.getMotif());
        return logger(moderateurId, request, ModerationActionType.SUSPENSION);
    }

    @Override
    @Transactional
    public ModerationLog bannir(Long moderateurId, ModerationActionRequest request) {
        userModerationPort.bannir(request.getUtilisateurCibleId(), request.getMotif());
        return logger(moderateurId, request, ModerationActionType.BANNISSEMENT);
    }

    @Override
    @Transactional
    public ModerationLog debannir(Long moderateurId, ModerationActionRequest request) {
        userModerationPort.debannir(request.getUtilisateurCibleId(), request.getMotif());
        return logger(moderateurId, request, ModerationActionType.DEBANNISSEMENT);
    }

    @Override
    public List<ModerationLog> historiquePourUtilisateur(Long utilisateurCibleId) {
        return moderationLogRepository.findAllByUtilisateurCibleIdOrderByDateActionDesc(utilisateurCibleId);
    }

    @Override
    public List<ModerationLog> journalComplet() {
        return moderationLogRepository.findAllByOrderByDateActionDesc();
    }

    private ModerationLog logger(Long moderateurId, ModerationActionRequest request, ModerationActionType action) {
        ModerationLog log = ModerationLog.builder()
                .utilisateurCibleId(request.getUtilisateurCibleId())
                .moderateurId(moderateurId)
                .action(action)
                .motif(request.getMotif())
                .reportId(request.getReportId())
                .build();
        return moderationLogRepository.save(log);
    }
}
