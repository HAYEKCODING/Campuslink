import React, { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";

import CampusLinkLanding from "./CampusLinkLanding";
import CampusLinkSignup from "./CampusLinkSignup";
import CampusLinkLogin from "./CampusLinkLogin";
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

/**
 * CampusLink — Routeur principal
 *
 * Structure des routes :
 * /                     -> Landing page
 * /inscription          -> Créer un compte
 * /connexion            -> Se connecter
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
      navigate("/connexion", { replace: true });
    };
    window.addEventListener("campuslink:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("campuslink:unauthorized", handleUnauthorized);
  }, [navigate]);

  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <UnauthorizedListener />
      <Routes>
        {/* Public */}
        <Route path="/" element={<CampusLinkLanding />} />
        <Route path="/inscription" element={<CampusLinkSignup />} />
        <Route path="/connexion" element={<CampusLinkLogin />} />

        {/* Onboarding */}
        <Route path="/onboarding/1" element={<OnboardingStep1 />} />
        <Route path="/onboarding/2" element={<OnboardingStep2 />} />
        <Route path="/onboarding/3" element={<OnboardingStep3 />} />

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
