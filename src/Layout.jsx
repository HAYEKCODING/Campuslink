import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { Heart, Search, Bell, MessageCircle, Home, User, Settings as SettingsIcon } from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getCurrentUser } from "./services/profileService";

/**
 * CampusLink — Layout partagé (sidebar + topbar + nav mobile)
 * Utilisé par tous les écrans de l'app une fois connecté :
 * Découverte, Recherche, Messages, Mes matchs, Profil, Paramètres.
 *
 * L'utilisateur courant et ses compteurs de badges (messages non lus,
 * notifications) sont chargés depuis l'API via getCurrentUser().
 * Tant que le backend n'est pas prêt, ces valeurs restent vides/nulles
 * et l'UI se contente d'afficher un état neutre (pas de fausses données).
 *
 * Props:
 * - active: le label du lien de nav actif ("Accueil", "Recherche", ...)
 * - showSearch: affiche ou non la barre de recherche du topbar (true par défaut)
 * - children: le contenu de la page
 *
 * TODO(backend): envisager un contexte global (AuthContext) pour éviter
 * de refaire cet appel à chaque changement d'écran.
 */

const DEFAULT_NAV = [
  { label: "Accueil", icon: Home, to: "/app" },
  { label: "Recherche", icon: Search, to: "/app/recherche" },
  { label: "Messages", icon: MessageCircle, to: "/app/messages", badgeKey: "unreadMessages" },
  { label: "Notifications", icon: Bell, to: "/app", badgeKey: "unreadNotifications" },
  { label: "Mes matchs", icon: Heart, to: "/app/matchs" },
  { label: "Profil", icon: User, to: "/app/profil/moi" },
  { label: "Paramètres", icon: SettingsIcon, to: "/app/parametres" },
];

const MOBILE_NAV = [
  { label: "Accueil", icon: Home, to: "/app" },
  { label: "Recherche", icon: Search, to: "/app/recherche" },
  { label: "Mes matchs", icon: Heart, to: "/app/matchs" },
  { label: "Messages", icon: MessageCircle, to: "/app/messages" },
  { label: "Profil", icon: User, to: "/app/profil/moi" },
];

export default function Layout({ active = "Accueil", showSearch = true, children }) {
  const navigate = useNavigate();
  const { data: user } = useAsyncData(() => getCurrentUser(), []);

  const handleTopbarSearch = (e) => {
    e.preventDefault();
    // La recherche s'effectue sur la page dédiée (filtres structurés).
    navigate("/app/recherche");
  };

  return (
    <div className="min-h-screen bg-slate-50 font-sans flex">
      {/* ---------- SIDEBAR ---------- */}
      <aside className="hidden md:flex flex-col w-56 bg-white border-r border-slate-100 px-4 py-6">
        <div className="flex items-center gap-2 px-2 mb-8">
          <Heart className="w-6 h-6 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
          <span className="font-extrabold">
            Campus<span className="text-fuchsia-500">Link</span>
          </span>
        </div>

        <nav className="flex flex-col gap-1">
          {DEFAULT_NAV.map(({ label, icon: Icon, to, badgeKey }) => {
            const isActive = active === label;
            const badgeValue = badgeKey ? user?.[badgeKey] : null;
            return (
              <Link
                key={label}
                to={to}
                className={`flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? "bg-violet-600 text-white" : "text-slate-500 hover:bg-slate-50"
                }`}
              >
                <span className="flex items-center gap-3">
                  <Icon className="w-4 h-4" />
                  {label}
                </span>
                {badgeValue ? (
                  <span
                    className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                      isActive ? "bg-white/20 text-white" : "bg-fuchsia-500 text-white"
                    }`}
                  >
                    {badgeValue}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* ---------- MAIN ---------- */}
      <div className="flex-1 flex flex-col pb-16 md:pb-0">
        <header className="flex items-center gap-4 px-6 py-4 bg-white border-b border-slate-100">
          {showSearch ? (
            <form onSubmit={handleTopbarSearch} className="flex-1 relative max-w-md">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                placeholder="Rechercher"
                aria-label="Rechercher un membre"
                className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg bg-slate-50 border border-transparent focus:outline-none focus:ring-2 focus:ring-violet-400"
              />
            </form>
          ) : (
            <div className="flex-1" />
          )}
          <button className="relative w-9 h-9 rounded-full hover:bg-slate-50 flex items-center justify-center">
            <Bell className="w-5 h-5 text-slate-500" />
            {user?.unreadNotifications ? (
              <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-fuchsia-500" />
            ) : null}
          </button>
          <Link to="/app/profil/moi">
            {user?.photo ? (
              <img
                src={user.photo}
                alt={`Photo de profil de ${user.name || "l'utilisateur"}`}
                className="w-9 h-9 rounded-full object-cover"
              />
            ) : (
              <span className="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center">
                <User className="w-4 h-4 text-slate-400" />
              </span>
            )}
          </Link>
        </header>

        <main className="flex-1 p-6">{children}</main>
      </div>

      {/* ---------- MOBILE BOTTOM NAV ---------- */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-slate-100 flex items-center justify-around py-2 z-10">
        {MOBILE_NAV.map(({ label, icon: Icon, to }) => {
          const isActive = active === label;
          return (
            <Link
              key={label}
              to={to}
              aria-label={label}
              className={`flex flex-col items-center justify-center w-12 h-12 rounded-full ${
                isActive ? "text-violet-600" : "text-slate-400"
              }`}
            >
              <Icon className="w-5 h-5" strokeWidth={isActive ? 2.25 : 1.75} />
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
