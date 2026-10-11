/**
 * Client HTTP générique pour CampusLink.
 *
 * L'URL de base se configure via VITE_API_URL (.env). Le backend renvoie deux
 * formes de réponse selon le module :
 *  - modules principaux (auth, profiles...) : enveloppe {success, message, data}
 *  - module temps réel (likes, matches, messages...) : JSON brut
 * unwrap() gère les deux de façon transparente pour les services.
 *
 * Authentification : access token + refresh token (voir /auth/refresh-token).
 * Sur un 401, on tente un unique refresh puis on rejoue la requête ; si ça
 * échoue, on nettoie les tokens et on émet "campuslink:unauthorized" (écouté
 * dans App.jsx pour rediriger vers /connexion).
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || "/api";

const ACCESS_TOKEN_KEY = "campuslink_token";
const REFRESH_TOKEN_KEY = "campuslink_refresh_token";

function getToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setAuthTokens({ accessToken, refreshToken } = {}) {
  if (accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  else localStorage.removeItem(ACCESS_TOKEN_KEY);

  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  else localStorage.removeItem(REFRESH_TOKEN_KEY);
}

// Compat : certains appels ne connaissent qu'un seul token.
export function setAuthToken(token) {
  setAuthTokens({ accessToken: token, refreshToken: getRefreshToken() });
}

export function isAuthenticated() {
  return !!getToken();
}

function clearAuth() {
  setAuthTokens({});
}

function unwrap(json) {
  if (json && typeof json === "object" && "success" in json && "data" in json) {
    return json.data;
  }
  return json;
}

/**
 * Recadre une URL de média locale sur l'origine API courante.
 *
 * Le backend construit les URLs de fichiers locaux depuis l'hôte:port de la
 * requête d'upload (`ServletUriComponentsBuilder`), et ces URLs sont ensuite
 * persistées en base (avatar_url…). Si le port d'écoute change (8081 -> 8080,
 * backend derrière un proxy, démo sur un autre hôte), toutes les URLs déjà
 * stockées deviennent mortes (ERR_CONNECTION_REFUSED).
 *
 * On rebase donc tout chemin se terminant par `/media/files/{fichier}` sur
 * l'origine de VITE_API_URL. Les URLs Cloudinary (res.cloudinary.com) ne
 * matchent pas ce motif et restent intactes.
 */
export function resolveMediaUrl(url) {
  if (!url || typeof url !== "string") return url;
  const match = url.match(/^https?:\/\/[^/]+(\/[^?]*)?\/media\/files\/([A-Za-z0-9][A-Za-z0-9._-]*)$/);
  if (!match) return url;
  return `${API_BASE_URL}/media/files/${match[2]}`;
}

function buildFetchOptions(method, body, headers) {
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  return {
    method,
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  };
}

let refreshInFlight = null;

async function tryRefreshToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh-token`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) return false;

    const data = unwrap(await response.json());
    setAuthTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    return true;
  } catch {
    return false;
  }
}

async function request(path, { method = "GET", body, headers = {}, skipAuthRetry = false, ...rest } = {}) {
  // Les routes /auth/* (login, register, reset...) renvoient un 401 métier
  // ("Identifiants invalides.") : ce n'est pas une session expirée.
  const isAuthRoute = path.startsWith("/auth/");
  // Une session n'existe que si au moins un token est stocké : un 401 reçu
  // sans session (visiteur anonyme sur une route qui exigerait une auth) n'est
  // pas une "session expirée" et ne doit pas déclencher de redirection.
  const hadSession = !!getToken() || !!getRefreshToken();
  let response = await fetch(`${API_BASE_URL}${path}`, { ...buildFetchOptions(method, body, headers), ...rest });

  if (response.status === 401 && !skipAuthRetry && !isAuthRoute && getRefreshToken()) {
    refreshInFlight = refreshInFlight || tryRefreshToken().finally(() => {
      refreshInFlight = null;
    });
    const refreshed = await refreshInFlight;
    if (refreshed) {
      response = await fetch(`${API_BASE_URL}${path}`, { ...buildFetchOptions(method, body, headers), ...rest });
    }
  }

  if (response.status === 401 && !isAuthRoute) {
    if (hadSession) {
      // Session réellement expirée (refresh échoué ou absent) : nettoyage +
      // redirection via App.jsx.
      clearAuth();
      window.dispatchEvent(new CustomEvent("campuslink:unauthorized"));
    }
    const error = new Error(
      hadSession ? "Session expirée, veuillez vous reconnecter." : "Authentification requise."
    );
    error.status = 401;
    throw error;
  }

  if (response.status === 403) {
    const error = new Error("Vous n'avez pas les droits nécessaires pour cette action.");
    error.status = 403;
    throw error;
  }

  if (!response.ok) {
    let message = `Erreur API (${response.status})`;
    let fieldErrors;
    try {
      const errJson = await response.json();
      message = errJson.message || message;
      fieldErrors = errJson.fieldErrors;
    } catch {
      // pas de corps JSON dans la réponse d'erreur
    }
    const error = new Error(message);
    error.status = response.status;
    if (fieldErrors) error.fieldErrors = fieldErrors;
    throw error;
  }

  if (response.status === 204) return null;
  return unwrap(await response.json());
}

export const api = {
  get: (path, options) => request(path, { ...options, method: "GET" }),
  post: (path, body, options) => request(path, { ...options, method: "POST", body }),
  patch: (path, body, options) => request(path, { ...options, method: "PATCH", body }),
  put: (path, body, options) => request(path, { ...options, method: "PUT", body }),
  delete: (path, options) => request(path, { ...options, method: "DELETE" }),
};
