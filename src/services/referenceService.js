import { api } from "../lib/api";
import { UNIVERSITIES, FACULTIES, NEIGHBORHOODS, INTERESTS } from "../lib/referenceData";

/**
 * Référentiels de suggestions alimentés par le backend.
 *
 * Le backend expose GET /reference/{universities,faculties,neighborhoods,interests}
 * (valeurs DISTINCT réellement présentes en base, voir PublicContentController).
 * Ces appels sont publics : aucune authentification requise.
 *
 * Stratégie : un échec réseau / 404 / réponse vide retombe silencieusement sur
 * les listes statiques de `lib/referenceData.js` — l'onboarding et la recherche
 * ne doivent jamais être bloqués par un référentiel indisponible (base vierge,
 * backend en cours de démarrage...).
 *
 * Les réponses sont mises en cache par onglet : les listes ne changent pas
 * pendant une session et chaque écran (onboarding, recherche) les redemande.
 */

const ENDPOINTS = {
  universities: UNIVERSITIES,
  faculties: FACULTIES,
  neighborhoods: NEIGHBORHOODS,
  interests: INTERESTS,
};

/** Cache { type -> Promise<string[]> } — une seule requête réseau par type et par onglet. */
const cache = new Map();

async function fetchReference(type) {
  try {
    const values = await api.get(`/reference/${type}`);
    if (Array.isArray(values) && values.length > 0) return values;
  } catch {
    // Backend indisponible ou route absente : on retombe sur le référentiel local.
  }
  return ENDPOINTS[type];
}

function getReference(type) {
  if (!cache.has(type)) {
    cache.set(type, fetchReference(type));
  }
  return cache.get(type);
}

export function getUniversities() {
  return getReference("universities");
}

export function getFaculties() {
  return getReference("faculties");
}

export function getNeighborhoods() {
  return getReference("neighborhoods");
}

export function getInterests() {
  return getReference("interests");
}

/** Vide le cache (utile après une création de profil en base, en tests). */
export function resetReferenceCache() {
  cache.clear();
}
