import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Heart, Check, Menu, X, ShieldCheck, Users, Search, MessageCircle, Send } from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getPlatformStats } from "./services/statsService";
import { getTestimonials } from "./services/testimonialsService";
import { sendContactMessage } from "./services/contactService";
import { LoadingState, EmptyState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Landing page
 * Stack: React + Tailwind
 * Chaque lien du menu pointe vers une vraie section de la page (ancres +
 * défilement fluide). Les témoignages viennent de l'API (getTestimonials) ;
 * le formulaire de contact envoie réellement un message (sendContactMessage).
 */

const NAV_LINKS = [
  { label: "Accueil", href: "#accueil" },
  { label: "À propos", href: "#a-propos" },
  { label: "Fonctionnalités", href: "#fonctionnalites" },
  { label: "Témoignages", href: "#temoignages" },
  { label: "Contact", href: "#contact" },
];

const CHECKLIST = [
  "Rencontrez des étudiants près de chez vous",
  "Trouvez l'amour, des amis ou des partenaires d'étude",
  "Rejoignez une communauté sécurisée et bienveillante",
];

const FEATURES = [
  {
    icon: ShieldCheck,
    title: "Profils vérifiés",
    description: "Chaque étudiant confirme son adresse universitaire avant de rejoindre la communauté.",
  },
  {
    icon: Search,
    title: "Recherche par affinités",
    description: "Filtrez par université, quartier ou centres d'intérêt pour trouver les bonnes personnes.",
  },
  {
    icon: MessageCircle,
    title: "Messagerie intégrée",
    description: "Discutez en toute simplicité avec vos matchs, directement dans l'application.",
  },
  {
    icon: Users,
    title: "Communauté active",
    description: "Rejoignez des milliers d'étudiants déjà inscrits sur votre campus et dans votre quartier.",
  },
];

function NavLink({ href, label, onClick }) {
  return (
    <a
      href={href}
      onClick={onClick}
      className="hover:text-slate-900 transition-colors"
    >
      {label}
    </a>
  );
}

function TestimonialsSection() {
  const { data: testimonials, loading, error, reload } = useAsyncData(() => getTestimonials(), []);

  return (
    <section id="temoignages" className="scroll-mt-24 max-w-6xl mx-auto px-6 md:px-10 py-20">
      <div className="text-center max-w-xl mx-auto mb-12">
        <span className="text-fuchsia-500 text-xs font-bold tracking-wide uppercase">Témoignages</span>
        <h2 className="text-3xl font-extrabold mt-2">Ce que dit la communauté</h2>
      </div>

      {loading && <LoadingState label="Chargement des témoignages..." />}
      {!loading && error && (
        <ErrorState label="Impossible de charger les témoignages." onRetry={reload} />
      )}
      {!loading && !error && (!testimonials || testimonials.length === 0) && (
        <EmptyState label="Pas encore de témoignage à afficher." />
      )}
      {!loading && !error && testimonials && testimonials.length > 0 && (
        <div className="grid md:grid-cols-3 gap-6">
          {testimonials.map((t) => (
            <div key={t.id} className="bg-slate-50 rounded-2xl p-6">
              <p className="text-sm text-slate-600 leading-relaxed mb-4">"{t.quote}"</p>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-slate-200 overflow-hidden flex-shrink-0">
                  {t.photo && <img src={t.photo} alt={t.name} className="w-full h-full object-cover" />}
                </div>
                <div>
                  <p className="text-sm font-bold text-slate-900">{t.name}</p>
                  {t.role && <p className="text-xs text-slate-400">{t.role}</p>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function ContactSection() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [status, setStatus] = useState("idle"); // idle | sending | sent | error

  const handleSubmit = (e) => {
    e.preventDefault();
    setStatus("sending");
    sendContactMessage({ name, email, message })
      .then(() => {
        setStatus("sent");
        setName("");
        setEmail("");
        setMessage("");
      })
      .catch(() => setStatus("error"));
  };

  return (
    <section id="contact" className="scroll-mt-24 bg-slate-50">
      <div className="max-w-3xl mx-auto px-6 md:px-10 py-20">
        <div className="text-center mb-10">
          <span className="text-fuchsia-500 text-xs font-bold tracking-wide uppercase">Contact</span>
          <h2 className="text-3xl font-extrabold mt-2">Une question ? Écrivez-nous</h2>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm p-8 space-y-4">
          <input
            type="text"
            placeholder="Votre nom"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400"
          />
          <input
            type="email"
            placeholder="Votre email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400"
          />
          <textarea
            placeholder="Votre message"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            required
            rows={4}
            className="w-full px-4 py-3 text-sm rounded-lg border border-slate-200 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-400 resize-none"
          />

          {status === "error" && (
            <p className="text-sm text-rose-500">L'envoi a échoué, réessayez dans un instant.</p>
          )}
          {status === "sent" && (
            <p className="text-sm text-emerald-500">Message envoyé, merci ! Nous revenons vers vous rapidement.</p>
          )}

          <button
            type="submit"
            disabled={status === "sending"}
            className="w-full flex items-center justify-center gap-2 bg-gradient-to-r from-violet-600 to-fuchsia-500 hover:from-violet-700 hover:to-fuchsia-600 transition-colors text-white text-sm font-semibold py-3 rounded-lg disabled:opacity-60"
          >
            <Send className="w-4 h-4" />
            {status === "sending" ? "Envoi..." : "Envoyer le message"}
          </button>
        </form>
      </div>
    </section>
  );
}

export default function CampusLinkLanding() {
  const { data: stats } = useAsyncData(() => getPlatformStats(), []);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-white font-sans text-slate-900">
      {/* ---------- HEADER ---------- */}
      <header className="sticky top-0 z-20 bg-white flex items-center justify-between px-6 md:px-10 py-5 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <Heart className="w-6 h-6 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
          <span className="text-lg font-extrabold">
            Campus<span className="text-fuchsia-500">Link</span>
          </span>
        </div>

        <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-slate-600">
          {NAV_LINKS.map((link) => (
            <NavLink key={link.label} {...link} />
          ))}
        </nav>

        <div className="hidden md:flex items-center gap-3">
          <Link
            to="/connexion"
            className="text-sm font-semibold border border-violet-600 text-violet-600 px-5 py-2 rounded-full hover:bg-violet-50 transition-colors"
          >
            Se connecter
          </Link>
          <Link
            to="/inscription"
            className="text-sm font-semibold text-white bg-gradient-to-r from-violet-600 to-fuchsia-500 px-5 py-2 rounded-full hover:from-violet-700 hover:to-fuchsia-600 transition-colors"
          >
            S'inscrire
          </Link>
        </div>

        <button
          onClick={() => setMobileMenuOpen((open) => !open)}
          aria-label={mobileMenuOpen ? "Fermer le menu" : "Ouvrir le menu"}
          className="md:hidden"
        >
          {mobileMenuOpen ? (
            <X className="w-6 h-6 text-slate-700" />
          ) : (
            <Menu className="w-6 h-6 text-slate-700" />
          )}
        </button>
      </header>

      {/* ---------- MOBILE MENU ---------- */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-slate-100 px-6 py-4 flex flex-col gap-4 text-sm font-medium text-slate-600 bg-white">
          {NAV_LINKS.map((link) => (
            <NavLink key={link.label} {...link} onClick={() => setMobileMenuOpen(false)} />
          ))}
          <div className="flex items-center gap-3 pt-2">
            <Link
              to="/connexion"
              className="flex-1 text-center text-sm font-semibold border border-violet-600 text-violet-600 px-5 py-2 rounded-full"
            >
              Se connecter
            </Link>
            <Link
              to="/inscription"
              className="flex-1 text-center text-sm font-semibold text-white bg-gradient-to-r from-violet-600 to-fuchsia-500 px-5 py-2 rounded-full"
            >
              S'inscrire
            </Link>
          </div>
        </div>
      )}

      {/* ---------- HERO / ACCUEIL ---------- */}
      <section id="accueil" className="scroll-mt-24 max-w-7xl mx-auto px-6 md:px-10 py-16 grid md:grid-cols-2 gap-12 items-center">
        <div>
          <h1 className="text-4xl md:text-5xl font-extrabold leading-tight mb-4">
            Bienvenue sur
            <br />
            <span className="bg-gradient-to-r from-slate-900 to-fuchsia-500 bg-clip-text text-transparent">
              CampusLink
            </span>
          </h1>

          <p className="text-slate-500 max-w-md mb-6 leading-relaxed">
            Le site de rencontre des étudiants et de la communauté universitaire.
          </p>

          <ul className="space-y-3 mb-8">
            {CHECKLIST.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm text-slate-700">
                <span className="mt-0.5 w-5 h-5 rounded-full bg-violet-100 flex items-center justify-center flex-shrink-0">
                  <Check className="w-3 h-3 text-violet-600" strokeWidth={3} />
                </span>
                {item}
              </li>
            ))}
          </ul>

          <div className="flex items-center gap-4 mb-8">
            <Link
              to="/inscription"
              className="text-sm font-semibold text-white bg-gradient-to-r from-violet-600 to-fuchsia-500 px-6 py-3 rounded-full hover:from-violet-700 hover:to-fuchsia-600 transition-colors"
            >
              Commencer maintenant
            </Link>
            <a
              href="#a-propos"
              className="text-sm font-semibold text-violet-600 border border-violet-600 px-6 py-3 rounded-full hover:bg-violet-50 transition-colors"
            >
              En savoir plus
            </a>
          </div>

          {stats?.memberCount ? (
            <div className="flex items-center gap-3">
              {stats.avatarPhotos?.length > 0 && (
                <div className="flex -space-x-3">
                  {stats.avatarPhotos.slice(0, 4).map((src) => (
                    <img
                      key={src}
                      src={src}
                      alt=""
                      className="w-9 h-9 rounded-full border-2 border-white object-cover"
                    />
                  ))}
                </div>
              )}
              <div className="text-sm">
                <span className="font-bold text-slate-900">
                  +{stats.memberCount.toLocaleString("fr-FR")} étudiants
                </span>
                <br />
                <span className="text-slate-400">déjà inscrits</span>
              </div>
            </div>
          ) : null}
        </div>

        {/* Boîte de taille fixe : l'image et les cercles décoratifs sont tous
            centrés à l'intérieur de cette même boîte, ce qui évite que
            l'image se retrouve décalée ou coupée selon son format. */}
        <div className="relative w-full max-w-md h-[420px] mx-auto">
          <div className="absolute inset-0 m-auto w-72 h-72 rounded-full bg-violet-100" />
          <div className="absolute top-4 right-4 w-32 h-32 rounded-full bg-fuchsia-50" />

          <img
            src="/hero-students.png"
            alt="Étudiants souriants regardant un téléphone"
            className="absolute inset-0 w-full h-full object-contain"
          />

          <div className="absolute top-6 right-8 w-11 h-11 rounded-full bg-white shadow-md flex items-center justify-center">
            <Heart className="w-5 h-5 text-fuchsia-500 fill-fuchsia-500" strokeWidth={0} />
          </div>
        </div>
      </section>

      {/* ---------- À PROPOS ---------- */}
      <section id="a-propos" className="scroll-mt-24 bg-slate-50">
        <div className="max-w-5xl mx-auto px-6 md:px-10 py-20 grid md:grid-cols-2 gap-12 items-center">
          <div>
            <span className="text-fuchsia-500 text-xs font-bold tracking-wide uppercase">À propos</span>
            <h2 className="text-3xl font-extrabold mt-2 mb-4">Pensé pour les étudiants, par des étudiants</h2>
            <p className="text-slate-500 leading-relaxed mb-4">
              CampusLink est né d'un constat simple : entre les cours, les stages et la vie
              associative, il n'est pas toujours facile de rencontrer de nouvelles personnes
              qui partagent vraiment son quotidien.
            </p>
            <p className="text-slate-500 leading-relaxed">
              Notre plateforme met en relation les étudiants d'un même campus ou d'une même
              ville, pour construire des amitiés, des relations, ou simplement trouver un
              binôme de révision.
            </p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white rounded-2xl p-6 text-center shadow-sm">
              <p className="text-2xl font-extrabold text-fuchsia-500">100%</p>
              <p className="text-xs text-slate-400 mt-1">Profils universitaires</p>
            </div>
            <div className="bg-white rounded-2xl p-6 text-center shadow-sm">
              <p className="text-2xl font-extrabold text-fuchsia-500">Bouaké</p>
              <p className="text-xs text-slate-400 mt-1">Première ville couverte</p>
            </div>
          </div>
        </div>
      </section>

      {/* ---------- FONCTIONNALITÉS ---------- */}
      <section id="fonctionnalites" className="scroll-mt-24 max-w-6xl mx-auto px-6 md:px-10 py-20">
        <div className="text-center max-w-xl mx-auto mb-12">
          <span className="text-fuchsia-500 text-xs font-bold tracking-wide uppercase">Fonctionnalités</span>
          <h2 className="text-3xl font-extrabold mt-2">Tout ce qu'il faut pour bien rencontrer</h2>
        </div>

        <div className="grid sm:grid-cols-2 gap-6">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <div key={title} className="flex gap-4 p-6 rounded-2xl bg-slate-50">
              <div className="w-11 h-11 rounded-xl bg-fuchsia-100 flex items-center justify-center flex-shrink-0">
                <Icon className="w-5 h-5 text-fuchsia-500" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900 mb-1">{title}</h3>
                <p className="text-sm text-slate-500 leading-relaxed">{description}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <TestimonialsSection />
      <ContactSection />
    </div>
  );
}
