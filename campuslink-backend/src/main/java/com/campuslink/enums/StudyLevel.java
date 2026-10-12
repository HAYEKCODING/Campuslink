package com.campuslink.enums;

/**
 * Niveau d'étude déclaré par l'utilisateur sur son profil
 * (colonne {@code profiles.level}).
 *
 * <p>Valeurs alignées sur le sélecteur de l'onboarding (étape 1) :
 * Licence, Master, Doctorat. La valeur persistée est le nom de l'enum
 * ({@code @Enumerated(EnumType.STRING)}) ; le libellé français est
 * affiché côté client.</p>
 */
public enum StudyLevel {

    LICENCE,
    MASTER,
    DOCTORAT

}
