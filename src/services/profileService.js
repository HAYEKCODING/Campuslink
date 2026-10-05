import { api } from "../lib/api";
import { FACULTIES, INTERESTS, NEIGHBORHOODS, UNIVERSITIES } from "../lib/referenceData";

/**
 * Service profils — aligné sur le backend réel (module Profile).
 *
 * Forme brute renvoyée par l'API (ProfileResponse) :
 *   { id, legacyId, avatarUrl, firstName, lastName, age, gender, university,
 *     fieldOfStudy, neighborhood, city, bio, interests[] }
 *
 * `normalizeProfile()` ajoute les champs "confort" utilisés par les écrans
 * (name, photo, faculty, about) pour éviter de dupliquer le mapping dans
 * chaque composant.
 *
 * Identifiants : `id` est l'UUID du profil (liens /app/profil/:id, consultation
 * publique) ; `legacyId` est l'identifiant numérique du module temps réel
 * (likes, matchs, messages). Ne jamais les confondre.
 */

/** Normalise une réponse ProfileResponse vers la forme consommée par l'interface. */
export function normalizeProfile(raw) {
  if (!raw) return raw;
  const name = [raw.firstName, raw.lastName].filter(Boolean).join(" ");
  return {
    ...raw,
    name: name || raw.name || null,
    photo: raw.avatarUrl || raw.photo || null,
    faculty: raw.fieldOfStudy ?? null,
    about: raw.bio ?? null,
    interests: Array.isArray(raw.interests) ? raw.interests : [],
  };
}

async function getPageContent(path) {
  const page = await api.get(path);
  return (page?.content ?? []).map(normalizeProfile);
}

export async function getDiscoveryProfiles() {
  return getPageContent("/profiles/search");
}

export function getProfileById(id) {
  return api.get(`/profiles/${encodeURIComponent(id)}/public`).then(normalizeProfile);
}

export function getMyProfile() {
  return api.get("/profiles/me").then(normalizeProfile);
}

/**
 * Format compact attendu par l'en-tête de navigation (Layout).
 */
export async function getCurrentUser() {
  const profile = await getMyProfile();
  return {
    ...profile,
    name: profile.name || "Mon profil",
    photo: profile.photo || null,
  };
}

// ===================== Référentiels (listes locales) =====================

export function getUniversities() {
  return Promise.resolve(UNIVERSITIES);
}

export function getFaculties() {
  return Promise.resolve(FACULTIES);
}

export function getNeighborhoods() {
  return Promise.resolve(NEIGHBORHOODS);
}

export function getInterests() {
  return Promise.resolve(INTERESTS);
}

// ===================== Médias =====================

/**
 * Upload plusieurs photos via POST /media/upload (multipart, champ `file`).
 * Renvoie `{ photos: string[] }` contenant les URLs Cloudinary.
 */
export async function uploadProfilePhotos(files = []) {
  const photos = await Promise.all(
    files.map(async (file) => {
      const form = new FormData();
      form.append("file", file);
      const uploaded = await api.post("/media/upload", form);
      return uploaded.url;
    })
  );
  return { photos };
}

/** Upload d'une photo unique (avatar) — renvoie l'URL Cloudinary. */
export async function uploadAvatar(file) {
  const { photos } = await uploadProfilePhotos([file]);
  return photos[0];
}

// ===================== Mise à jour du profil =====================

const GENDER_TO_API = { Homme: "MALE", Femme: "FEMALE", Autre: "OTHER" };

/**
 * PUT /profiles/me remplace intégralement le profil (pas de PATCH côté
 * backend) : on relit le profil courant, on fusionne les champs modifiés puis
 * on renvoie le tout.
 *
 * Seuls les champs réellement persistés par `ProfileRequest` sont envoyés :
 * `faculty -> fieldOfStudy`, `about -> bio`, `birthDate -> dateOfBirth`,
 * `gender` traduit vers l'enum `Gender`. Le "niveau d'étude" n'a pas d'équivalent
 * en base et n'est donc pas envoyé (voir API_CONTRACT.md).
 */
export async function updateProfile(partialData) {
  const current = await getMyProfile().catch(() => ({}));

  const gender =
    partialData.gender === undefined
      ? current.gender
      : GENDER_TO_API[partialData.gender] ?? partialData.gender;

  const merged = {
    avatarUrl: partialData.photo ?? partialData.avatarUrl ?? current.avatarUrl ?? null,
    firstName: current.firstName,
    lastName: current.lastName,
    gender,
    dateOfBirth: partialData.birthDate ?? partialData.dateOfBirth ?? current.dateOfBirth ?? null,
    university: partialData.university ?? current.university ?? null,
    fieldOfStudy: partialData.faculty ?? partialData.fieldOfStudy ?? current.fieldOfStudy ?? null,
    neighborhood: partialData.neighborhood ?? current.neighborhood ?? null,
    city: partialData.city ?? current.city ?? null,
    bio: partialData.about ?? partialData.bio ?? current.bio ?? null,
    interests: partialData.interests ?? current.interests ?? [],
  };

  return api.put("/profiles/me", merged).then(normalizeProfile);
}

/**
 * Like : POST /likes { cibleId } — `cibleId` est le `legacyId` du profil ciblé
 * (identifiant du module temps réel), jamais son UUID.
 */
export function likeProfile(legacyId) {
  if (legacyId === undefined || legacyId === null) {
    return Promise.reject(new Error("Ce profil ne peut pas encore être liké."));
  }
  return api.post("/likes", { cibleId: legacyId });
}
