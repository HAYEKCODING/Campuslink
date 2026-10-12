package com.campuslink.service.impl;

import com.campuslink.dto.request.UserPreferencesRequest;
import com.campuslink.entity.Profile;
import com.campuslink.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Test
    void update_persistsOnlyProvidedPreferences() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .firstName("Awa")
                .lastName("Kone")
                .profileVisible(true)
                .notificationLikes(true)
                .build();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        UserPreferencesRequest request = new UserPreferencesRequest();
        request.setProfileVisible(false);
        request.setNotificationLikes(false);

        var response = new UserPreferencesServiceImpl(profileRepository).update(userId, request);

        assertThat(response.isProfileVisible()).isFalse();
        assertThat(response.isNotificationLikes()).isFalse();
        assertThat(response.isNotificationMatches()).isTrue();
        assertThat(response.isNotificationMessages()).isTrue();
        verify(profileRepository).save(profile);
    }
}
