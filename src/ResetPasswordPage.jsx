import React, { useState } from "react";
import { useNavigate, Link, useSearchParams } from "react-router-dom";
import { Lock, Eye, EyeOff, Heart } from "lucide-react";
import { resetPassword } from "./services/authService";

/**
 * CampusLink — Réinitialiser le mot de passe.
 *
 * Le lien reçu par email pointe vers /reset-password?token=… (backend :
 * campuslink.password-reset.reset-url). Cette route n'existait pas côté
 * front : le lien redirigeait vers l'accueil et le flux était bloqué.
 */

function InputField({ icon: Icon, placeholder, value, onChange }) {
  const [show, setShow] = useState(false);

  return (
    <div className="relative">
      <Icon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
      <input
        type={show ? "text" : "password"}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full pl-10 pr-10 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
      />
      <button
        type="button"
        onClick={() => setShow((s) => !s)}
        className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
        aria-label={show ? "Masquer le mot de passe" : "Afficher le mot de passe"}
      >
        {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
      </button>
    </div>
  );
}

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const navigate = useNavigate();

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setError(null);

    if (password !== confirm) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }
    if (password.length < 8 || password.length > 72 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError("Le mot de passe doit contenir entre 8 et 72 caractères, au moins une lettre et un chiffre.");
      return;
    }

    setSubmitting(true);
    resetPassword(token, password, confirm)
      .then(() => setDone(true))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-8">
        <div className="flex items-center justify-center mb-6">
          <Heart className="w-6 h-6 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
        </div>

        <h1 className="text-2xl font-extrabold text-center mb-1">Nouveau mot de passe</h1>
        <p className="text-sm text-slate-400 text-center mb-8">
          {done ? "Votre mot de passe a été mis à jour." : "Choisissez un mot de passe sécurisé pour votre compte."}
        </p>

        {!token ? (
          <div className="space-y-4 text-center">
            <p className="text-sm text-rose-500">
              Lien invalide : aucun jeton de réinitialisation n'a été fourni.
            </p>
            <Link
              to="/connexion"
              className="inline-block w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg"
            >
              Retour à la connexion
            </Link>
          </div>
        ) : done ? (
          <div className="space-y-4 text-center">
            <p className="text-sm text-emerald-600">
              Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.
            </p>
            <button
              type="button"
              onClick={() => navigate("/connexion")}
              className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg"
            >
              Se connecter
            </button>
          </div>
        ) : (
          <form className="space-y-4" onSubmit={handleSubmit}>
            <InputField icon={Lock} placeholder="Nouveau mot de passe" value={password} onChange={setPassword} />
            <InputField icon={Lock} placeholder="Confirmer le mot de passe" value={confirm} onChange={setConfirm} />

            {error && <p className="text-sm text-rose-500">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
            >
              {submitting ? "Enregistrement..." : "Réinitialiser le mot de passe"}
            </button>
          </form>
        )}

        <p className="text-sm text-slate-500 text-center mt-6">
          Vous vous souvenez de votre mot de passe ?{" "}
          <Link to="/connexion" className="text-violet-600 font-semibold hover:underline">
            Se connecter
          </Link>
        </p>
      </div>
    </div>
  );
}
