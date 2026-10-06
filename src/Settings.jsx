import React from "react";
import { useNavigate } from "react-router-dom";
import {
  User,
  Pencil,
  Lock,
  Shield,
  Bell,
  HelpCircle,
  FileText,
  Info,
  LogOut,
  ChevronRight,
} from "lucide-react";
import Layout from "./Layout";
import { logoutRemote } from "./services/authService";

/**
 * CampusLink — Paramètres
 * Utilise le Layout partagé (sidebar + topbar).
 */

const SETTINGS_ITEMS = [
  { label: "Informations personnelles", icon: User },
  { label: "Modifier le profil", icon: Pencil },
  { label: "Changer le mot de passe", icon: Lock },
  { label: "Confidentialité", icon: Shield },
  { label: "Notifications", icon: Bell },
  { label: "Aide et support", icon: HelpCircle },
  { label: "Conditions d'utilisation", icon: FileText },
  { label: "À propos de CampusLink", icon: Info },
];

function SettingsRow({ label, icon: Icon }) {
  return (
    <button className="w-full flex items-center gap-3 py-3.5 px-1 hover:bg-slate-50 rounded-lg transition-colors text-left">
      <span className="w-9 h-9 rounded-full bg-violet-50 flex items-center justify-center flex-shrink-0">
        <Icon className="w-4 h-4 text-violet-500" />
      </span>
      <span className="flex-1 text-sm font-medium text-slate-700">{label}</span>
      <ChevronRight className="w-4 h-4 text-slate-300" />
    </button>
  );
}

export default function Settings() {
  const navigate = useNavigate();

  const handleLogout = async () => {
    // Révoque le refresh token côté serveur, nettoie les tokens locaux, puis redirige.
    await logoutRemote();
    navigate("/connexion", { replace: true });
  };

  return (
    <Layout active="Paramètres" showSearch={false}>
      <div className="max-w-sm mx-auto bg-white rounded-2xl shadow-sm p-6">
        <h1 className="text-lg font-extrabold mb-4">Paramètres</h1>

        <div className="divide-y divide-slate-50">
          {SETTINGS_ITEMS.map((item) => (
            <SettingsRow key={item.label} {...item} />
          ))}
        </div>

        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 py-3.5 px-1 mt-2 hover:bg-fuchsia-50 rounded-lg transition-colors text-left"
        >
          <span className="w-9 h-9 rounded-full bg-fuchsia-50 flex items-center justify-center flex-shrink-0">
            <LogOut className="w-4 h-4 text-fuchsia-500" />
          </span>
          <span className="flex-1 text-sm font-semibold text-fuchsia-500">Déconnexion</span>
        </button>
      </div>
    </Layout>
  );
}
