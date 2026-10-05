import React from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import { useAsyncData } from "./hooks/useAsyncData";
import { getMatches } from "./services/matchesService";
import { LoadingState, EmptyState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Mes matchs
 * GET /matches renvoie l'identité de l'autre membre (nom, photo, UUID du
 * profil) : on l'utilise pour afficher la carte et ouvrir la conversation.
 */

function MatchCard({ name, photo, onClick }) {
  const label = name || "Membre";
  return (
    <button onClick={onClick} className="flex flex-col items-center gap-2 group" aria-label={label}>
      <div className="relative">
        <div className="w-16 h-16 rounded-full bg-slate-100 overflow-hidden ring-2 ring-transparent group-hover:ring-violet-400 transition-all flex items-center justify-center text-lg font-bold text-slate-400">
          {photo ? (
            <img src={photo} alt="" className="w-full h-full object-cover" />
          ) : (
            label.charAt(0)
          )}
        </div>
      </div>
      <div className="text-center">
        <p className="text-sm font-semibold text-slate-900 max-w-[6rem] truncate">{label}</p>
        <p className="text-xs text-slate-400">Ouvrir la discussion</p>
      </div>
    </button>
  );
}

export default function MyMatches() {
  const navigate = useNavigate();
  const { data: matches, loading, error, reload } = useAsyncData(() => getMatches(), []);

  const openMatch = (match) => {
    // Priorité : ouvrir la discussion ; sinon retomber sur la fiche profil.
    if (match.id != null) navigate(`/app/messages/${match.id}`);
    else if (match.autreUtilisateurProfilId) navigate(`/app/profil/${match.autreUtilisateurProfilId}`);
  };

  return (
    <Layout active="Mes matchs">
      <div className="max-w-2xl mx-auto bg-white rounded-2xl shadow-sm p-6">
        <h1 className="text-lg font-extrabold mb-6">Mes matchs</h1>

        {loading && <LoadingState label="Chargement de vos matchs..." />}
        {!loading && error && (
          <ErrorState label="Impossible de charger vos matchs." onRetry={reload} />
        )}
        {!loading && !error && (!matches || matches.length === 0) && (
          <EmptyState label="Pas encore de match. Continuez à explorer !" />
        )}
        {!loading && !error && matches && matches.length > 0 && (
          <div className="grid grid-cols-3 sm:grid-cols-6 gap-6">
            {matches.map((match) => (
              <MatchCard
                key={match.id}
                name={match.autreUtilisateurNom}
                photo={match.autreUtilisateurPhoto}
                onClick={() => openMatch(match)}
              />
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}
