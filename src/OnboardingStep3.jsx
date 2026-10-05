import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, X } from "lucide-react";
import { uploadProfilePhotos } from "./services/profileService";

/**
 * CampusLink — Complétez votre profil (Étape 3/3)
 *
 * Les photos sont envoyées via POST /media/upload (multipart, champ `file`)
 * au moment de cliquer sur "Terminer", puis leurs URLs sont disponibles côté
 * profil. Les aperçus (object URLs) sont révoqués dès qu'ils ne sont plus
 * nécessaires pour ne pas fuiter de mémoire.
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

const MAX_PHOTOS = 6;

export default function OnboardingStep3() {
  const navigate = useNavigate();
  const [files, setFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  // Création / libération des URLs d'aperçu en fonction des fichiers sélectionnés.
  useEffect(() => {
    const urls = files.map((file) => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [files]);

  const addPhotos = (e) => {
    const selected = Array.from(e.target.files || []);
    setFiles((prev) => [...prev, ...selected].slice(0, MAX_PHOTOS));
    e.target.value = "";
  };

  const removePhoto = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleFinish = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      if (files.length > 0) {
        await uploadProfilePhotos(files);
      }
      navigate("/app");
    } catch (err) {
      setSubmitError(err.message || "L'envoi des photos a échoué.");
    } finally {
      setSubmitting(false);
    }
  };

  const emptySlots = MAX_PHOTOS - 1 - files.length;

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-8">
        <h1 className="text-xl font-extrabold mb-1">Complétez votre profil</h1>
        <div className="flex items-center justify-between mb-6">
          <p className="text-sm text-slate-400">Étape 3 sur 3</p>
          <ProgressDots step={3} />
        </div>

        <p className="text-sm font-semibold text-slate-700 mb-3">
          Ajouter jusqu'à {MAX_PHOTOS} photos
        </p>

        <div className="grid grid-cols-3 gap-3 mb-8">
          <label
            className={`aspect-square rounded-xl border-2 border-dashed border-violet-200 bg-violet-50 flex flex-col items-center justify-center gap-1 hover:bg-violet-100 transition-colors ${
              files.length >= MAX_PHOTOS ? "opacity-50 pointer-events-none" : "cursor-pointer"
            }`}
          >
            <Plus className="w-5 h-5 text-violet-500" />
            <span className="text-xs font-semibold text-violet-600">Ajouter</span>
            <input
              type="file"
              accept="image/png, image/jpeg"
              multiple
              onChange={addPhotos}
              className="hidden"
              aria-label="Ajouter des photos"
            />
          </label>

          {files.map((file, i) => (
            <div key={`${file.name}-${i}`} className="relative aspect-square rounded-xl overflow-hidden group">
              {previews[i] && (
                <img
                  src={previews[i]}
                  alt={`Photo de profil ${i + 1}`}
                  className="w-full h-full object-cover"
                />
              )}
              <button
                type="button"
                onClick={() => removePhoto(i)}
                className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-black/50 flex items-center justify-center text-white opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity"
                aria-label="Retirer la photo"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}

          {Array.from({ length: Math.max(emptySlots, 0) }).map((_, i) => (
            <div
              key={`empty-${i}`}
              className="aspect-square rounded-xl border-2 border-dashed border-slate-200 bg-slate-50"
            />
          ))}
        </div>

        {submitError && <p className="text-sm text-rose-500 mb-4">{submitError}</p>}

        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => navigate("/onboarding/2")}
            className="flex-1 border border-slate-200 text-slate-600 text-sm font-semibold py-3 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Retour
          </button>
          <button
            type="button"
            onClick={handleFinish}
            disabled={submitting}
            className="flex-1 bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            {submitting ? "Envoi..." : "Terminer"}
          </button>
        </div>
      </div>
    </div>
  );
}
