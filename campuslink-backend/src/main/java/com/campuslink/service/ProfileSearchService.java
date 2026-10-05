package com.campuslink.service;

import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.dto.response.ProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrat métier du moteur de recherche de profils.
 */
public interface ProfileSearchService {

    /**
     * Recherche des profils selon des critères dynamiques, avec pagination et tri.
     *
     * @throws com.campuslink.exception.BadRequestException si {@code minAge > maxAge}
     */
    Page<ProfileResponse> search(ProfileSearchCriteria criteria, Pageable pageable);

}
