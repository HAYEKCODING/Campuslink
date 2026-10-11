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
import com.campuslink.service.PublicContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Implémentation du contenu public de la landing page.
 *
 * <p>Les requêtes sont minimales et déterministes : le compteur est un
 * simple {@code COUNT(*)} sur les profils, les référentiels sont des
 * {@code DISTINCT} sur les colonnes concernées — suffisants pour une
 * landing, sans table de référentiel à synchroniser.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicContentServiceImpl implements PublicContentService {

    /** Nombre d'avatars illustrant le compteur d'inscrits. */
    private static final int AVATAR_SAMPLE_SIZE = 4;

    /** Types de référentiels exposés par {@code GET /reference/{type}}. */
    private static final Set<String> REFERENCE_TYPES =
            Set.of("universities", "faculties", "neighborhoods", "interests");

    private final ProfileRepository profileRepository;
    private final TestimonialRepository testimonialRepository;
    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicStatsResponse getPublicStats() {
        long memberCount = profileRepository.count();
        List<String> avatars = profileRepository.findAvatarUrls(PageRequest.of(0, AVATAR_SAMPLE_SIZE));

        return PublicStatsResponse.builder()
                .memberCount(memberCount)
                .avatarPhotos(avatars)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestimonialResponse> getTestimonials() {
        return testimonialRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(PublicContentServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void submitContact(ContactRequest request) {
        ContactMessage contactMessage = ContactMessage.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .message(request.getMessage().trim())
                .build();

        contactMessageRepository.save(contactMessage);
        log.info("Message de contact recu de {} <{}> (id={})",
                contactMessage.getName(), contactMessage.getEmail(), contactMessage.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findReferenceValues(String referenceType) {
        if (referenceType == null || !REFERENCE_TYPES.contains(referenceType)) {
            throw new BadRequestException(
                    "Référentiel inconnu : '" + referenceType + "'. Types acceptés : "
                            + String.join(", ", REFERENCE_TYPES) + ".");
        }

        return switch (referenceType) {
            case "universities" -> profileRepository.findDistinctUniversities();
            case "faculties" -> profileRepository.findDistinctFieldOfStudies();
            case "neighborhoods" -> profileRepository.findDistinctNeighborhoods();
            case "interests" -> profileRepository.findDistinctInterests();
            // Garanti par REFERENCE_TYPES ci-dessus.
            default -> List.of();
        };
    }

    private static TestimonialResponse toResponse(Testimonial testimonial) {
        return TestimonialResponse.builder()
                .id(testimonial.getId())
                .name(testimonial.getName())
                .role(testimonial.getRole())
                .photo(testimonial.getPhoto())
                .quote(testimonial.getQuote())
                .build();
    }

}
