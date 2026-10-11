import React, { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";

import CampusLinkLanding from "./CampusLinkLanding";
import CampusLinkSignup from "./CampusLinkSignup";
import CampusLinkLogin from "./CampusLinkLogin";
import ResetPasswordPage from "./ResetPasswordPage";
import OnboardingStep1 from "./OnboardingStep1";
import OnboardingStep2 from "./OnboardingStep2";
import OnboardingStep3 from "./OnboardingStep3";
import DiscoveryFeed from "./DiscoveryFeed";
import SearchFilters from "./SearchFilters";
import ProfileDetail from "./ProfileDetail";
import MessagesList from "./MessagesList";
import ChatConversation from "./ChatConversation";
import MyMatches from "./MyMatches";
import Settings from "./Settings";
import { isAuthenticated, logout } from "./services/authService";
import { getRealtimeClient } from "./lib/realtime";

/**
 * CampusLink — Routeur principal
 *
 * Structure des routes :
 * /                     -> Landing page
 * /inscription          -> Créer un compte
 * /connexion            -> Se connecter
 * /reset-password       -> Réinitialiser le mot de passe (lien email)
 * /onboarding/1|2|3      -> Complétez votre profil (3 étapes)
 * /app                  -> Fil de découverte (Accueil)
 * /app/recherche        -> Recherche & filtres
 * /app/profil/:id       -> Profil détaillé d'un membre
 * /app/messages         -> Liste des conversations
 * /app/messages/:id     -> Conversation avec un membre
 * /app/matchs           -> Mes matchs
 * /app/parametres       -> Paramètres
 *
 * RequireAuth protège les routes /app/* : sans token, redirection vers
 * /connexion. UnauthorizedListener réagit à un 401 émis par lib/api.js
 * (token expiré / refresh échoué) en déconnectant et redirigeant.
 */

function RequireAuth({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/connexion" replace />;
  }
  return children;
}

function UnauthorizedListener() {
  const navigate = useNavigate();

  useEffect(() => {
    const handleUnauthorized = () => {
      logout();
      getRealtimeClient().deactivate();
      navigate("/connexion", { replace: true });
    };
    window.addEventListener("campuslink:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("campuslink:unauthorized", handleUnauthorized);
  }, [navigate]);

  return null;
}

/**
 * Ouvre (ou referme) la connexion STOMP temps réel selon l'état de session.
 * Singleton géré par lib/realtime.js : pas de connexion tant qu'il n'y a
 * aucun token, déconnexion à la déconnexion / expiration de session.
 */
function RealtimeSession() {
  useEffect(() => {
    const rt = getRealtimeClient();
    if (isAuthenticated()) rt.connect();

    const handleLogin = () => rt.connect();
    const handleLogout = () => rt.deactivate();
    // Émis par authService après stockage (login) ou purge (logout) des tokens.
    window.addEventListener("campuslink:authenticated", handleLogin);
    window.addEventListener("campuslink:logged-out", handleLogout);
    return () => {
      window.removeEventListener("campuslink:authenticated", handleLogin);
      window.removeEventListener("campuslink:logged-out", handleLogout);
      rt.deactivate();
    };
  }, []);

  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <UnauthorizedListener />
      <RealtimeSession />
      <Routes>
        {/* Public */}
        <Route path="/" element={<CampusLinkLanding />} />
        <Route path="/inscription" element={<CampusLinkSignup />} />
        <Route path="/connexion" element={<CampusLinkLogin />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        {/* Onboarding */}
        <Route path="/onboarding/1" element={<RequireAuth><OnboardingStep1 /></RequireAuth>} />
        <Route path="/onboarding/2" element={<RequireAuth><OnboardingStep2 /></RequireAuth>} />
        <Route path="/onboarding/3" element={<RequireAuth><OnboardingStep3 /></RequireAuth>} />

        {/* App connectée (protégée par JWT) */}
        <Route path="/app" element={<RequireAuth><DiscoveryFeed /></RequireAuth>} />
        <Route path="/app/recherche" element={<RequireAuth><SearchFilters /></RequireAuth>} />
        <Route path="/app/profil/:id" element={<RequireAuth><ProfileDetail /></RequireAuth>} />
        <Route path="/app/messages" element={<RequireAuth><MessagesList /></RequireAuth>} />
        <Route path="/app/messages/:id" element={<RequireAuth><ChatConversation /></RequireAuth>} />
        <Route path="/app/matchs" element={<RequireAuth><MyMatches /></RequireAuth>} />
        <Route path="/app/parametres" element={<RequireAuth><Settings /></RequireAuth>} />

        {/* Repli */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
