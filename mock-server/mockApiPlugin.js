/**
 * Mock API pour le développement local, tant que le vrai backend n'est pas prêt.
 *
 * Ce plugin Vite intercepte toutes les requêtes /api/* du serveur de dev
 * (npm run dev) et répond avec des données réalistes, conformes au contrat
 * décrit dans API_CONTRACT.md. Il ne s'exécute qu'en dev (configureServer
 * n'est jamais appelé lors de `npm run build`), donc il n'a aucun impact
 * sur la production.
 *
 * Aucune photo fixe n'est renvoyée ici (photo: null partout) : l'UI affiche
 * un avatar neutre tant qu'un vrai utilisateur n'a pas uploadé sa photo.
 *
 * -> Pour désactiver le mock et pointer vers le vrai backend : retire
 *    `mockApiPlugin()` de vite.config.js et règle VITE_API_URL dans un
 *    fichier .env vers l'URL réelle de l'API.
 */

function slugify(label) {
  return label
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "") // retire les accents
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

const UNIVERSITIES = [
  { id: "uao", label: "Université Alassane Ouattara" },
  { id: "uao2", label: "Université Alassane Ouattara - Campus 2" },
  { id: "atlantique", label: "Université de l'Atlantique" },
  { id: "ick", label: "Université Internationale ICK" },
  { id: "hetec", label: "HETEC Bouaké" },
  { id: "essect", label: "ESSECT Poincaré" },
  { id: "esc", label: "ESC Bouaké" },
  { id: "ipf", label: "IPF Bouaké" },
  { id: "epct", label: "EPCT Bouaké" },
  { id: "ies", label: "IES Le Campus" },
  { id: "esumag", label: "ESUMAG" },
  { id: "edhec", label: "EDHEC Bouaké" },
];

const FACULTIES = [
  "Droit",
  "Droit des Affaires",
  "Droit Public",
  "Droit Privé",
  "Sciences Juridiques",
  "Sciences Politiques",
  "Économie",
  "Économie et Gestion",
  "Gestion",
  "Gestion Commerciale",
  "Gestion des Ressources Humaines",
  "Comptabilité",
  "Finance",
  "Banque et Finance",
  "Audit et Contrôle de Gestion",
  "Marketing",
  "Marketing Digital",
  "Commerce International",
  "Logistique et Transport",
  "Management",
  "Entrepreneuriat",
  "Informatique",
  "Génie Logiciel",
  "Réseaux et Télécommunications",
  "Cybersécurité",
  "Intelligence Artificielle",
  "Science des Données",
  "Mathématiques",
  "Statistiques",
  "Communication",
  "Communication Digitale",
  "Journalisme",
  "Lettres Modernes",
  "Anglais",
  "Langues Étrangères",
  "Sciences Sociales",
  "Sociologie",
  "Psychologie",
  "Criminologie",
  "Relations Internationales",
  "Administration Publique",
  "Sciences de l'Éducation",
  "Sciences de la Santé",
  "Infirmier(ère)",
  "Sage-femme",
  "Biologie",
  "Biochimie",
  "Microbiologie",
  "Génie Civil",
  "Bâtiment et Travaux Publics (BTP)",
  "Génie Électrique",
  "Génie Industriel",
  "Génie Mécanique",
  "Architecture",
  "Agronomie",
  "Agroalimentaire",
  "Qualité, Hygiène, Sécurité et Environnement (QHSE)",
  "Tourisme et Hôtellerie",
  "Restauration",
  "Assurance",
  "Fiscalité",
].map((label) => ({ id: slugify(label), label }));

const CITIES = [{ id: "bouake", label: "Bouaké" }];

