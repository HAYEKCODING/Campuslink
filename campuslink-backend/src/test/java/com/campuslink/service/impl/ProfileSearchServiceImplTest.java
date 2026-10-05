package com.campuslink.service.impl;

import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import com.campuslink.exception.BadRequestException;
import com.campuslink.mapper.ProfileMapper;
import com.campuslink.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link ProfileSearchServiceImpl}, isolés de la base via
 * Mockito. Complémentaires à {@code ProfileSpecificationIntegrationTest} qui
 * vérifie le comportement réel des prédicats Criteria API contre H2 — ici on
 * teste uniquement la logique propre au service : validation des bornes d'âge
 * et traduction du tri sur "age".
 */
@ExtendWith(MockitoExtension.class)
class ProfileSearchServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileMapper profileMapper;

    private ProfileSearchServiceImpl profileSearchService;

    @BeforeEach
    void setUp() {
        profileSearchService = new ProfileSearchServiceImpl(profileRepository, profileMapper);
    }

    @Test
    void shouldThrowBadRequest_whenMinAgeGreaterThanMaxAge() {
        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().minAge(30).maxAge(20).build();

        assertThatThrownBy(() -> profileSearchService.search(criteria, Pageable.unpaged()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldReturnMappedPage_whenSearchSucceeds() {
        Profile profile = Profile.builder().firstName("Awa").lastName("Kone").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Profile> profilePage = new PageImpl<>(List.of(profile), pageable, 1);

        when(profileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(profilePage);
        when(profileMapper.toProfileResponse(profile)).thenReturn(ProfileResponse.builder().firstName("Awa").build());

        Page<ProfileResponse> result = profileSearchService.search(ProfileSearchCriteria.builder().build(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(ProfileResponse::getFirstName).containsExactly("Awa");
    }

    @Test
    void shouldTranslateAgeSortAscending_toDateOfBirthDescending() {
        Page<Profile> emptyPage = new PageImpl<>(List.of());
        when(profileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Pageable requestedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "age"));
        profileSearchService.search(ProfileSearchCriteria.builder().build(), requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(profileRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Sort.Order translatedOrder = pageableCaptor.getValue().getSort().getOrderFor("dateOfBirth");
        assertThat(translatedOrder).isNotNull();
        assertThat(translatedOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldTranslateAgeSortDescending_toDateOfBirthAscending() {
        Page<Profile> emptyPage = new PageImpl<>(List.of());
        when(profileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Pageable requestedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "age"));
        profileSearchService.search(ProfileSearchCriteria.builder().build(), requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(profileRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Sort.Order translatedOrder = pageableCaptor.getValue().getSort().getOrderFor("dateOfBirth");
        assertThat(translatedOrder).isNotNull();
        assertThat(translatedOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldLeaveNonAgeSort_unchanged() {
        Page<Profile> emptyPage = new PageImpl<>(List.of());
        when(profileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Pageable requestedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "city"));
        profileSearchService.search(ProfileSearchCriteria.builder().build(), requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(profileRepository).findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("city")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("dateOfBirth")).isNull();
    }

}
