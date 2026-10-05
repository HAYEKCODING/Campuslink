package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByEmetteurIdAndCibleId(Long emetteurId, Long cibleId);

    boolean existsByEmetteurIdAndCibleId(Long emetteurId, Long cibleId);

    List<Like> findAllByEmetteurId(Long emetteurId);

    List<Like> findAllByCibleId(Long cibleId);

    void deleteByEmetteurIdAndCibleId(Long emetteurId, Long cibleId);
}
