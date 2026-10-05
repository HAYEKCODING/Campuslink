package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.request.ReportRequest;
import com.campuslink.realtime.dto.response.ReportResponse;
import com.campuslink.realtime.entity.Report;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.realtime.entity.enums.ReportStatus;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.repository.ReportRepository;
import com.campuslink.realtime.service.NotificationService;
import com.campuslink.realtime.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReportResponse creer(Long emetteurId, ReportRequest request) {
        if (emetteurId.equals(request.getCibleId())) {
            throw new DuplicateResourceException("Impossible de se signaler soi-meme");
        }

        Report report = Report.builder()
                .emetteurId(emetteurId)
                .cibleId(request.getCibleId())
                .matchId(request.getMatchId())
                .motif(request.getMotif())
                .description(request.getDescription())
                .statut(ReportStatus.EN_ATTENTE)
                .build();
        report = reportRepository.save(report);

        // Notifie les administrateurs/moderateurs pourrait passer par un canal /topic/admin/reports
        // (a brancher cote frontend admin ; ici on notifie via le meme mecanisme que les autres evenements)
        notificationService.creerEtEnvoyer(
                request.getCibleId(), NotificationType.SIGNALEMENT,
                "Un signalement vous concernant a ete enregistre", report.getId());

        return toResponse(report);
    }

    @Override
    public List<ReportResponse> lister(ReportStatus statutOuNull) {
        List<Report> reports = statutOuNull != null
                ? reportRepository.findAllByStatutOrderByDateCreationDesc(statutOuNull)
                : reportRepository.findAll();
        return reports.stream().map(this::toResponse).toList();
    }

    @Override
    public List<ReportResponse> listerParCible(Long cibleId) {
        return reportRepository.findAllByCibleIdOrderByDateCreationDesc(cibleId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReportResponse mettreAJourStatut(Long reportId, ReportStatus nouveauStatut) {
        Report report = getOuException(reportId);
        report.setStatut(nouveauStatut);
        report.setDateTraitement(Instant.now());
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public void archiver(Long reportId) {
        Report report = getOuException(reportId);
        report.setStatut(ReportStatus.ARCHIVE);
        report.setDateTraitement(Instant.now());
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public void supprimer(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Signalement introuvable : " + reportId);
        }
        reportRepository.deleteById(reportId);
    }

    private Report getOuException(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable : " + reportId));
    }

    private ReportResponse toResponse(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .emetteurId(r.getEmetteurId())
                .cibleId(r.getCibleId())
                .matchId(r.getMatchId())
                .motif(r.getMotif())
                .description(r.getDescription())
                .statut(r.getStatut())
                .dateCreation(r.getDateCreation())
                .dateTraitement(r.getDateTraitement())
                .build();
    }
}
