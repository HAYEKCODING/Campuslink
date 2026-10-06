import { api, setAuthTokens, isAuthenticated } from "../lib/api";

/**
 * Service d'authentification, aligné sur le backend réel :
 * - POST /auth/register (email, password, confirmPassword, firstName, lastName)
 *   ne renvoie pas de token (inscription != connexion) -> on connecte
 *   automatiquement juste après pour ne pas casser le flux existant
 *   (redirection vers l'onboarding).
 * - POST /auth/login renvoie { accessToken, refreshToken, user }.
 *
 * `phone` et `gender` sont collectés par le formulaire mais n'ont pour
 * l'instant aucun champ équivalent côté backend : ils ne sont pas envoyés
 * (à ajouter côté backend si besoin plus tard).
 */

export async function signup({ fullName, firstName, email, password, confirmPassword }) {
  await api.post("/auth/register", {
    email,
    password,
    confirmPassword: confirmPassword ?? password,
    firstName,
    lastName: fullName, // le champ "Nom complet" du formulaire sert de nom de famille
  });

  return login({ email, password });
}

export async function login({ email, password }) {
  const data = await api.post("/auth/login", { email, password });
  setAuthTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
  return data;
}

export function logout() {
  setAuthTokens({});
}

// Déconnexion volontaire : révoque aussi le refresh token côté serveur
// (POST /auth/logout), puis nettoie le stockage local quoi qu'il arrive.
export async function logoutRemote() {
  const refreshToken = localStorage.getItem("campuslink_refresh_token");
  try {
    if (refreshToken) await api.post("/auth/logout", { refreshToken });
  } catch {
    // best effort : on se déconnecte localement même si le serveur est injoignable
  } finally {
    logout();
  }
}

export function requestPasswordReset(email) {
  return api.post("/auth/forgot-password", { email });
}

export { isAuthenticated };
