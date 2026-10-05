package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.request.ModerationActionRequest;
import com.campuslink.realtime.entity.ModerationLog;

import java.util.List;

public interface ModerationService {

    ModerationLog avertir(Long moderateurId, ModerationActionRequest request);

    ModerationLog suspendre(Long moderateurId, ModerationActionRequest request);

    ModerationLog bannir(Long moderateurId, ModerationActionRequest request);

    ModerationLog debannir(Long moderateurId, ModerationActionRequest request);

    List<ModerationLog> historiquePourUtilisateur(Long utilisateurCibleId);

    List<ModerationLog> journalComplet();
}
