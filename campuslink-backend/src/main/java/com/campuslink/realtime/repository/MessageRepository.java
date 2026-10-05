package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.Message;
import com.campuslink.realtime.entity.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByMatchIdOrderByDateEnvoiDesc(Long matchId, Pageable pageable);

    List<Message> findAllByMatchIdOrderByDateEnvoiAsc(Long matchId);

    long countByMatchIdAndExpediteurIdNotAndStatutLectureNot(Long matchId, Long expediteurId, MessageStatus statut);

    /**
     * Marque comme lus tous les messages d'un match reçus par {@code lecteurId}
     * (c'est-à-dire envoyés par l'autre membre du match).
     *
     * <p>La date de lecture est fournie comme paramètre ({@code :now}, de type
     * {@link Instant}) plutôt que par {@code CURRENT_TIMESTAMP} : Hibernate 6
     * typerait cette expression en {@code java.sql.Timestamp}, incompatible avec
     * le champ {@code Instant} de l'entité — l'erreur empêchait la création du
     * bean et donc le démarrage complet de l'application.</p>
     */
    @Modifying
    @Query("update Message m set m.statutLecture = com.campuslink.realtime.entity.enums.MessageStatus.LU, " +
           "m.dateLecture = :now " +
           "where m.matchId = :matchId and m.expediteurId <> :lecteurId and m.statutLecture <> com.campuslink.realtime.entity.enums.MessageStatus.LU")
    int marquerCommeLu(@Param("matchId") Long matchId,
                       @Param("lecteurId") Long lecteurId,
                       @Param("now") Instant now);
}
