import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Camera, Calendar } from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getUniversities, getFaculties, uploadAvatar, updateProfile } from "./services/profileService";

/**
 * CampusLink — Complétez votre profil (Étape 1/3)
 *
 * - La photo est envoyée via POST /media/upload (URL Cloudinary) puis stockée
 *   dans `avatarUrl` : envoyer un dataURL base64 dépasserait la limite de
 *   500 caractères du champ et ferait échouer la sauvegarde.
 * - Université / faculté sont des champs éditables avec suggestions
 *   (référentiel local, le backend n'expose pas encore /reference/*).
 * - "Niveau d'étude" a été retiré : le modèle `ProfileRequest` ne possède
 *   aucun champ correspondant, la valeur serait perdue silencieusement.
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

function ComboboxField({ label, placeholder, suggestions = [], value, onChange, listId }) {
  return (
    <div>
      <label htmlFor={listId} className="text-sm font-semibold text-slate-700 mb-1.5 block">
        {label}
      </label>
      <input
        id={listId}
        list={listId}
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
      />
      <datalist id={listId}>
        {suggestions.map((opt) => (
          <option key={opt.id ?? opt} value={opt.id ?? opt}>
            {opt.label ?? opt}
          </option>
        ))}
      </datalist>
    </div>
  );
}

export default function OnboardingStep1() {
  const navigate = useNavigate();
  const [birthDate, setBirthDate] = useState("");
  const [university, setUniversity] = useState("");
  const [faculty, setFaculty] = useState("");
  const [photoFile, setPhotoFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const { data: universities } = useAsyncData(() => getUniversities(), []);
  const { data: faculties } = useAsyncData(() => getFaculties(), []);

  // L'aperçu est révoqué au changement / démontage pour ne pas fuiter d'objet URL.
  useEffect(() => {
    if (!photoFile) {
      setPreviewUrl(null);
      return undefined;
    }
    const url = URL.createObjectURL(photoFile);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [photoFile]);

  const handlePhotoChange = (e) => {
    const file = e.target.files?.[0];
    if (file) setPhotoFile(file);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!birthDate || !university.trim() || !faculty.trim()) {
      setSubmitError("Merci de compléter tous les champs obligatoires avant de continuer.");
      return;
    }

    setSubmitting(true);
    try {
      const photo = photoFile ? await uploadAvatar(photoFile) : undefined;
      await updateProfile({
        birthDate,
        university: university.trim(),
        faculty: faculty.trim(),
        ...(photo ? { photo } : {}),
      });
      navigate("/onboarding/2");
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
          <p className="text-sm text-slate-400">Étape 1 sur 3</p>
          <ProgressDots step={1} />
        </div>

        <div className="flex flex-col items-center mb-8">
          <label
            htmlFor="profile-photo"
            className="w-24 h-24 rounded-full bg-violet-50 flex items-center justify-center mb-3 hover:bg-violet-100 transition-colors cursor-pointer overflow-hidden"
          >
            {previewUrl ? (
              <img
                src={previewUrl}
                alt="Aperçu de la photo de profil"
                className="w-full h-full object-cover"
              />
            ) : (
              <Camera className="w-7 h-7 text-violet-500" strokeWidth={1.75} />
            )}
          </label>
          <input
            id="profile-photo"
            type="file"
            accept="image/png, image/jpeg"
            onChange={handlePhotoChange}
            className="hidden"
          />
          <p className="text-sm font-semibold text-violet-600">Ajouter une photo</p>
          <p className="text-xs text-slate-400 mt-0.5">JPG, PNG, max. 5Mo</p>
        </div>

        <form className="space-y-5" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="birth-date" className="text-sm font-semibold text-slate-700 mb-1.5 block">
              Date de naissance *
            </label>
            <div className="relative">
              <input
                id="birth-date"
                type="date"
                value={birthDate}
                onChange={(e) => setBirthDate(e.target.value)}
                className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent"
              />
              <Calendar className="w-4 h-4 text-slate-400 absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>
          </div>

          <ComboboxField
            label="Université *"
            listId="onboarding-universite"
            placeholder="Saisir ou choisir votre université"
            suggestions={universities || []}
            value={university}
            onChange={setUniversity}
          />

          <ComboboxField
            label="Faculté *"
            listId="onboarding-faculte"
            placeholder="Saisir ou choisir votre faculté"
            suggestions={faculties || []}
            value={faculty}
            onChange={setFaculty}
          />

          {submitError && <p className="text-sm text-rose-500">{submitError}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg mt-2 disabled:opacity-60"
          >
            {submitting ? "Enregistrement..." : "Suivant"}
          </button>
        </form>
      </div>
    </div>
  );
}
