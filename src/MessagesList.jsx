import React from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import Layout from "./Layout";
import { useAsyncData } from "./hooks/useAsyncData";
import { getMatches } from "./services/matchesService";
import { LoadingState, EmptyState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Liste des conversations
 *
 * Le backend n'a pas d'entité "conversation" : une conversation est un match
 * actif (GET /matches), dont l'API renvoie l'identité de l'autre membre.
 * L'absence de last message / compteur non lu reflète l'API actuelle — pas de
 * données inventées.
 */

function formatDate(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" });
}

export default function MessagesList() {
  const navigate = useNavigate();
  const { data: matches, loading, error, reload } = useAsyncData(() => getMatches(), []);

  return (
    <Layout active="Messages" showSearch={false}>
      <div className="max-w-sm mx-auto bg-white rounded-2xl shadow-sm p-5">
        <h1 className="text-lg font-extrabold mb-4">Messages</h1>

        <div className="relative mb-4">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Rechercher"
            aria-label="Rechercher une conversation"
            disabled
            className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg bg-slate-50 border border-transparent focus:outline-none focus:ring-2 focus:ring-violet-400 disabled:opacity-60"
          />
        </div>

        {loading && <LoadingState label="Chargement de vos conversations..." />}
        {!loading && error && (
          <ErrorState label="Impossible de charger vos messages." onRetry={reload} />
        )}
        {!loading && !error && (!matches || matches.length === 0) && (
          <EmptyState label="Aucune conversation pour l'instant. Likez un profil pour démarrer !" />
        )}

        {!loading && !error && matches && matches.length > 0 && (
          <div className="flex flex-col gap-0.5">
            {matches.map((match) => {
              const name = match.autreUtilisateurNom || "Membre CampusLink";
              return (
                <button
                  key={match.id}
                  onClick={() => navigate(`/app/messages/${match.id}`)}
                  className="w-full flex items-center gap-3 p-2.5 rounded-xl text-left transition-colors hover:bg-slate-50"
                >
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex-shrink-0 overflow-hidden flex items-center justify-center text-sm font-bold text-slate-400">
                    {match.autreUtilisateurPhoto ? (
                      <img src={match.autreUtilisateurPhoto} alt="" className="w-full h-full object-cover" />
                    ) : (
                      name.charAt(0)
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-bold text-slate-900 truncate">{name}</p>
                      <span className="text-xs text-slate-400 flex-shrink-0">
                        {formatDate(match.dateMatch)}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 truncate">
                      {match.statut === "ACTIF" ? "Conversation active" : "Conversation rompue"}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>
    </Layout>
  );
}
