import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
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
  ChevronLeft,
  Check,
} from "lucide-react";
import Layout from "./Layout";
import { logoutRemote, requestPasswordReset } from "./services/authService";
import { useAsyncData } from "./hooks/useAsyncData";
import { getMyProfile, updateProfile, uploadAvatar } from "./services/profileService";
import {
  getNotifications,
  getUserPreferences,
  markAllNotificationsRead,
  markNotificationRead,
  updateUserPreferences,
} from "./services/settingsService";
import { sendContactMessage } from "./services/contactService";
import { GENDERS, LEVELS } from "./lib/referenceData";
import { NOTIFICATIONS_CHANGED_EVENT } from "./services/settingsService";

const SETTINGS_ITEMS = [
  { id: "personal", label: "Informations personnelles", icon: User },
  { id: "profile", label: "Modifier le profil", icon: Pencil },
  { id: "password", label: "Changer le mot de passe", icon: Lock },
  { id: "privacy", label: "Confidentialité", icon: Shield },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "support", label: "Aide et support", icon: HelpCircle },
  { id: "terms", label: "Conditions d'utilisation", icon: FileText },
  { id: "about", label: "À propos de CampusLink", icon: Info },
];

const INPUT_CLASS =
  "w-full px-4 py-3 text-sm rounded-lg border border-slate-200 text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:border-transparent";

function SettingsRow({ label, icon: Icon, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full flex items-center gap-3 py-3.5 px-1 hover:bg-slate-50 rounded-lg transition-colors text-left"
    >
      <span className="w-9 h-9 rounded-full bg-violet-50 flex items-center justify-center flex-shrink-0">
        <Icon className="w-4 h-4 text-violet-500" />
      </span>
      <span className="flex-1 text-sm font-medium text-slate-700">{label}</span>
      <ChevronRight className="w-4 h-4 text-slate-300" />
    </button>
  );
}

function ToggleRow({ label, description, checked, onChange }) {
  return (
    <label className="flex items-center justify-between gap-4 py-3 border-b border-slate-100">
      <span>
        <span className="block text-sm font-medium text-slate-700">{label}</span>
        {description && <span className="block text-xs text-slate-500 mt-1">{description}</span>}
      </span>
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="w-4 h-4 accent-violet-600"
      />
    </label>
  );
}

function FormMessage({ error, success }) {
  if (error) return <p role="alert" className="text-sm text-rose-500">{error}</p>;
  if (success) return <p role="status" className="text-sm text-emerald-600">{success}</p>;
  return null;
}

function getProfileDraft(profile) {
  return {
    firstName: profile.firstName || "",
    lastName: profile.lastName || "",
    dateOfBirth: profile.dateOfBirth || "",
    gender: profile.gender || "",
    level: profile.level || "",
    university: profile.university || "",
    fieldOfStudy: profile.fieldOfStudy || "",
    city: profile.city || "",
    neighborhood: profile.neighborhood || "",
    bio: profile.bio || "",
    interests: (profile.interests || []).join(", "),
    avatarUrl: profile.avatarUrl || "",
  };
}

