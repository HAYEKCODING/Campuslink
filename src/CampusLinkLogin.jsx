import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Mail, Lock, Eye, EyeOff, Heart } from "lucide-react";
import { login, requestPasswordReset } from "./services/authService";

/**
 * CampusLink — Se connecter
 * Formulaire entièrement contrôlé, envoyé à authService.login().
 */

function InputField({ icon: Icon, placeholder, type = "text", toggle, value, onChange }) {
  const [show, setShow] = useState(false);
  const inputType = toggle ? (show ? "text" : "password") : type;

  return (
    <div className="relative">
      <Icon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
      <input
        type={inputType}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full pl-10 pr-10 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
      />
      {toggle && (
        <button
          type="button"
          onClick={() => setShow((s) => !s)}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
          aria-label={show ? "Masquer le mot de passe" : "Afficher le mot de passe"}
        >
          {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
        </button>
      )}
    </div>
  );
}

function GoogleIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" {...props}>
      <path fill="#4285F4" d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12v4.3h6.47a5.53 5.53 0 0 1-2.4 3.63v3h3.87c2.27-2.09 3.55-5.17 3.55-8.66z" />
      <path fill="#34A853" d="M12 24c3.24 0 5.95-1.07 7.93-2.91l-3.87-3c-1.07.72-2.45 1.15-4.06 1.15-3.13 0-5.78-2.11-6.73-4.96H1.27v3.1A12 12 0 0 0 12 24z" />
      <path fill="#FBBC05" d="M5.27 14.28a7.2 7.2 0 0 1 0-4.56v-3.1H1.27a12 12 0 0 0 0 10.76z" />
      <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.94 1.19 15.24 0 12 0 7.31 0 3.26 2.69 1.27 6.62l4 3.1C6.22 6.86 8.87 4.75 12 4.75z" />
    </svg>
  );
}

function FacebookIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="#1877F2" {...props}>
      <path d="M22 12a10 10 0 1 0-11.56 9.88v-6.99H7.9V12h2.54V9.8c0-2.5 1.49-3.89 3.78-3.89 1.1 0 2.24.2 2.24.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56V12h2.78l-.44 2.89h-2.34v6.99A10 10 0 0 0 22 12z" />
    </svg>
  );
}

function AppleIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="#000000" {...props}>
      <path d="M16.36 1.43c0 1.14-.42 2.2-1.16 3.02-.83.9-2.1 1.6-3.31 1.5-.14-1.1.4-2.28 1.15-3.06.84-.87 2.24-1.5 3.32-1.46zM20.7 17.2c-.53 1.2-.78 1.74-1.47 2.8-.96 1.48-2.3 3.32-3.98 3.34-1.48.01-1.86-.96-3.87-.95-2 0-2.43.94-3.9.96-1.68.02-2.96-1.65-3.92-3.13-2.68-4.1-2.96-8.92-1.31-11.48 1.17-1.82 3.02-2.9 4.75-2.9 1.77 0 2.88 1 4.35 1 1.42 0 2.28-.98 4.33-.98 1.53 0 3.15.83 4.3 2.27-3.79 2.08-3.18 7.5.72 9.07z" />
    </svg>
  );
}

export default function CampusLinkLogin() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [resetMode, setResetMode] = useState(false);
  const [resetStatus, setResetStatus] = useState(null); // null | "sending" | "sent" | "error"

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    login({ email, password, remember })
      .then(() => navigate("/app"))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  };

  const handlePasswordReset = (e) => {
    e.preventDefault();
    setResetStatus("sending");
    requestPasswordReset(email)
      .then(() => setResetStatus("sent"))
      .catch((err) => {
        setResetStatus("error");
        setError(err.message);
      });
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-8">
        <div className="flex items-center gap-2 justify-center mb-6">
          <Heart className="w-6 h-6 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
        </div>

        <h1 className="text-2xl font-extrabold text-center mb-1">Se connecter</h1>
        <p className="text-sm text-slate-400 text-center mb-8">Bienvenue de retour !</p>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <InputField icon={Mail} placeholder="Email universitaire" type="email" value={email} onChange={setEmail} />
          <InputField icon={Lock} placeholder="Mot de passe" toggle value={password} onChange={setPassword} />

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-slate-500 cursor-pointer">
              <input
                type="checkbox"
                checked={remember}
                onChange={() => setRemember((r) => !r)}
                className="w-4 h-4 rounded border-slate-300 text-violet-600 focus:ring-violet-400"
              />
              Se souvenir de moi
            </label>
            <button
              type="button"
              onClick={() => {
                setResetMode((m) => !m);
                setResetStatus(null);
                setError(null);
              }}
              className="text-violet-600 font-medium hover:underline"
            >
              Mot de passe oublié ?
            </button>
          </div>

          {resetMode && (
            <div className="rounded-lg bg-slate-50 p-3 space-y-2">
              <p className="text-xs text-slate-500">
                Recevez un lien de réinitialisation par email.
              </p>
              <form onSubmit={handlePasswordReset} className="flex gap-2">
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Votre email"
                  aria-label="Email pour réinitialiser le mot de passe"
                  className="flex-1 px-3 py-2 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400"
                />
                <button
                  type="submit"
                  disabled={resetStatus === "sending"}
                  className="px-3 py-2 text-xs font-semibold text-white bg-violet-600 hover:bg-violet-700 rounded-lg disabled:opacity-60"
                >
                  {resetStatus === "sending" ? "..." : "Envoyer"}
                </button>
              </form>
              {resetStatus === "sent" && (
                <p className="text-xs text-emerald-600">
                  Si un compte existe, un email de réception vient d'être envoyé.
                </p>
              )}
              {resetStatus === "error" && (
                <p className="text-xs text-rose-500">
                  {error || "Impossible d'envoyer l'email pour le moment."}
                </p>
              )}
            </div>
          )}

          {error && <p className="text-sm text-rose-500">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            {submitting ? "Connexion..." : "Se connecter"}
          </button>
        </form>

        <div className="flex items-center gap-3 my-6">
          <div className="flex-1 h-px bg-slate-200" />
          <span className="text-xs text-slate-400">ou continuer avec</span>
          <div className="flex-1 h-px bg-slate-200" />
        </div>

        <div className="grid grid-cols-3 gap-3">
          {/* Pas d'OAuth côté backend pour l'instant : boutons conservés mais
              explicitement inactifs plutôt que cliquables sans effet. */}
          <button
            type="button"
            disabled
            title="Connexion OAuth bientôt disponible"
            aria-label="Continuer avec Google (bientôt disponible)"
            className="flex items-center justify-center py-2.5 rounded-lg border border-slate-200 opacity-50 cursor-not-allowed"
          >
            <GoogleIcon />
          </button>
          <button
            type="button"
            disabled
            title="Connexion OAuth bientôt disponible"
            aria-label="Continuer avec Facebook (bientôt disponible)"
            className="flex items-center justify-center py-2.5 rounded-lg border border-slate-200 opacity-50 cursor-not-allowed"
          >
            <FacebookIcon />
          </button>
          <button
            type="button"
            disabled
            title="Connexion OAuth bientôt disponible"
            aria-label="Continuer avec Apple (bientôt disponible)"
            className="flex items-center justify-center py-2.5 rounded-lg border border-slate-200 opacity-50 cursor-not-allowed"
          >
            <AppleIcon />
          </button>
        </div>

        <p className="text-sm text-slate-500 text-center mt-6">
          Pas encore membre ?{" "}
          <Link to="/inscription" className="text-violet-600 font-semibold hover:underline">
            Créer un compte
          </Link>
        </p>
      </div>
    </div>
  );
}