const NEIGHBORHOODS = [
  "Cité CIDT",
  "Air France",
  "Air France 2",
  "Air France 3",
  "Ahougnansou",
  "Ahougnansou Château",
  "Allakro",
  "Angoua",
  "Belleville",
  "Broukro",
  "Commerce",
  "Dar Es Salam",
  "Dougouba",
  "Djambourou",
  "Gonfreville",
  "Habitat",
  "Kennedy",
  "Kennedy Extension",
  "Kôkô",
  "Kôkô Commerce",
  "Kôkô Résidentiel",
  "Kpangbassou",
  "Liberté",
  "Municipal",
  "Nimbo",
  "Nimbo Extension",
  "Odiennékourani",
  "Sokoura",
  "Tolakouadiokro",
  "Zone",
].map((label) => ({ id: slugify(label), label }));

const INTERESTS = [
  { id: "sport", label: "Sport" },
  { id: "musique", label: "Musique" },
  { id: "cinema", label: "Cinéma" },
  { id: "lecture", label: "Lecture" },
  { id: "voyage", label: "Voyage" },
  { id: "danse", label: "Danse" },
];

const PROFILES = {
  aissata: {
    id: "aissata",
    name: "Aissata",
    age: 22,
    university: "Université Alassane Ouattara",
    faculty: "Informatique",
    level: "Licence 3",
    city: "Bouaké",
    neighborhood: "Kôkô",
    country: "Côte d'Ivoire",
    rating: 5,
    about: "Passionnée de lecture, musique et de voyages.",
    interests: ["Musique", "Voyage", "Lecture"],
    tags: ["Musique", "Voyage"],
    photo: null,
    gallery: [],
    verified: true,
  },
  kevin: {
    id: "kevin",
    name: "Kevin",
    age: 24,
    university: "HETEC Bouaké",
    faculty: "Génie Logiciel",
    level: "Master 1",
    city: "Bouaké",
    neighborhood: "Belleville",
    country: "Côte d'Ivoire",
    rating: 4,
    about: "Fan de sport et de cinéma, toujours partant pour découvrir de nouveaux endroits.",
    interests: ["Sport", "Cinéma"],
    tags: ["Sport", "Cinéma"],
    photo: null,
    gallery: [],
    verified: false,
  },
  sarah: {
    id: "sarah",
    name: "Sarah",
    age: 21,
    university: "ESC Bouaké",
    faculty: "Marketing",
    level: "Licence 2",
    city: "Bouaké",
    neighborhood: "Commerce",
    country: "Côte d'Ivoire",
    rating: 5,
    about: "Danseuse à mes heures perdues, toujours un livre dans le sac.",
    interests: ["Danse", "Lecture"],
    tags: ["Danse", "Lecture"],
    photo: null,
    gallery: [],
    verified: true,
  },
};

const NEW_MEMBERS = [
  { id: "laura", name: "Laura", photo: null },
  { id: "yann", name: "Yann", photo: null },
  { id: "chloe", name: "Chloé", photo: null },
  { id: "mohamed", name: "Mohamed", photo: null },
];

const CONVERSATIONS = [
  { id: "alice", name: "Alice", time: "14:30", preview: "Salut ! 🎉", unread: 1, photo: null },
  { id: "kevin", name: "Kevin", time: "13:15", preview: "Comment vas-tu et toi ?", unread: 1, photo: null },
  { id: "sarah", name: "Sarah", time: "12:45", preview: "Merci beaucoup !", photo: null },
  { id: "david", name: "David", time: "Hier", preview: "On peut se voir ce week-end ?", photo: null },
  { id: "emma", name: "Emma", time: "Hier", preview: "D'accord avec plaisir !", photo: null },
];

const CONVERSATION_MESSAGES = {
  alice: {
    contact: { name: "Alice", photo: null, online: true },
    messages: [
      { from: "them", text: "Salut ! 🎉", time: "14:28" },
      { from: "me", text: "Salut ! Ça va ?", time: "14:30" },
      { from: "them", text: "Oui ça va bien et toi ?", time: "14:29" },
    ],
  },
};

const MATCHES = [
  { id: "alice", name: "Alice", online: true, photo: null },
  { id: "kevin", name: "Kevin", online: true, photo: null },
  { id: "sarah", name: "Sarah", online: false, photo: null },
  { id: "david", name: "David", online: true, photo: null },
];

