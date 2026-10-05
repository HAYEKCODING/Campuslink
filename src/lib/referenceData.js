/**
 * Listes de démarrage (référentiel) utilisées par l'onboarding et la recherche.
 *
 * Le backend ne expose pas encore d'endpoint `/reference/*` : ces listes vivent
 * donc côté client, dans ce fichier de configuration, pour ne jamais bloquer
 * l'inscription. Ce sont des *suggestions* : tous les champs concernés restent
 * modifiables librement (champ éditable + liste de suggestions), car le backend
 * stocke de simples chaînes de caractères (université, filière, quartier).
 *
 * TODO(backend): exposer GET /reference/universities, /reference/faculties,
 * /reference/neighborhoods et /reference/interests alimentés par la base, puis
 * remplacer ces listes statiques par un appel réseau.
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
