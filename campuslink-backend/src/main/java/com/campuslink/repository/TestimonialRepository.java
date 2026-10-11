package com.campuslink.repository;

import com.campuslink.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link Testimonial}.
 */
public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    /**
     * Témoignages visibles sur la landing page : actifs uniquement,
     * triés par ordre d'affichage croissant puis du plus récent au plus ancien.
     */
    List<Testimonial> findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

}
