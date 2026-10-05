package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("select m from Match m where (m.utilisateur1Id = :a and m.utilisateur2Id = :b) " +
           "or (m.utilisateur1Id = :b and m.utilisateur2Id = :a)")
    Optional<Match> findEntreUtilisateurs(@Param("a") Long a, @Param("b") Long b);

    @Query("select m from Match m where (m.utilisateur1Id = :userId or m.utilisateur2Id = :userId) " +
           "and m.statut = :statut order by m.dateMatch desc")
    List<Match> findAllByUtilisateurEtStatut(@Param("userId") Long userId, @Param("statut") MatchStatus statut);

    @Query("select m from Match m where m.utilisateur1Id = :userId or m.utilisateur2Id = :userId " +
           "order by m.dateMatch desc")
    List<Match> findHistoriqueByUtilisateur(@Param("userId") Long userId);

    long countByStatut(MatchStatus statut);
}
