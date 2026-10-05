import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { User, Mail, Phone, Lock, Eye, EyeOff, Heart } from "lucide-react";
import { signup } from "./services/authService";

/**
 * CampusLink — Créer un compte
 * Formulaire entièrement contrôlé, avec validation obligatoire de chaque
 * champ avant de pouvoir soumettre (impossible d'avancer sans avoir
 * correctement rempli le formulaire).
 */

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

function InputField({ icon: Icon, placeholder, type = "text", toggle, value, onChange, error }) {
  const [show, setShow] = useState(false);
  const inputType = toggle ? (show ? "text" : "password") : type;

  return (
    <div>
      <div className="relative">
        <Icon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type={inputType}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className={`w-full pl-10 pr-10 py-3 text-sm rounded-lg border placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:border-transparent ${
            error ? "border-rose-400 focus:ring-rose-300" : "border-slate-200 focus:ring-violet-400"
          }`}
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
      {error && <p className="text-xs text-rose-500 mt-1">{error}</p>}
    </div>
  );
}

export default function CampusLinkSignup() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [firstName, setFirstName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [gender, setGender] = useState("Homme");
  const [accepted, setAccepted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const validate = () => {
    const errors = {};

    if (!fullName.trim()) errors.fullName = "Le nom complet est obligatoire.";
    if (!firstName.trim()) errors.firstName = "Le prénom est obligatoire.";

    if (!email.trim()) errors.email = "L'email universitaire est obligatoire.";
    else if (!EMAIL_REGEX.test(email.trim())) errors.email = "L'adresse email n'est pas valide.";

    if (!phone.trim()) errors.phone = "Le numéro de téléphone est obligatoire.";

    if (!password) errors.password = "Le mot de passe est obligatoire.";
    else if (password.length < MIN_PASSWORD_LENGTH)
      errors.password = `Le mot de passe doit contenir au moins ${MIN_PASSWORD_LENGTH} caractères.`;

    if (!confirmPassword) errors.confirmPassword = "Merci de confirmer le mot de passe.";
    else if (password && confirmPassword !== password)
      errors.confirmPassword = "Les mots de passe ne correspondent pas.";

    return errors;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError(null);

    const errors = validate();
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) {
      setError("Merci de compléter correctement tous les champs obligatoires.");
      return;
    }
    if (!accepted) {
      setError("Merci d'accepter les conditions d'utilisation.");
      return;
    }

    setSubmitting(true);
    signup({ fullName, firstName, email, phone, password, gender })
      .then(() => navigate("/onboarding/1"))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-8">
        <div className="flex items-center gap-2 justify-center mb-6">
          <Heart className="w-6 h-6 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
        </div>

        <h1 className="text-2xl font-extrabold text-center mb-1">Créer un compte</h1>
        <p className="text-sm text-slate-400 text-center mb-8">
          Rejoignez la communauté CampusLink
        </p>

        <form className="space-y-4" onSubmit={handleSubmit} noValidate>
          <InputField
            icon={User}
            placeholder="Nom complet"
            value={fullName}
            onChange={setFullName}
            error={fieldErrors.fullName}
          />
          <InputField
            icon={User}
            placeholder="Prénom"
            value={firstName}
            onChange={setFirstName}
            error={fieldErrors.firstName}
          />
          <InputField
            icon={Mail}
            placeholder="Email universitaire"
            type="email"
            value={email}
            onChange={setEmail}
            error={fieldErrors.email}
          />
          <InputField
            icon={Phone}
            placeholder="Téléphone"
            type="tel"
            value={phone}
            onChange={setPhone}
            error={fieldErrors.phone}
          />
          <InputField
            icon={Lock}
            placeholder="Mot de passe (8 caractères minimum)"
            toggle
            value={password}
            onChange={setPassword}
            error={fieldErrors.password}
          />
          <InputField
            icon={Lock}
            placeholder="Confirmer le mot de passe"
            toggle
            value={confirmPassword}
            onChange={setConfirmPassword}
            error={fieldErrors.confirmPassword}
          />

          <div>
            <p className="text-sm font-semibold text-slate-700 mb-2">Je suis</p>
            <div className="grid grid-cols-3 gap-2">
              {["Homme", "Femme", "Autre"].map((option) => (
                <button
                  type="button"
                  key={option}
                  onClick={() => setGender(option)}
                  className={`text-sm font-medium py-2.5 rounded-lg border transition-colors ${
                    gender === option
                      ? "bg-violet-600 text-white border-violet-600"
                      : "bg-white text-slate-600 border-slate-200 hover:border-violet-300"
                  }`}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>

          <label className="flex items-start gap-2 text-sm text-slate-500 cursor-pointer">
            <input
              type="checkbox"
              checked={accepted}
              onChange={() => setAccepted((a) => !a)}
              className="mt-0.5 w-4 h-4 rounded border-slate-300 text-violet-600 focus:ring-violet-400"
            />
            <span>
              J'accepte les{" "}
              <a href="#" className="text-violet-600 font-medium hover:underline">
                Conditions d'utilisation
              </a>{" "}
              et la{" "}
              <a href="#" className="text-violet-600 font-medium hover:underline">
                Politique de confidentialité
              </a>
            </span>
          </label>

          {error && <p className="text-sm text-rose-500">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            {submitting ? "Création..." : "Créer mon compte"}
          </button>
        </form>

        <p className="text-sm text-slate-500 text-center mt-6">
          Déjà un compte ?{" "}
          <Link to="/connexion" className="text-violet-600 font-semibold hover:underline">
            Se connecter
          </Link>
        </p>
      </div>
    </div>
  );
}
