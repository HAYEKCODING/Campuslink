package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.DashboardStatsResponse;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.entity.enums.ModerationActionType;
import com.campuslink.realtime.entity.enums.ReportStatus;
import com.campuslink.realtime.repository.MatchRepository;
import com.campuslink.realtime.repository.MessageRepository;
import com.campuslink.realtime.repository.ModerationLogRepository;
import com.campuslink.realtime.repository.ReportRepository;
import com.campuslink.realtime.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final MatchRepository matchRepository;
    private final MessageRepository messageRepository;
    private final ReportRepository reportRepository;
    private final ModerationLogRepository moderationLogRepository;

    @Override
    public DashboardStatsResponse getStatistiques() {
        long matchsActifs = matchRepository.countByStatut(MatchStatus.ACTIF);

        long signalementsOuverts = reportRepository.findAllByStatutOrderByDateCreationDesc(ReportStatus.EN_ATTENTE).size()
                + reportRepository.findAllByStatutOrderByDateCreationDesc(ReportStatus.EN_COURS).size();

        long utilisateursSuspendusOuBannis = moderationLogRepository.findAllByOrderByDateActionDesc().stream()
                .filter(l -> l.getAction() == ModerationActionType.SUSPENSION || l.getAction() == ModerationActionType.BANNISSEMENT)
                .map(l -> l.getUtilisateurCibleId())
                .distinct()
                .count();

        return DashboardStatsResponse.builder()
                .nombreMatchsActifs(matchsActifs)
                .nombreMessagesEnvoyes(messageRepository.count())
                .nombreSignalementsOuverts(signalementsOuverts)
                .nombreUtilisateursSuspendusOuBannis(utilisateursSuspendusOuBannis)
                .build();
    }
}
