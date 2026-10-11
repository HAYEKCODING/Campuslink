package com.campuslink.service.impl;

import com.campuslink.dto.request.ContactRequest;
import com.campuslink.dto.response.PublicStatsResponse;
import com.campuslink.dto.response.TestimonialResponse;
import com.campuslink.entity.ContactMessage;
import com.campuslink.entity.Testimonial;
import com.campuslink.exception.BadRequestException;
import com.campuslink.repository.ContactMessageRepository;
import com.campuslink.repository.ProfileRepository;
import com.campuslink.repository.TestimonialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link PublicContentServiceImpl} — le contenu public
 * de la landing (statistiques, témoignages, contact, référentiels).
 */
@ExtendWith(MockitoExtension.class)
class PublicContentServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TestimonialRepository testimonialRepository;

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @InjectMocks
    private PublicContentServiceImpl service;

    // ===================== GET /stats/public =====================

    @Test
    @DisplayName("Les stats retournent le compteur de profils et un échantillon d'avatars")
    void getPublicStats_shouldReturnCountAndAvatarSample() {
        when(profileRepository.count()).thenReturn(42L);
        when(profileRepository.findAvatarUrls(any(Pageable.class)))
                .thenReturn(List.of("https://cdn/a.jpg", "https://cdn/b.jpg"));

        PublicStatsResponse stats = service.getPublicStats();

        assertThat(stats.getMemberCount()).isEqualTo(42L);
        assertThat(stats.getAvatarPhotos()).containsExactly("https://cdn/a.jpg", "https://cdn/b.jpg");

        // L'échantillon d'avatars doit être borné (pagina de taille 4).
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(profileRepository).findAvatarUrls(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(4);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    }

    // ===================== GET /testimonials =====================

    @Test
    @DisplayName("Seuls les témoignages actifs sont mappés, dans l'ordre renvoyé par la base")
    void getTestimonials_shouldMapActiveOnes() {
        Testimonial first = Testimonial.builder()
                .name("Awa").role("Étudiante en médecine")
                .quote("Campuslink m'a aidée à trouver mon groupe de révision.").build();
        Testimonial second = Testimonial.builder()
                .name("Moussa").role("Doctorant").photo("https://cdn/m.jpg")
                .quote("Une communauté vraiment utile.").build();
        when(testimonialRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc())
                .thenReturn(List.of(first, second));

        List<TestimonialResponse> testimonials = service.getTestimonials();

        assertThat(testimonials).hasSize(2);
        assertThat(testimonials.get(0).getName()).isEqualTo("Awa");
        assertThat(testimonials.get(0).getRole()).isEqualTo("Étudiante en médecine");
        assertThat(testimonials.get(0).getPhoto()).isNull();
        assertThat(testimonials.get(1).getPhoto()).isEqualTo("https://cdn/m.jpg");
        assertThat(testimonials.get(1).getQuote()).isEqualTo("Une communauté vraiment utile.");
    }

    // ===================== POST /contact =====================

    @Test
    @DisplayName("Le message de contact est persisté, normalisé (trim + email en minuscules)")
    void submitContact_shouldPersistNormalizedMessage() {
        ContactRequest request = ContactRequest.builder()
                .name("  Koffi  ")
                .email("  KOFFI@Example.COM ")
                .message("  Bonjour, une question.  ")
                .build();

        service.submitContact(request);

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        ContactMessage saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Koffi");
        assertThat(saved.getEmail()).isEqualTo("koffi@example.com");
        assertThat(saved.getMessage()).isEqualTo("Bonjour, une question.");
    }

    // ===================== GET /reference/{type} =====================

    @Test
    @DisplayName("Chaque type de référentiel connu délégué au bon tri DISTINCT du repository")
    void findReferenceValues_shouldDelegatePerType() {
        when(profileRepository.findDistinctUniversities()).thenReturn(List.of("UFHB"));
        when(profileRepository.findDistinctFieldOfStudies()).thenReturn(List.of("Droit"));
        when(profileRepository.findDistinctNeighborhoods()).thenReturn(List.of("Cocody"));
        when(profileRepository.findDistinctInterests()).thenReturn(List.of("Football"));

        assertThat(service.findReferenceValues("universities")).containsExactly("UFHB");
        assertThat(service.findReferenceValues("faculties")).containsExactly("Droit");
        assertThat(service.findReferenceValues("neighborhoods")).containsExactly("Cocody");
        assertThat(service.findReferenceValues("interests")).containsExactly("Football");
    }

    @Test
    @DisplayName("Un type inconnu lève BadRequestException sans toucher au repository")
    void findReferenceValues_shouldRejectUnknownType() {
        assertThatThrownBy(() -> service.findReferenceValues("couleurs"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("couleurs");

        verify(profileRepository, never()).findDistinctUniversities();
        verify(profileRepository, never()).findDistinctInterests();
    }

    @Test
    @DisplayName("Un type null est rejeté (pas de NPE)")
    void findReferenceValues_shouldRejectNullType() {
        assertThatThrownBy(() -> service.findReferenceValues(null))
                .isInstanceOf(BadRequestException.class);
    }

    // Pas de write dans les lectures : le compteur et les référentiels
    // ne doivent jamais persister (MockitoExtension strict signifierait
    // un save() inattendu comme un échec de test).
    @Test
    @DisplayName("getPublicStats ne persiste rien (lecture seule)")
    void getPublicStats_isReadOnly() {
        when(profileRepository.count()).thenReturn(0L);
        when(profileRepository.findAvatarUrls(any(Pageable.class))).thenReturn(List.of());

        PublicStatsResponse stats = service.getPublicStats();

        assertThat(stats.getMemberCount()).isZero();
        assertThat(stats.getAvatarPhotos()).isEmpty();
        verify(contactMessageRepository, never()).save(any(ContactMessage.class));
    }

}
