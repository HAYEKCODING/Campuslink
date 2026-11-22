import React from "react";
import { Link, useLocation } from "react-router-dom";
import { ChevronRight, UserCheck, UserPlus } from "lucide-react";
import Layout from "./Layout";
import { useAsyncData } from "./hooks/useAsyncData";
import { getDiscoveryProfiles, subscribeToProfile } from "./services/profileService";
import { LoadingState, EmptyState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Fil de découverte
 *
 * Les profils viennent de GET /profiles/search (module Profile du backend).
 * Le même appel alimente la section "Nouveaux membres" : le backend n'a pas de
 * route dédiée, on évite donc une seconde requête identique.
 */

function ProfileCard({
  id, name, university, faculty, interests = [], photo, onSubscribe, subscribed, subscribing, returnTo,
}) {
  return (
    <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
      <Link to={`/app/profil/${id}`} state={{ from: returnTo }} className="block">
        <div className="relative bg-slate-100">
          {photo ? (
            <img src={photo} alt={`Profil de ${name || "ce membre"}`} className="w-full h-44 object-cover" />
          ) : (
            <div className="w-full h-44" />
          )}
        </div>
        <div className="p-4 pb-0">
          <p className="font-bold text-slate-900">
            {name}
          </p>
          {university && <p className="text-xs text-slate-400 mt-0.5">{university}</p>}
          {faculty && <p className="text-xs text-emerald-500 font-medium mt-0.5">{faculty}</p>}
        </div>
      </Link>
      <div className="px-4 pb-4 pt-3 flex items-center justify-between">
        <div className="flex flex-wrap gap-1.5 min-w-0">
          {interests.slice(0, 3).map((tag) => (
            <span
              key={tag}
              className="text-[11px] font-medium text-brand-600 bg-brand-50 px-2.5 py-1 rounded-full"
            >
              {tag}
            </span>
          ))}
        </div>
        <button
          type="button"
          onClick={onSubscribe}
          disabled={subscribed || subscribing}
          aria-label={subscribed ? `Abonné au profil de ${name || "ce membre"}` : `S'abonner au profil de ${name || "ce membre"}`}
          aria-pressed={subscribed}
          title={subscribed ? "Abonné" : subscribing ? "Abonnement..." : "S'abonner"}
          className="w-9 h-9 rounded-full bg-brand-50 text-brand-600 shadow-sm flex items-center justify-center hover:bg-brand-100 transition-colors flex-shrink-0 disabled:opacity-70 disabled:cursor-default"
        >
          {subscribed ? <UserCheck className="w-4 h-4" /> : <UserPlus className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );
}

export default function DiscoveryFeed() {
  const location = useLocation();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;
  const { data: profiles, loading, error, reload } = useAsyncData(() => getDiscoveryProfiles(), []);
  const [subscribeNotice, setSubscribeNotice] = React.useState("");
  const [subscribeError, setSubscribeError] = React.useState("");
  const [subscribedProfiles, setSubscribedProfiles] = React.useState(() => new Set());
  const [subscribingProfiles, setSubscribingProfiles] = React.useState(() => new Set());

  const handleSubscribe = (profile) => {
    if (subscribedProfiles.has(profile.id) || subscribingProfiles.has(profile.id)) return;
    setSubscribeNotice("");
    setSubscribeError("");
    setSubscribingProfiles((current) => new Set(current).add(profile.id));
    subscribeToProfile(profile.legacyId)
      .then(() => {
        setSubscribedProfiles((current) => new Set(current).add(profile.id));
        setSubscribeNotice(`Vous êtes maintenant abonné au profil de ${profile.name || "ce membre"}.`);
      })
      .catch((err) => setSubscribeError(err.message || "Impossible d'enregistrer votre abonnement."))
      .finally(() => {
        setSubscribingProfiles((current) => {
          const next = new Set(current);
          next.delete(profile.id);
          return next;
        });
      });
  };

  const newMembers = profiles ? profiles.slice(0, 8) : null;

  return (
    <Layout active="Accueil">
      <div className="max-w-5xl w-full mx-auto">
        <h1 className="text-2xl font-extrabold mb-1">Bonjour 👋</h1>
        <p className="text-sm text-slate-400 mb-6">
          Voici des étudiants à découvrir pour vos projets et centres d'intérêt
        </p>
        {subscribeNotice && <p role="status" className="text-sm text-emerald-600 mb-4">{subscribeNotice}</p>}
        {subscribeError && <p role="alert" className="text-sm text-rose-500 mb-4">{subscribeError}</p>}

        {loading && <LoadingState label="Chargement des profils..." />}
        {!loading && error && (
          <ErrorState label="Impossible de charger les profils pour le moment." onRetry={reload} />
        )}
        {!loading && !error && (!profiles || profiles.length === 0) && (
          <EmptyState label="Aucun profil à vous proposer pour l'instant." />
        )}
        {!loading && !error && profiles && profiles.length > 0 && (
          <div className="grid md:grid-cols-3 gap-5 mb-8">
            {profiles.map((profile) => (
              <ProfileCard
                key={profile.id}
                {...profile}
                onSubscribe={() => handleSubscribe(profile)}
                subscribed={subscribedProfiles.has(profile.id)}
                subscribing={subscribingProfiles.has(profile.id)}
                returnTo={returnTo}
              />
            ))}
          </div>
        )}

        {newMembers && newMembers.length > 0 && (
          <div>
            <p className="text-sm font-semibold text-slate-700 mb-3">Nouveaux membres</p>
            <div className="flex items-center gap-4 overflow-x-auto pb-2">
              {newMembers.map((member) => (
                <Link
                  key={member.id}
                  to={`/app/profil/${member.id}`}
                  state={{ from: returnTo }}
                  aria-label={member.name || "Voir le profil"}
                >
                  {member.photo ? (
                    <img
                      src={member.photo}
                      alt={member.name || ""}
                      className="w-12 h-12 rounded-full object-cover ring-2 ring-white shadow flex-shrink-0"
                    />
                  ) : (
                    <span className="w-12 h-12 rounded-full bg-slate-200 ring-2 ring-white shadow flex items-center justify-center text-sm font-bold text-slate-500 flex-shrink-0">
                      {(member.name || "?").charAt(0)}
                    </span>
                  )}
                </Link>
              ))}
              <Link
                to="/app/recherche"
                aria-label="Voir plus de nouveaux membres"
                className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center hover:bg-slate-200 transition-colors flex-shrink-0"
              >
                <ChevronRight className="w-4 h-4 text-slate-500" />
              </Link>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
