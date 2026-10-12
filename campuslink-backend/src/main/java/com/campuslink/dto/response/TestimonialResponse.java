package com.campuslink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Témoignage public de la landing page — {@code GET /testimonials}.
 *
 * <p>Forme exacte attendue par {@code src/CampusLinkLanding.jsx}
 * (section témoignages) : {@code { id, quote, name, role, photo }}.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestimonialResponse {

    private UUID id;
    private String name;
    private String role;
    private String photo;
    private String quote;

}
