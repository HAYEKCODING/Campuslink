package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.request.ReportRequest;
import com.campuslink.realtime.dto.response.ReportResponse;
import com.campuslink.realtime.entity.enums.ReportStatus;

import java.util.List;

public interface ReportService {

    ReportResponse creer(Long emetteurId, ReportRequest request);

    List<ReportResponse> lister(ReportStatus statutOuNull);

    List<ReportResponse> listerParCible(Long cibleId);

    ReportResponse mettreAJourStatut(Long reportId, ReportStatus nouveauStatut);

    void archiver(Long reportId);

    void supprimer(Long reportId);
}
