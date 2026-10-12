import { api, resolveMediaUrl } from "../lib/api";

/**
 * Service recherche — GET /profiles/search (backend réel).
 *
 * Paramètres réellement supportés par le backend :
 *   minAge, maxAge, gender, university, fieldOfStudy, neighborhood, city, interests[]
 * (recherche partielle insensible à la casse pour les chaînes, égalité stricte
 * pour le genre).
 *
 * `interests` est répété (`?interests=Football&interests=Musique`) et signifie
 * "au moins un de ces centres d'intérêt". `gender` est l'enum Gender
 * (MALE/FEMALE/OTHER/PREFER_NOT_TO_SAY) ; vide/absent = tous les genres.
 */
export async function searchProfiles(filters = {}) {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    if (Array.isArray(value)) {
      value.forEach((v) => {
        if (v !== undefined && v !== null && v !== "") params.append(key, v);
      });
    } else {
      params.append(key, value);
    }
  });

  const query = params.toString();
  const page = await api.get(`/profiles/search${query ? `?${query}` : ""}`);
  return (page?.content ?? []).map((profile) => ({
    ...profile,
    name: [profile.firstName, profile.lastName].filter(Boolean).join(" "),
    photo: resolveMediaUrl(profile.avatarUrl) || null,
    faculty: profile.fieldOfStudy ?? null,
  }));
}
