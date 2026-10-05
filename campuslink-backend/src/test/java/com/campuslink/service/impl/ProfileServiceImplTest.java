package com.campuslink.service.impl;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import com.campuslink.entity.User;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.mapper.ProfileMapper;
import com.campuslink.repository.ProfileRepository;
import com.campuslink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link ProfileServiceImpl}, isolés de Spring et de la
 * base de données grâce à Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileMapper profileMapper;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(profileRepository, userRepository, profileMapper);
    }

    // ===================== createProfile =====================

    @Nested
    class CreateProfile {

        @Test
        void shouldCreateProfile_whenUserHasNone() {
            User user = User.builder().email("etudiant@campuslink.io").password("hashed").build();
            ProfileRequest request = ProfileRequest.builder()
                    .firstName("Awa")
                    .lastName("Kone")
                    .city("Bouaké")
                    .interests(Set.of("Musique", "Football"))
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(profileMapper.toProfileResponse(any(Profile.class)))
                    .thenReturn(ProfileResponse.builder().firstName("Awa").build());

            ProfileResponse response = profileService.createProfile(USER_ID, request);

            assertThat(response.getFirstName()).isEqualTo("Awa");
            assertThat(user.getProfile()).isNotNull();
            assertThat(user.getProfile().getFirstName()).isEqualTo("Awa");
            assertThat(user.getProfile().getUser()).isEqualTo(user);
            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowDuplicateResource_whenProfileAlreadyExists() {
            User user = User.builder().email("etudiant@campuslink.io").password("hashed").build();
            user.setProfile(Profile.builder().firstName("Existant").lastName("Profil").build());

            ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileService.createProfile(USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowResourceNotFound_whenUserUnknown() {
            ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.createProfile(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ===================== updateProfile =====================

    @Nested
    class UpdateProfile {

        @Test
        void shouldUpdateProfile_whenProfileExists() {
            Profile existingProfile = Profile.builder().firstName("Ancien").lastName("Nom").build();
            ProfileRequest request = ProfileRequest.builder()
                    .firstName("Nouveau")
                    .lastName("Nom")
                    .interests(Set.of("Lecture"))
                    .build();

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
            when(profileRepository.save(existingProfile)).thenReturn(existingProfile);
            when(profileMapper.toProfileResponse(existingProfile))
                    .thenReturn(ProfileResponse.builder().firstName("Nouveau").build());

            ProfileResponse response = profileService.updateProfile(USER_ID, request);

            assertThat(response.getFirstName()).isEqualTo("Nouveau");
            assertThat(existingProfile.getInterests()).containsExactly("Lecture");
            verify(profileMapper).updateEntityFromRequest(request, existingProfile);
            verify(profileRepository).save(existingProfile);
        }

        @Test
        void shouldThrowResourceNotFound_whenNoProfileExists() {
            ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(profileRepository, never()).save(any());
        }
    }

    // ===================== deleteProfile =====================

    @Nested
    class DeleteProfile {

        @Test
        void shouldDetachProfile_whenProfileExists() {
            User user = User.builder().email("etudiant@campuslink.io").password("hashed").build();
            user.setProfile(Profile.builder().firstName("Awa").lastName("Kone").build());

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            profileService.deleteProfile(USER_ID);

            assertThat(user.getProfile())
                    .as("orphanRemoval supprimera physiquement la ligne au flush")
                    .isNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getProfile()).isNull();
        }

        @Test
        void shouldThrowResourceNotFound_whenNoProfileExists() {
            User user = User.builder().email("etudiant@campuslink.io").password("hashed").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileService.deleteProfile(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ===================== getOwnProfile / getPublicProfile =====================

    @Nested
    class GetProfile {

        @Test
        void getOwnProfile_shouldReturnMappedResponse() {
            Profile profile = Profile.builder().firstName("Awa").lastName("Kone").build();

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileMapper.toProfileResponse(profile))
                    .thenReturn(ProfileResponse.builder().firstName("Awa").build());

            ProfileResponse response = profileService.getOwnProfile(USER_ID);

            assertThat(response.getFirstName()).isEqualTo("Awa");
        }

        @Test
        void getOwnProfile_shouldThrowResourceNotFound_whenNoProfile() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getOwnProfile(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getPublicProfile_shouldReturnMappedResponse() {
            UUID profileId = UUID.randomUUID();
            Profile profile = Profile.builder().firstName("Moussa").lastName("Traoré").build();

            when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profileMapper.toProfileResponse(profile))
                    .thenReturn(ProfileResponse.builder().firstName("Moussa").build());

            ProfileResponse response = profileService.getPublicProfile(profileId);

            assertThat(response.getFirstName()).isEqualTo("Moussa");
        }

        @Test
        void getPublicProfile_shouldThrowResourceNotFound_whenProfileUnknown() {
            UUID profileId = UUID.randomUUID();
            when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getPublicProfile(profileId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

}