export default function Settings() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [profileDraft, setProfileDraft] = useState(null);
  const [avatarFile, setAvatarFile] = useState(null);
  const [preferences, setPreferences] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [sectionLoading, setSectionLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [profileReloadKey, setProfileReloadKey] = useState(0);
  const [passwordEmail, setPasswordEmail] = useState("");
  const [supportForm, setSupportForm] = useState({ name: "", email: "", message: "" });
  const activeSection = searchParams.get("section") || "menu";
  const selectedItem = SETTINGS_ITEMS.find((item) => item.id === activeSection);
  const {
    data: profile,
    loading: profileLoading,
    error: profileError,
  } = useAsyncData(() => getMyProfile(), [profileReloadKey]);

  useEffect(() => {
    if (!profile) return;
    setProfileDraft(getProfileDraft(profile));
    setSupportForm((current) => ({
      ...current,
      name: current.name || profile.name || "",
    }));
  }, [profile]);

  useEffect(() => {
    if (activeSection !== "privacy" && activeSection !== "notifications") return undefined;
    let cancelled = false;
    setSectionLoading(true);
    setError("");
    const load = activeSection === "notifications"
      ? Promise.all([getUserPreferences(), getNotifications()])
      : getUserPreferences().then((value) => [value, null]);
    load
      .then(([nextPreferences, nextNotifications]) => {
        if (cancelled) return;
        setPreferences(nextPreferences);
        if (nextNotifications) {
          setNotifications((current) => {
            const byId = new Map(
              [...nextNotifications, ...current].map((notification) => [
                String(notification.id),
                notification,
              ])
            );
            return [...byId.values()].sort(
              (left, right) => new Date(right.dateCreation) - new Date(left.dateCreation)
            );
          });
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Impossible de charger ces paramètres.");
      })
      .finally(() => {
        if (!cancelled) setSectionLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeSection]);

  useEffect(() => {
    if (activeSection !== "notifications") return undefined;
    const onNotification = (event) => {
      const incoming = event.detail;
      if (incoming?.id == null) return;
      setNotifications((current) => {
        const byId = new Map(
          [[String(incoming.id), incoming], ...current.map((notification) => [
            String(notification.id),
            notification,
          ])]
        );
        return [...byId.values()].sort(
          (left, right) => new Date(right.dateCreation) - new Date(left.dateCreation)
        );
      });
    };
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, onNotification);
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, onNotification);
  }, [activeSection]);

  const openSection = (id) => {
    setError("");
    setSuccess("");
    setSearchParams({ section: id });
  };

  const goBack = () => {
    setError("");
    setSuccess("");
    setSearchParams({});
  };

  const handleLogout = async () => {
    await logoutRemote();
    navigate("/connexion", { replace: true });
  };

  const handleProfileSave = async (event) => {
    event.preventDefault();
    if (!profileDraft) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const photo = avatarFile ? await uploadAvatar(avatarFile) : undefined;
      await updateProfile({
        ...profileDraft,
        dateOfBirth: profileDraft.dateOfBirth || undefined,
        gender: profileDraft.gender || undefined,
        level: profileDraft.level || undefined,
        photo,
        interests: profileDraft.interests
          .split(",")
          .map((interest) => interest.trim())
          .filter(Boolean),
      });
      setAvatarFile(null);
      setSuccess("Votre profil a été enregistré.");
      setProfileReloadKey((key) => key + 1);
    } catch (err) {
      setError(err.message || "L'enregistrement du profil a échoué.");
    } finally {
      setSaving(false);
    }
  };

  const savePreferences = async () => {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setPreferences(await updateUserPreferences(preferences));
      setSuccess("Vos préférences ont été enregistrées.");
    } catch (err) {
      setError(err.message || "L'enregistrement des préférences a échoué.");
    } finally {
      setSaving(false);
    }
  };

  const updatePreference = (key, value) => {
    setPreferences((current) => ({ ...current, [key]: value }));
  };

  const handlePasswordReset = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      await requestPasswordReset(passwordEmail.trim());
      setSuccess("Si cette adresse correspond à un compte, un lien de réinitialisation a été envoyé.");
    } catch (err) {
      setError(err.message || "La demande de réinitialisation a échoué.");
    } finally {
      setSaving(false);
    }
  };

  const handleSupportSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      await sendContactMessage(supportForm);
      setSuccess("Votre message a été envoyé.");
      setSupportForm((current) => ({ ...current, message: "" }));
    } catch (err) {
      setError(err.message || "L'envoi du message a échoué.");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await markNotificationRead(id);
      setNotifications((current) =>
        current.map((notification) =>
          notification.id === id ? { ...notification, lu: true } : notification
        )
      );
    } catch (err) {
      setError(err.message || "Impossible de marquer cette notification comme lue.");
    }
  };

  const handleOpenNotification = async (notification) => {
    if (!notification.lu) {
      try {
        await markNotificationRead(notification.id);
        setNotifications((current) =>
          current.map((item) =>
            item.id === notification.id ? { ...item, lu: true } : item
          )
        );
      } catch (err) {
        setError(err.message || "Impossible de marquer cette notification comme lue.");
        return;
      }
    }
    if (notification.navigationUrl?.startsWith("/app/")) {
      navigate(notification.navigationUrl);
    }
  };

  const handleMarkAllRead = async () => {
    setSaving(true);
    setError("");
    try {
      await markAllNotificationsRead();
      setNotifications((current) => current.map((notification) => ({ ...notification, lu: true })));
    } catch (err) {
      setError(err.message || "Impossible de marquer les notifications comme lues.");
    } finally {
      setSaving(false);
    }
  };

  const saveButton = (onClick, label = "Enregistrer") => (
    <button
      type="button"
      onClick={onClick}
      disabled={saving || sectionLoading}
      className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
    >
      {saving ? "Enregistrement..." : label}
    </button>
  );

  const renderSection = () => {
    if (activeSection === "personal") {
      if (profileLoading) return <p className="text-sm text-slate-500">Chargement du profil...</p>;
      if (profileError) return <p role="alert" className="text-sm text-rose-500">{profileError}</p>;
      if (!profile) return <p className="text-sm text-slate-500">Aucune information de profil disponible.</p>;
      const fields = [
        ["Nom", profile.name],
        ["Date de naissance", profile.dateOfBirth],
        ["Genre", GENDERS.find((item) => item.value === profile.gender)?.label || profile.gender],
        ["Niveau d'étude", LEVELS.find((item) => item.value === profile.level)?.label || profile.level],
        ["Université", profile.university],
        ["Filière", profile.fieldOfStudy],
        ["Ville", profile.city],
        ["Quartier", profile.neighborhood],
      ];
      return (
        <div className="space-y-3">
          {fields.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 border-b border-slate-100 py-2">
              <span className="text-sm text-slate-500">{label}</span>
              <span className="text-sm font-medium text-slate-700 text-right">{value || "Non renseigné"}</span>
            </div>
          ))}
          <button
            type="button"
            onClick={() => openSection("profile")}
            className="text-sm font-semibold text-violet-600 hover:underline"
          >
            Modifier ces informations
          </button>
        </div>
      );
    }

    if (activeSection === "profile") {
      if (profileLoading || !profileDraft) return <p className="text-sm text-slate-500">Chargement du profil...</p>;
      if (profileError) return <p role="alert" className="text-sm text-rose-500">{profileError}</p>;
      const textFields = [
        ["firstName", "Prénom", "text"],
        ["lastName", "Nom", "text"],
        ["dateOfBirth", "Date de naissance", "date"],
        ["university", "Université", "text"],
        ["fieldOfStudy", "Filière", "text"],
        ["city", "Ville", "text"],
        ["neighborhood", "Quartier", "text"],
      ];
      return (
        <form className="space-y-4" onSubmit={handleProfileSave}>
          {textFields.map(([key, label, type]) => (
            <label key={key} className="block text-sm font-semibold text-slate-700">
              {label}
              <input
                type={type}
                value={profileDraft[key]}
                onChange={(event) => setProfileDraft((current) => ({ ...current, [key]: event.target.value }))}
                required={key === "firstName" || key === "lastName"}
                className={`${INPUT_CLASS} mt-1.5 font-normal`}
              />
            </label>
          ))}
          <label className="block text-sm font-semibold text-slate-700">
            Genre
            <select
              value={profileDraft.gender}
              onChange={(event) => setProfileDraft((current) => ({ ...current, gender: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            >
              {!profileDraft.gender && <option value="" disabled>Non renseigné</option>}
              {profileDraft.gender && !GENDERS.some((item) => item.value === profileDraft.gender) && (
                <option value={profileDraft.gender}>{profileDraft.gender}</option>
              )}
              {GENDERS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Niveau d'étude
            <select
              value={profileDraft.level}
              onChange={(event) => setProfileDraft((current) => ({ ...current, level: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            >
              {!profileDraft.level && <option value="" disabled>Non renseigné</option>}
              {LEVELS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            À propos de moi
            <textarea
              rows={4}
              maxLength={1000}
              value={profileDraft.bio}
              onChange={(event) => setProfileDraft((current) => ({ ...current, bio: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal resize-y`}
            />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Centres d'intérêt <span className="font-normal text-slate-500">(séparés par des virgules)</span>
            <input
              type="text"
              value={profileDraft.interests}
              onChange={(event) => setProfileDraft((current) => ({ ...current, interests: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Photo de profil
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => setAvatarFile(event.target.files?.[0] || null)}
              className="block w-full mt-1.5 text-sm text-slate-600"
            />
            {profileDraft.avatarUrl && <span className="block text-xs font-normal text-slate-500 mt-1">Une photo existe déjà ; choisissez-en une nouvelle pour la remplacer.</span>}
          </label>
          <FormMessage error={error} success={success} />
          <button
            type="submit"
            disabled={saving}
            className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            {saving ? "Enregistrement..." : "Enregistrer le profil"}
          </button>
        </form>
      );
    }

    if (activeSection === "password") {
      return (
        <form className="space-y-4" onSubmit={handlePasswordReset}>
          <p className="text-sm text-slate-600">
            Pour sécuriser votre compte, nous envoyons un lien de réinitialisation à l'adresse associée à votre compte.
          </p>
          <label className="block text-sm font-semibold text-slate-700">
            Adresse e-mail du compte
            <input
              type="email"
              required
              autoComplete="email"
              value={passwordEmail}
              onChange={(event) => setPasswordEmail(event.target.value)}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            />
          </label>
          <FormMessage error={error} success={success} />
          {saveButton(handlePasswordReset, "Envoyer le lien")}
        </form>
      );
    }

    if (activeSection === "privacy") {
      if (sectionLoading) return <p className="text-sm text-slate-500">Chargement des préférences...</p>;
      if (!preferences) return <FormMessage error={error || "Préférences indisponibles."} />;
      return (
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Un profil masqué n'apparaît plus dans la recherche et ne peut plus être consulté via son lien public.
          </p>
          <ToggleRow
            label="Rendre mon profil visible"
            description="Autoriser les autres membres à vous découvrir."
            checked={preferences.profileVisible}
            onChange={(value) => updatePreference("profileVisible", value)}
          />
          <FormMessage error={error} success={success} />
          {saveButton(savePreferences)}
        </div>
      );
    }

    if (activeSection === "notifications") {
      if (sectionLoading) return <p className="text-sm text-slate-500">Chargement des notifications...</p>;
      if (!preferences) return <FormMessage error={error || "Notifications indisponibles."} />;
      const notificationTypes = [
        ["notificationLikes", "J'aime"],
        ["notificationMatches", "Matchs"],
        ["notificationMessages", "Messages"],
        ["notificationReports", "Signalements"],
      ];
      return (
        <div className="space-y-5">
          <div>
            <h2 className="text-sm font-bold text-slate-800 mb-1">Préférences d'alertes</h2>
            {notificationTypes.map(([key, label]) => (
              <ToggleRow
                key={key}
                label={label}
                  description="Désactiver l'alerte temps réel ; la notification reste disponible dans le centre."
                  checked={preferences[key]}
                  onChange={(value) => updatePreference(key, value)}
              />
            ))}
            <div className="mt-4">
              <FormMessage error={error} success={success} />
              {saveButton(savePreferences)}
            </div>
          </div>
          <div className="border-t border-slate-100 pt-4">
            <div className="flex items-center justify-between gap-3 mb-3">
              <h2 className="text-sm font-bold text-slate-800">Centre de notifications</h2>
              <button
                type="button"
                onClick={handleMarkAllRead}
                disabled={saving || notifications.every((notification) => notification.lu)}
                className="text-xs font-semibold text-violet-600 hover:underline disabled:text-slate-400"
              >
                Tout marquer comme lu
              </button>
            </div>
            {notifications.length === 0 ? (
              <p className="text-sm text-slate-500">Aucune notification pour le moment.</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {notifications.map((notification) => (
                  <li key={notification.id} className="flex items-start gap-3 py-3">
                    <span className={`mt-1 w-2 h-2 rounded-full ${notification.lu ? "bg-slate-200" : "bg-violet-500"}`} />
                    <div className="flex-1">
                      {notification.navigationUrl?.startsWith("/app/") ? (
                        <button
                          type="button"
                          onClick={() => handleOpenNotification(notification)}
                          className="text-left text-sm text-slate-700 hover:text-violet-700"
                        >
                          {notification.contenu}
                        </button>
                      ) : (
                        <p className="text-sm text-slate-700">{notification.contenu}</p>
                      )}
                      {notification.dateCreation && (
                        <time className="text-xs text-slate-400">
                          {new Date(notification.dateCreation).toLocaleString()}
                        </time>
                      )}
                    </div>
                    {!notification.lu && (
                      <button
                        type="button"
                        onClick={() => handleMarkRead(notification.id)}
                        aria-label="Marquer comme lu"
                        className="text-violet-600 hover:text-violet-800"
                      >
                        <Check className="w-4 h-4" />
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      );
    }

    if (activeSection === "support") {
      return (
        <form className="space-y-4" onSubmit={handleSupportSubmit}>
          <p className="text-sm text-slate-600">Décrivez votre demande ; elle sera transmise à l'équipe CampusLink.</p>
          <label className="block text-sm font-semibold text-slate-700">
            Nom
            <input
              required
              value={supportForm.name}
              onChange={(event) => setSupportForm((current) => ({ ...current, name: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            E-mail
            <input
              type="email"
              required
              value={supportForm.email}
              onChange={(event) => setSupportForm((current) => ({ ...current, email: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal`}
            />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Message
            <textarea
              required
              maxLength={2000}
              rows={5}
              value={supportForm.message}
              onChange={(event) => setSupportForm((current) => ({ ...current, message: event.target.value }))}
              className={`${INPUT_CLASS} mt-1.5 font-normal resize-y`}
            />
          </label>
          <FormMessage error={error} success={success} />
          <button
            type="submit"
            disabled={saving}
            className="w-full bg-gradient-to-r from-violet-600 to-fuchsia-500 text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            {saving ? "Envoi..." : "Envoyer au support"}
          </button>
        </form>
      );
    }

    if (activeSection === "terms") {
      return (
        <div className="space-y-3 text-sm text-slate-600">
          <p>Les conditions d'utilisation détaillées ne sont pas encore publiées dans cette version de CampusLink.</p>
          <p>Pour toute question sur l'utilisation du service ou vos données, contactez l'équipe depuis « Aide et support ».</p>
          <button type="button" onClick={() => openSection("support")} className="font-semibold text-violet-600 hover:underline">
            Contacter le support
          </button>
        </div>
      );
    }

    if (activeSection === "about") {
      return (
        <div className="space-y-3 text-sm text-slate-600">
          <p className="text-base font-bold text-slate-800">CampusLink</p>
          <p>Une plateforme pour rencontrer des étudiants, découvrir des profils selon les affinités et échanger avec ses matchs.</p>
          <p>Version web de CampusLink.</p>
        </div>
      );
    }

    return null;
  };

  return (
    <Layout active="Paramètres" showSearch={false}>
      <div className="max-w-lg mx-auto bg-white rounded-2xl shadow-sm p-6">
        {selectedItem ? (
          <>
            <button
              type="button"
              onClick={goBack}
              className="inline-flex items-center gap-1 text-sm font-semibold text-violet-600 mb-4 hover:underline"
            >
              <ChevronLeft className="w-4 h-4" />
              Paramètres
            </button>
            <h1 className="text-lg font-extrabold mb-4">{selectedItem.label}</h1>
            {renderSection()}
          </>
        ) : (
          <>
            <h1 className="text-lg font-extrabold mb-4">Paramètres</h1>
            <div className="divide-y divide-slate-50">
              {SETTINGS_ITEMS.map((item) => (
                <SettingsRow key={item.id} {...item} onClick={() => openSection(item.id)} />
              ))}
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="w-full flex items-center gap-3 py-3.5 px-1 mt-2 hover:bg-fuchsia-50 rounded-lg transition-colors text-left"
            >
              <span className="w-9 h-9 rounded-full bg-fuchsia-50 flex items-center justify-center flex-shrink-0">
                <LogOut className="w-4 h-4 text-fuchsia-500" />
              </span>
              <span className="flex-1 text-sm font-semibold text-fuchsia-500">Déconnexion</span>
            </button>
          </>
        )}
      </div>
    </Layout>
  );
}
