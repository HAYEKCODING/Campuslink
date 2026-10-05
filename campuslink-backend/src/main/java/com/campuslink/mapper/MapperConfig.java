package com.campuslink.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Configuration MapStruct centrale, à référencer par tous les mappers du projet
 * via {@code @Mapper(config = MapperConfig.class)}.
 *
 * <p>Garantit une stratégie de mapping homogène sur l'ensemble de l'application :</p>
 * <ul>
 *     <li>Génération des mappers en tant que composants Spring ({@code componentModel = "spring"})</li>
 *     <li>Injection par constructeur</li>
 *     <li>Les valeurs {@code null} ne modifient pas les propriétés cibles lors des mises à jour</li>
 *     <li>Signalement des propriétés non mappées (erreurs de mapping silencieuses évitées)</li>
 * </ul>
 */
@org.mapstruct.MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface MapperConfig {

}
