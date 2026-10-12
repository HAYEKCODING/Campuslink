package com.campuslink.service.impl;

import com.campuslink.dto.request.UserPreferencesRequest;
import com.campuslink.dto.response.UserPreferencesResponse;
import com.campuslink.entity.Profile;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.repository.ProfileRepository;
import com.campuslink.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final ProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse get(UUID userId) {
        return toResponse(findProfile(userId));
    }

    @Override
    @Transactional
    public UserPreferencesResponse update(UUID userId, UserPreferencesRequest request) {
        Profile profile = findProfile(userId);
        if (request.getProfileVisible() != null) {
            profile.setProfileVisible(request.getProfileVisible());
        }
        if (request.getNotificationLikes() != null) {
            profile.setNotificationLikes(request.getNotificationLikes());
        }
        if (request.getNotificationMatches() != null) {
            profile.setNotificationMatches(request.getNotificationMatches());
        }
        if (request.getNotificationMessages() != null) {
            profile.setNotificationMessages(request.getNotificationMessages());
        }
        if (request.getNotificationReports() != null) {
            profile.setNotificationReports(request.getNotificationReports());
        }
        return toResponse(profileRepository.save(profile));
    }

    private Profile findProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", "utilisateur", userId));
    }

    private UserPreferencesResponse toResponse(Profile profile) {
        return UserPreferencesResponse.builder()
                .profileVisible(profile.isProfileVisible())
                .notificationLikes(profile.isNotificationLikes())
                .notificationMatches(profile.isNotificationMatches())
                .notificationMessages(profile.isNotificationMessages())
                .notificationReports(profile.isNotificationReports())
                .build();
    }
}
