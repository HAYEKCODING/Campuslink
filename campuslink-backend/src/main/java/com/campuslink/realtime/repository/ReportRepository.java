package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.Report;
import com.campuslink.realtime.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByStatutOrderByDateCreationDesc(ReportStatus statut);

    List<Report> findAllByCibleIdOrderByDateCreationDesc(Long cibleId);

    long countByCibleIdAndStatutNot(Long cibleId, ReportStatus statut);
}
