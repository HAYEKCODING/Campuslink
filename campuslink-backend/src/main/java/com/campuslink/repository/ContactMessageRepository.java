package com.campuslink.repository;

import com.campuslink.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link ContactMessage} (file d'attente des
 * messages du formulaire de contact public).
 */
public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
}
