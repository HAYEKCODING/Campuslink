package com.campuslink.service.impl;

import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import com.campuslink.exception.BadRequestException;
import com.campuslink.mapper.ProfileMapper;
import com.campuslink.repository.ProfileRepository;
import com.campuslink.service.ProfileSearchService;
import com.campuslink.specification.ProfileSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation du moteur de recherche de profils.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileSearchServiceImpl implements ProfileSearchService {

    private static final String AGE_SORT_PROPERTY = "age";
    private static final String DATE_OF_BIRTH_PROPERTY = "dateOfBirth";

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> search(ProfileSearchCriteria criteria, Pageable pageable) {
        validateAgeRange(criteria);

        Specification<Profile> specification = ProfileSpecification.withCriteria(criteria);
        Pageable resolvedPageable = translateAgeSort(pageable);

        Page<Profile> profiles = profileRepository.findAll(specification, resolvedPageable);

        log.debug("Recherche de profils : {} résultat(s) sur {} page(s)",
                profiles.getNumberOfElements(), profiles.getTotalPages());

        return profiles.map(profileMapper::toProfileResponse);
    }

    private void validateAgeRange(ProfileSearchCriteria criteria) {
        if (criteria.getMinAge() != null && criteria.getMaxAge() != null
                && criteria.getMinAge() > criteria.getMaxAge()) {
            throw new BadRequestException("L'âge minimum ne peut pas être supérieur à l'âge maximum.");
        }
    }

    /**
     * Traduit un tri demandé sur "âge" en tri sur {@code dateOfBirth}, seul
     * champ réellement persisté. La direction est inversée : un âge croissant
     * correspond à une date de naissance décroissante (plus récente), et
     * inversement — voir {@link com.campuslink.specification.ProfileSpecification}
     * pour la même logique appliquée au filtrage.
     */
    private Pageable translateAgeSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        boolean hasAgeSort = pageable.getSort().stream()
                .anyMatch(order -> AGE_SORT_PROPERTY.equalsIgnoreCase(order.getProperty()));

        if (!hasAgeSort) {
            return pageable;
        }

        List<Sort.Order> translatedOrders = pageable.getSort().stream()
                .map(order -> AGE_SORT_PROPERTY.equalsIgnoreCase(order.getProperty())
                        ? new Sort.Order(reverse(order.getDirection()), DATE_OF_BIRTH_PROPERTY)
                        : order)
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translatedOrders));
    }

    private Sort.Direction reverse(Sort.Direction direction) {
        return direction.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

}
