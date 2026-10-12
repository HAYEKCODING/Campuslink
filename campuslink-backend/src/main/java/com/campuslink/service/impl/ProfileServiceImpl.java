package com.campuslink.service.impl;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import com.campuslink.entity.User;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.BadRequestException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.enums.Gender;
import com.campuslink.mapper.ProfileMapper;
import com.campuslink.repository.ProfileRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

/**
 * Implémentation du module Profile.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public ProfileResponse createProfile(UUID userId, ProfileRequest request) {
        validateNewGender(request.getGender());
        User user = getUserOrThrow(userId);

        if (user.getProfile() != null) {
            throw new DuplicateResourceException("Un profil existe déjà pour cet utilisateur.");
        }

        Profile profile = Profile.builder()
                .avatarUrl(request.getAvatarUrl())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .gender(request.getGender())
                .level(request.getLevel())
                .dateOfBirth(request.getDateOfBirth())
                .university(request.getUniversity())
                .fieldOfStudy(request.getFieldOfStudy())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .bio(request.getBio())
                .interests(request.getInterests() != null ? new HashSet<>(request.getInterests()) : new HashSet<>())
                .build();

        // Maintient la cohérence bidirectionnelle (helper déjà présent sur User) ;
        // cascade=ALL sur User.profile persiste le Profile en même temps que le save.
        user.setProfile(profile);
        userRepository.save(user);

        log.info("Profil créé pour l'utilisateur {}", userId);
        return profileMapper.toProfileResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        Profile profile = getProfileOrThrow(userId);
        Gender requestedGender = request.getGender();
        if (requestedGender != null
                && requestedGender != Gender.MALE
                && requestedGender != Gender.FEMALE
                && requestedGender != profile.getGender()) {
            throw new BadRequestException("Le genre doit être Homme ou Femme.");
        }

        profileMapper.updateEntityFromRequest(request, profile);
        // Une collection omise signifie « inchangée » pour les mises à jour
        // partielles envoyées par l'onboarding et les paramètres.
        if (request.getInterests() != null) {
            profile.setInterests(new HashSet<>(request.getInterests()));
        }

        Profile savedProfile = profileRepository.save(profile);

        log.info("Profil mis à jour pour l'utilisateur {}", userId);
        return profileMapper.toProfileResponse(savedProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        User user = getUserOrThrow(userId);

        if (user.getProfile() == null) {
            throw new ResourceNotFoundException("Profil", "utilisateur", userId);
        }

        // orphanRemoval=true sur User.profile supprime physiquement la ligne
        // Profile dès que la référence est rompue et que user est sauvegardé.
        user.setProfile(null);
        userRepository.save(user);

        log.info("Profil supprimé pour l'utilisateur {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getOwnProfile(UUID userId) {
        Profile profile = getProfileOrThrow(userId);
        return profileMapper.toProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", "id", profileId));
        if (!profile.isProfileVisible()) {
            throw new ResourceNotFoundException("Profil", "id", profileId);
        }
        return profileMapper.toPublicProfileResponse(profile);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));
    }

    private Profile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", "utilisateur", userId));
    }

    private void validateNewGender(Gender gender) {
        if (gender != null && gender != Gender.MALE && gender != Gender.FEMALE) {
            throw new BadRequestException("Le genre doit être Homme ou Femme.");
        }
    }
}
