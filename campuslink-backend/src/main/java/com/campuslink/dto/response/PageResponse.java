package com.campuslink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination exposée par l'API — volontairement distincte de
 * {@link org.springframework.data.domain.Page} de Spring Data.
 *
 * <p>Sérialiser directement un {@code Page<T>} Spring Data expose des détails
 * internes (structure imbriquée {@code pageable}/{@code sort}) qui ne
 * concernent pas le client de l'API et dont la forme JSON exacte a varié
 * selon les versions de Spring Data. Cette classe fixe un contrat stable.</p>
 *
 * @param <T> type des éléments de la page
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

}