// État en mémoire de l'utilisateur "connecté" pendant la session de test.
// Remis à zéro à chaque redémarrage de `npm run dev`.
let currentUserProfile = {
  id: "moi",
  name: "",
  age: null,
  university: "",
  faculty: "",
  level: "",
  city: "",
  neighborhood: "",
  country: "Côte d'Ivoire",
  gender: "",
  rating: 0,
  about: "",
  interests: [],
  photo: null,
  gallery: [],
  verified: false,
  unreadMessages: 0,
  unreadNotifications: 0,
};

function universityLabel(id) {
  return UNIVERSITIES.find((u) => u.id === id)?.label || id;
}

function facultyLabel(id) {
  return FACULTIES.find((f) => f.id === id)?.label || id;
}

function neighborhoodLabel(id) {
  return NEIGHBORHOODS.find((n) => n.id === id)?.label || id;
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = "";
    req.on("data", (chunk) => (raw += chunk));
    req.on("end", () => {
      try {
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        resolve({});
      }
    });
  });
}

function sendJson(res, status, data) {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json");
  res.end(JSON.stringify(data));
}

export default function mockApiPlugin() {
  return {
    name: "campuslink-mock-api",
    configureServer(server) {
      server.middlewares.use("/api", async (req, res) => {
        await new Promise((r) => setTimeout(r, 300));

        const url = new URL(req.url, "http://localhost");
        const path = url.pathname;
        const method = req.method;

        // --- AUTH ---
        if (path === "/auth/signup" && method === "POST") {
          const body = await readBody(req);
          currentUserProfile = {
            ...currentUserProfile,
            name: body.firstName || body.fullName || "",
          };
          return sendJson(res, 200, {
            token: "mock-token",
            user: { name: currentUserProfile.name },
          });
        }
        if (path === "/auth/login" && method === "POST") {
          return sendJson(res, 200, { token: "mock-token", user: { name: currentUserProfile.name } });
        }
        if (path === "/auth/forgot-password" && method === "POST") {
          res.statusCode = 204;
          return res.end();
        }

        // --- PROFILES ---
        if (path === "/profiles/me" && method === "GET") {
          return sendJson(res, 200, {
            name: currentUserProfile.name || "Utilisateur",
            photo: currentUserProfile.photo,
            unreadMessages: currentUserProfile.unreadMessages,
            unreadNotifications: currentUserProfile.unreadNotifications,
          });
        }
        if (path === "/profiles/discovery" && method === "GET") {
          return sendJson(res, 200, Object.values(PROFILES));
        }
        if (path === "/profiles/new-members" && method === "GET") {
          return sendJson(res, 200, NEW_MEMBERS);
        }
        if (path.match(/^\/profiles\/[^/]+\/like$/) && method === "POST") {
          res.statusCode = 204;
          return res.end();
        }
        if (path.match(/^\/profiles\/[^/]+\/pass$/) && method === "POST") {
          res.statusCode = 204;
          return res.end();
        }
        if (path === "/profiles/me/photos" && method === "POST") {
          return sendJson(res, 200, { photos: currentUserProfile.gallery });
        }
        if (path === "/profiles/me" && method === "PATCH") {
          const body = await readBody(req);
          currentUserProfile = {
            ...currentUserProfile,
            ...body,
            ...(body.university ? { universityLabel: universityLabel(body.university) } : {}),
            ...(body.faculty ? { facultyLabel: facultyLabel(body.faculty) } : {}),
            ...(body.neighborhood ? { neighborhoodLabel: neighborhoodLabel(body.neighborhood) } : {}),
          };
          return sendJson(res, 200, currentUserProfile);
        }
        const profileMatch = path.match(/^\/profiles\/([^/]+)$/);
        if (profileMatch && method === "GET") {
          const id = profileMatch[1];
          if (id === "moi") {
            return sendJson(res, 200, {
              ...currentUserProfile,
              university: currentUserProfile.universityLabel || currentUserProfile.university,
              faculty: currentUserProfile.facultyLabel || currentUserProfile.faculty,
              city: currentUserProfile.city || "Bouaké",
              country: "Côte d'Ivoire",
            });
          }
          const profile = PROFILES[id] || {
            id,
            name: "Utilisateur",
            age: null,
            university: "",
            faculty: "",
            level: "",
            city: "Bouaké",
            neighborhood: "",
            country: "Côte d'Ivoire",
            rating: 0,
            about: "",
            interests: [],
            photo: null,
            gallery: [],
            verified: false,
          };
          return sendJson(res, 200, profile);
        }

        // --- REFERENCE DATA ---
        if (path === "/reference/universities" && method === "GET") {
          return sendJson(res, 200, UNIVERSITIES);
        }
        if (path === "/reference/faculties" && method === "GET") {
          return sendJson(res, 200, FACULTIES);
        }
        // Compatibilité si un ancien appel /reference/universities/:id/faculties est fait
        if (path.match(/^\/reference\/universities\/[^/]+\/faculties$/) && method === "GET") {
          return sendJson(res, 200, FACULTIES);
        }
        if (path === "/reference/cities" && method === "GET") {
          return sendJson(res, 200, CITIES);
        }
        if (path === "/reference/neighborhoods" && method === "GET") {
          return sendJson(res, 200, NEIGHBORHOODS);
        }
        if (path === "/reference/interests" && method === "GET") {
          return sendJson(res, 200, INTERESTS);
        }

        // --- SEARCH ---
        if (path === "/search" && method === "GET") {
          return sendJson(res, 200, Object.values(PROFILES));
        }

        // --- CONVERSATIONS ---
        if (path === "/conversations" && method === "GET") {
          return sendJson(res, 200, CONVERSATIONS);
        }
        const convMessagesMatch = path.match(/^\/conversations\/([^/]+)\/messages$/);
        if (convMessagesMatch && method === "POST") {
          const body = await readBody(req);
          return sendJson(res, 200, { id: "msg-" + Date.now(), from: "me", text: body.text, time: "" });
        }
        const convMatch = path.match(/^\/conversations\/([^/]+)$/);
        if (convMatch && method === "GET") {
          const id = convMatch[1];
          const found = CONVERSATION_MESSAGES[id];
          const conv = CONVERSATIONS.find((c) => c.id === id);
          return sendJson(
            res,
            200,
            found || {
              contact: { name: conv?.name || id, photo: null, online: true },
              messages: [],
            }
          );
        }

        // --- MATCHES ---
        if (path === "/matches" && method === "GET") {
          return sendJson(res, 200, MATCHES);
        }

        // --- STATS ---
        if (path === "/stats/public" && method === "GET") {
          return sendJson(res, 200, { memberCount: 10842, avatarPhotos: [] });
        }

        // --- TESTIMONIALS (landing page) ---
        if (path === "/testimonials" && method === "GET") {
          return sendJson(res, 200, [
            {
              id: "t1",
              name: "Aissata",
              role: "Étudiante en Informatique",
              quote: "J'ai rencontré des amis proches grâce à CampusLink, on révise ensemble toutes les semaines.",
              photo: null,
            },
            {
              id: "t2",
              name: "Kevin",
              role: "Étudiant en Master",
              quote: "Une interface simple et des profils vraiment vérifiés, ça change tout.",
              photo: null,
            },
            {
              id: "t3",
              name: "Sarah",
              role: "Étudiante en Licence",
              quote: "Grâce à la recherche par quartier, j'ai trouvé des gens à deux pas de chez moi.",
              photo: null,
            },
          ]);
        }

        // --- CONTACT (landing page) ---
        if (path === "/contact" && method === "POST") {
          await readBody(req);
          res.statusCode = 204;
          return res.end();
        }

        return sendJson(res, 404, { message: `Mock API: route non gérée (${method} ${path})` });
      });
    },
  };
}
