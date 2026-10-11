/**
 * Listes de démarrage (référentiel) utilisées par l'onboarding et la recherche.
 *
 * Le backend expose désormais GET /reference/* (valeurs DISTINCT de la base,
 * voir services/referenceService.js) : ces listes statiques ne servent plus
 * que de *fallback* quand l'endpoint est indisponible (base vierge, backend
 * en démarrage) — il ne faut donc jamais les supprimer. Ce sont des
 * *suggestions* : tous les champs concernés restent modifiables librement
 * (champ éditable + liste de suggestions), car le backend stocke de simples
 * chaînes de caractères (université, filière, quartier).
 */

export const UNIVERSITIES = [
  "Université Norbert Zongo",
  "Université de Bouaké",
  "Institut Supérieur de Technologie de Bouaké",
  "École Normale Supérieure de Bouaké",
];

export const FACULTIES = [
  "Sciences exactes et informatique",
  "Sciences de la vie et de la terre",
  "Lettres et sciences humaines",
  "Droit et économie",
  "Médecine et pharmacie",
  "Techniques de gestion",
];

export const NEIGHBORHOODS = [
  "Air France",
  "Accart-Ville",
  "Kanorosso",
  "Bouaké 2000",
  "Serpent",
  "Dar Es Salam",
];

export const INTERESTS = [
  "Musique",
  "Football",
  "Lecture",
  "Cinéma",
  "Voyage",
  "Informatique",
  "Cuisine",
  "Danse",
  "Photographie",
  "Études",
];

/** Genres : `value` est la valeur envoyée à l'API (enum Gender), `label` celle affichée. */
export const GENDERS = [
  { value: "MALE", label: "Homme" },
  { value: "FEMALE", label: "Femme" },
  { value: "OTHER", label: "Autre" },
];

/**
 * Niveaux d'étude : `value` est la valeur envoyée à l'API (enum StudyLevel),
 * `label` celle affichée (champ level du profil, onboarding étape 1).
 */
export const LEVELS = [
  { value: "LICENCE", label: "Licence" },
  { value: "MASTER", label: "Master" },
  { value: "DOCTORAT", label: "Doctorat" },
];
