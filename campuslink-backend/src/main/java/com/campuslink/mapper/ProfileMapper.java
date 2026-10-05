package com.campuslink.mapper;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.Period;

/**
 * Mapper MapStruct entre {@link Profile} et ses représentations exposées par l'API.
 *
 * <p>{@link #updateEntityFromRequest} modifie une instance existante plutôt que
 * d'en construire une nouvelle : {@link Profile} n'a volontairement pas de
 * constructeur public (voir {@code BaseEntity}), donc la création d'un profil
 * se fait via {@code Profile.builder()} directement dans {@code ProfileServiceImpl},
 * pas ici.</p>
 */
@Mapper(config = MapperConfig.class)
public interface ProfileMapper {

    @Mapping(target = "age", source = "dateOfBirth", qualifiedByName = "dateOfBirthToAge")
    @Mapping(target = "legacyId", source = "user.legacyId")
    ProfileResponse toProfileResponse(Profile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(ProfileRequest request, @MappingTarget Profile profile);

    /**
     * Calcule l'âge à partir de la date de naissance. {@code null} si la date
     * de naissance n'est pas renseignée — l'âge est alors simplement absent
     * de la réponse (voir {@code @JsonInclude(NON_NULL)} sur {@link ProfileResponse}).
     */
    @Named("dateOfBirthToAge")
    default Integer dateOfBirthToAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

}
