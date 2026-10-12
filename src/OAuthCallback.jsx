import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { setAuthTokens } from "./lib/api";

export default function OAuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const accessToken = params.get("access_token");
    const refreshToken = params.get("refresh_token");
    const newAccount = params.get("new_account") === "true";

    window.history.replaceState(null, "", window.location.pathname);
    if (!accessToken || !refreshToken) {
      navigate("/connexion?oauthError=failed", { replace: true });
      return;
    }

    setAuthTokens({ accessToken, refreshToken });
    window.dispatchEvent(new CustomEvent("campuslink:authenticated"));
    navigate(newAccount ? "/onboarding/1" : "/app", { replace: true });
  }, [navigate]);

  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-600">
      Connexion sécurisée en cours…
    </main>
  );
}
