import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getNeighborhoods, getInterests, updateProfile } from "./services/profileService";
import { GENDERS } from "./lib/referenceData";

/**
 * CampusLink — Complétez votre profil (Étape 2/3)
 *
 * - Ville fixée à Bouaké (seule zone couverte pour l'instant).
 * - Quartier : champ éditable avec suggestions (référentiel local).
 * - Genre : envoyé au format enum de l'API (MALE / FEMALE / OTHER) — le champ
 *   `Profile.gender` existe en base mais n'était pas exposé par le DTO, la
 *   valeur était perdue.
 * - Centres d'intérêt : suggestions + ajout libre (tags gérés comme de simples
 *   chaînes par le backend).
 */

function ProgressDots({ step, total = 3 }) {
  return (
    <div className="flex items-center gap-2">
      {Array.from({ length: total }).map((_, i) => (
        <React.Fragment key={i}>
          <span className={`w-2.5 h-2.5 rounded-full ${i < step ? "bg-violet-600" : "bg-slate-200"}`} />
          {i < total - 1 && (
            <span className={`w-8 h-0.5 ${i < step - 1 ? "bg-violet-600" : "bg-slate-200"}`} />
          )}
        </React.Fragment>
      ))}
    </div>
  );
}

export default function OnboardingStep2() {
  const navigate = useNavigate();
  const [neighborhood, setNeighborhood] = useState("");
  const [gender, setGender] = useState("");
  const [about, setAbout] = useState("");
  const [interests, setInterests] = useState([]);
  const [customInterest, setCustomInterest] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const { data: neighborhoods } = useAsyncData(() => getNeighborhoods(), []);
  const { data: availableInterests } = useAsyncData(() => getInterests(), []);

  const toggleInterest = (interest) => {
    setInterests((prev) =>
      prev.includes(interest) ? prev.filter((i) => i !== interest) : [...prev, interest]
    );
  };

  const addCustomInterest = () => {
    const value = customInterest.trim();
    if (!value) return;
    setInterests((prev) => (prev.includes(value) ? prev : [...prev, value]));
    setCustomInterest("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!neighborhood.trim() || !gender) {
      setSubmitError("Merci de compléter tous les champs obligatoires avant de continuer.");
      return;
    }
    if (interests.length < 3) {
      setSubmitError("Merci de choisir au moins 3 centres d'intérêt.");
      return;
    }

    setSubmitting(true);
    try {
      await updateProfile({
        city: "Bouaké",
        neighborhood: neighborhood.trim(),
        gender,
        about,
        interests,
      });
      navigate("/onboarding/3");
    } catch (err) {
      setSubmitError(err.message || "L'enregistrement a échoué.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-8">
        <h1 className="text-xl font-extrabold mb-1">Complétez votre profil</h1>
        <div className="flex items-center justify-between mb-6">
          <p className="text-sm text-slate-400">Étape 2 sur 3</p>
          <ProgressDots step={2} />
        </div>

        <form className="space-y-5" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="ville" className="text-sm font-semibold text-slate-700 mb-1.5 block">
              Ville
            </label>
            <input
              id="ville"
              type="text"
              value="Bouaké"
              disabled
              className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 bg-slate-50 text-slate-500"
            />
          </div>

          <div>
            <label htmlFor="quartier" className="text-sm font-semibold text-slate-700 mb-1.5 block">
              Quartier *
            </label>
            <input
              id="quartier"
              list="onboarding-quartier"
              type="text"
              placeholder="Saisir ou choisir votre quartier"
              value={neighborhood}
              onChange={(e) => setNeighborhood(e.target.value)}
              className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
            />
            <datalist id="onboarding-quartier">
              {(neighborhoods || []).map((n) => (
                <option key={n.id ?? n} value={n.id ?? n}>
                  {n.label ?? n}
                </option>
              ))}
            </datalist>
          </div>

          <div>
            <span className="text-sm font-semibold text-slate-700 mb-2 block">Genre *</span>
            <div className="flex items-center gap-6">
              {GENDERS.map((option) => (
                <label key={option.value} className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                  <input
                    type="radio"
                    name="gender"
                    value={option.value}
                    checked={gender === option.value}
                    onChange={() => setGender(option.value)}
                    className="w-4 h-4 text-violet-600 focus:ring-violet-400"
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </div>

          <div>
            <label htmlFor="about" className="text-sm font-semibold text-slate-700 mb-1.5 block">
              À propos de moi
            </label>
            <textarea
              id="about"
              value={about}
              onChange={(e) => setAbout(e.target.value.slice(0, 300))}
              placeholder="Parlez un peu de vous..."
              rows={3}
              className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent resize-none"
            />
            <p className="text-xs text-slate-400 text-right mt-1">{about.length}/300</p>
          </div>

          <div>
            <span className="text-sm font-semibold text-slate-700 block">Centres d'intérêt *</span>
            <p className="text-xs text-slate-400 mb-2">choisissez au moins 3</p>
            <div className="flex flex-wrap gap-2">
              {(availableInterests || []).map((interest) => {
                const label = interest.label ?? interest;
                const active = interests.includes(label);
                return (
                  <button
                    type="button"
                    key={interest.id ?? interest}
                    onClick={() => toggleInterest(label)}
                    className={`text-sm px-4 py-2 rounded-full border transition-colors ${
                      active
                        ? "bg-violet-600 text-white border-violet-600"
                        : "bg-white text-slate-600 border-slate-200 hover:border-violet-300"
                    }`}
                  >
                    {label}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center gap-2 mt-3">
              <input
                type="text"
                value={customInterest}
                onChange={(e) => setCustomInterest(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addCustomInterest();
                  }
                }}
                placeholder="Ajouter un centre d'intérêt"
                aria-label="Ajouter un centre d'intérêt"
                className="flex-1 px-4 py-2 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400"
              />
              <button
                type="button"
                onClick={addCustomInterest}
                aria-label="Ajouter"
                className="w-9 h-9 rounded-lg bg-violet-600 hover:bg-violet-700 text-white flex items-center justify-center flex-shrink-0"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>

            {interests.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-3">
                {interests.map((interest) => (
                  <button
                    type="button"
                    key={interest}
                    onClick={() => toggleInterest(interest)}
                    className="text-[11px] font-medium text-white bg-violet-600 px-2.5 py-1 rounded-full"
                    title="Retirer"
                  >
                    {interest} ×
                  </button>
                ))}
              </div>
            )}
          </div>

          {submitError && <p className="text-sm text-rose-500">{submitError}</p>}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate("/onboarding/1")}
              className="flex-1 border border-slate-200 text-slate-600 text-sm font-semibold py-3 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Retour
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
            >
              {submitting ? "Enregistrement..." : "Suivant"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
