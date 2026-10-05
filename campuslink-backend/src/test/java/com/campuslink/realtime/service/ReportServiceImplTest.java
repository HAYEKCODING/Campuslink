package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.request.ReportRequest;
import com.campuslink.realtime.dto.response.ReportResponse;
import com.campuslink.realtime.entity.Report;
import com.campuslink.realtime.entity.enums.ReportStatus;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.repository.ReportRepository;
import com.campuslink.realtime.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void creer_enregistreEtNotifie() {
        ReportRequest request = new ReportRequest();
        request.setCibleId(2L);
        request.setMotif("Comportement inapproprie");
        request.setDescription("Details...");

        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(77L);
            return r;
        });

        ReportResponse response = reportService.creer(1L, request);

        assertThat(response.getStatut()).isEqualTo(ReportStatus.EN_ATTENTE);
        assertThat(response.getEmetteurId()).isEqualTo(1L);
        verify(notificationService).creerEtEnvoyer(eq(2L), any(), anyString(), eq(77L));
    }

    @Test
    void creer_refuseAutoSignalement() {
        ReportRequest request = new ReportRequest();
        request.setCibleId(1L);
        request.setMotif("Test");

        assertThrows(DuplicateResourceException.class, () -> reportService.creer(1L, request));
        verifyNoInteractions(reportRepository);
    }

    @Test
    void mettreAJourStatut_leveExceptionSiIntrouvable() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> reportService.mettreAJourStatut(1L, ReportStatus.RESOLU));
    }

    @Test
    void archiver_changeLeStatut() {
        Report report = Report.builder().id(1L).statut(ReportStatus.EN_COURS).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        reportService.archiver(1L);

        assertThat(report.getStatut()).isEqualTo(ReportStatus.ARCHIVE);
        assertThat(report.getDateTraitement()).isNotNull();
    }
}
