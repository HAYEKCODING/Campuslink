import React, { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  GraduationCap,
  MapPin,
  UserPlus,
  MessageCircle,
  Pencil,
} from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getMyProfile, getProfileById, subscribeToProfile } from "./services/profileService";
import { getMatches } from "./services/matchesService";
import { LoadingState, ErrorState, EmptyState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Profil détaillé
 *
 * Deux cas de figure :
 *  - /app/profil/moi  → le profil de l'utilisateur connecté (GET /profiles/me),
 *    affiché en lecture seule avec un raccourci vers l'onboarding ;
 *  - /app/profil/:id  → fiche publique d'un membre (GET /profiles/{id}/public).
 *
 * La messagerie n'est ouverte qu'entre membres liés par un abonnement
 * réciproque : le bouton "Message" n'est donc actif que si ce contact existe
 * (GET /matches), sinon il invite à s'abonner d'abord.
 */

const SELF_IDS = ["moi", "me"];

export default function ProfileDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const isSelf = SELF_IDS.includes(id);
  const [subscribeNotice, setSubscribeNotice] = useState("");
  const [subscribeError, setSubscribeError] = useState("");
  const returnTo = location.state?.from;
  const goBack = () => {
    if (typeof returnTo === "string" && returnTo.startsWith("/") && !returnTo.startsWith("//")) {
      navigate(returnTo);
    } else if (location.key !== "default") {
      navigate(-1);
    } else {
      navigate("/app");
    }
  };

  const { data: profile, loading, error, reload } = useAsyncData(
    () => (isSelf ? getMyProfile() : getProfileById(id)),
    [id, isSelf]
  );
  const { data: matches } = useAsyncData(() => (isSelf ? Promise.resolve([]) : getMatches()), [isSelf]);

  const contact = !isSelf && matches && profile
    ? matches.find((m) => m.autreUtilisateurProfilId === profile.id)
    : null;

  const handleSubscribe = () => {
    setSubscribeNotice("");
    setSubscribeError("");
    subscribeToProfile(profile?.legacyId)
      .then(() => setSubscribeNotice(`Vous êtes maintenant abonné au profil de ${profile?.name || "ce membre"}.`))
      .catch((err) => setSubscribeError(err.message || "Impossible d'enregistrer votre abonnement."));
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
        <LoadingState label="Chargement du profil..." />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
        <ErrorState label="Impossible de charger ce profil." onRetry={reload} />
      </div>
    );
  }

  const { name, university, faculty, city, about, interests = [], photo } = profile;
  const initial = (name || "?").charAt(0);

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm overflow-hidden">
        <div className="relative bg-slate-100">
          {photo ? (
            <img src={photo} alt={`Photo de profil de ${name || "ce membre"}`} className="w-full h-72 object-cover" />
          ) : (
            <div className="w-full h-72 flex items-center justify-center text-6xl font-extrabold text-slate-300">
              {initial}
            </div>
          )}
          <button
            type="button"
            onClick={goBack}
            aria-label="Retour"
            className="absolute top-4 left-4 w-9 h-9 rounded-full bg-white/90 backdrop-blur flex items-center justify-center hover:bg-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4 text-slate-700" />
          </button>
        </div>

        <div className="p-6">
          <div className="mb-2">
            <h1 className="text-xl font-extrabold text-slate-900">
              {name}
            </h1>
            {isSelf && <p className="text-xs text-slate-400 mt-0.5">Votre profil</p>}
          </div>

          {(university || faculty) && (
            <div className="flex items-center gap-2 text-sm text-slate-500 mb-1.5">
              <GraduationCap className="w-4 h-4 text-slate-400 flex-shrink-0" />
              <span>
                {university}
                {university && faculty ? " — " : ""}
                {faculty}
              </span>
            </div>
          )}

          {city && (
            <div className="flex items-center gap-2 text-sm text-slate-500 mb-3">
              <MapPin className="w-4 h-4 text-slate-400 flex-shrink-0" />
              <span>{city}</span>
            </div>
          )}

          {about && (
            <div className="mb-5">
              <h2 className="text-sm font-bold text-slate-900 mb-1.5">À propos de moi</h2>
              <p className="text-sm text-slate-500 leading-relaxed">{about}</p>
            </div>
          )}

          {interests.length > 0 && (
            <div className="mb-5">
              <h2 className="text-sm font-bold text-slate-900 mb-2">Centres d'intérêt</h2>
              <div className="flex flex-wrap gap-1.5">
                {interests.map((interest) => (
                  <span
                    key={interest}
                    className="text-[11px] font-medium text-brand-600 bg-brand-50 px-2.5 py-1 rounded-full"
                  >
                    {interest}
                  </span>
                ))}
              </div>
            </div>
          )}

          {interests.length === 0 && !about && (
            <div className="mb-5">
              <EmptyState label="Ce profil n'a pas encore renseigné de détails." />
            </div>
          )}

          {isSelf ? (
            <button
              onClick={() => navigate("/onboarding/1")}
              className="w-full flex items-center justify-center gap-2 bg-gradient-to-r from-brand-600 to-accent-500 hover:from-brand-700 hover:to-accent-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg"
            >
              <Pencil className="w-4 h-4" />
              Compléter mon profil
            </button>
          ) : (
            <>
              <div className="flex gap-3">
                <button
                  onClick={handleSubscribe}
                  className="flex-1 flex items-center justify-center gap-2 bg-brand-600 hover:bg-brand-700 transition-colors text-white text-sm font-semibold py-3 rounded-lg"
                >
                  <UserPlus className="w-4 h-4" />
                  S'abonner
                </button>
                <button
                  onClick={() => contact && navigate(`/app/messages/${contact.id}`, {
                    state: { from: `${location.pathname}${location.search}${location.hash}` },
                  })}
                  disabled={!contact}
                  title={contact ? undefined : "Abonnez-vous pour ouvrir la discussion"}
                  className="flex-1 flex items-center justify-center gap-2 border border-brand-600 text-brand-600 hover:bg-brand-50 transition-colors text-sm font-semibold py-3 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <MessageCircle className="w-4 h-4" />
                  Message
                </button>
              </div>
              {subscribeNotice && <p role="status" className="text-sm text-emerald-600 text-center mt-3">{subscribeNotice}</p>}
              {subscribeError && <p role="alert" className="text-sm text-rose-500 text-center mt-3">{subscribeError}</p>}
              {!contact && (
                <p className="text-xs text-slate-400 text-center mt-2">
                  Vous pouvez discuter uniquement après un abonnement réciproque.
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
