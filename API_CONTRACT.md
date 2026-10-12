# CampusLink — Contrat API réel (frontend ↔ backend)

Ce document décrit **l'API réellement implémentée** par `campuslink-backend` et
consommée par le frontend. Il décrit aussi les routes publiques de la landing
(`/reference/*`, `/stats/public`, `/testimonials`, `/contact`), désormais
implémentées côté backend (module `PublicContentController`).

- **Base URL** : variable d'environnement `VITE_API_URL` (fichier `.env` à la
  racine, ex. `VITE_API_URL=https://api.campuslink.ci`). Défaut : `/api`
  (le backend sert l'API sous `server.servlet.context-path: /api`).
- **Enveloppe de succès** : `{ success, message, data, timestamp }` — le client
  délivre `data` (voir `src/lib/api.js`). Les modules temps réel (`/matches`,
  `/likes`, `/messages`, `/notifications`) répondent en **JSON brut**, sans
  enveloppe : `unwrap()` gère les deux formes.
- **Authentification** : `Authorization: Bearer <accessToken>`.
  Sur un `401`, le client tente **un** `POST /auth/refresh-token`, rejoue la
  requête, puis nettoie les tokens et émet `campuslink:unauthorized`
  (redirection vers `/connexion`) — **uniquement si une session existait**
  (au moins un token stocké) : un `401` reçu en navigation anonyme est remonté
  comme erreur sans déconnecter ni rediriger.
- **Erreurs** : corps JSON `{ message, fieldErrors? }` → affiché tel quel s'il
  est présent, sinon message générique côté client. `fieldErrors[].rejectedValue`
  est masqué (`"***"`) pour les champs sensibles (mot de passe, token, code
  OTP, email) : la valeur saisie n'est jamais réaffichée dans la réponse.

---

## Authentification — `src/services/authService.js`

| Fonction | Méthode & route | Payload | Réponse |
|---|---|---|---|
| `signup(data)` | `POST /auth/register` | `{ email, password, confirmPassword, firstName, lastName }` | `201` (aucun token) — le client se connecte ensuite via `/auth/login` |
| `login(credentials)` | `POST /auth/login` | `{ email, password }` | `{ accessToken, refreshToken, tokenType, ... }` |
| — | `POST /auth/refresh-token` | `{ refreshToken }` | nouvelle paire de tokens (rotation) |
| — | `POST /auth/logout` | `{ refreshToken }` | révoque le refresh token |
| `requestPasswordReset(email)` | `POST /auth/forgot-password` | `{ email }` | `200` (message identique que le compte existe ou non) ; `429` si relance sous 60 s (cooldown) |
| `resetPassword(token, pwd, confirm)` | `POST /auth/reset-password` | `{ token, newPassword, confirmPassword }` | `200` — jeton usage unique (30 min), toutes les sessions révoquées ; `400` si jeton invalide/expiré |

Le lien reçu par email pointe vers `/reset-password?token=…` : route gérée par
`src/ResetPasswordPage.jsx` (formulaire nouveau mot de passe + confirmation).
En local, aucun SMTP : l'email (et le lien/le code OTP) est **journalisé** dans
`backend.log` — `application.mail.log-only: true` dans le profil dev, forcé à
`false` en prod (sinon `/otp/send` et `/auth/forgot-password` renvoyaient 503).

`phone` et `gender` collectés à l'inscription ne sont pas envoyés : `RegisterRequest`
ne les possède pas encore (à ajouter côté backend si besoin).

## Profils — `src/services/profileService.js`

| Fonction | Méthode & route | Réponse |
|---|---|---|
| `getMyProfile()` / `getCurrentUser()` | `GET /profiles/me` | `ProfileResponse` |
| `updateProfile(data)` | `PUT /profiles/me` | `ProfileResponse` (remplacement intégral : le client relit puis fusionne) |
| `getProfileById(id)` | `GET /profiles/{uuid}/public` | `ProfileResponse` (route publique) |
| `getDiscoveryProfiles()` / `getNewMembers()` | `GET /profiles/search` | `PageResponse<ProfileResponse>` |
| `uploadProfilePhotos(files)` / `uploadAvatar(file)` | `POST /media/upload` (multipart, champ `file`) | `{ url, publicId, ... }` |
| `likeProfile(legacyId)` | `POST /likes` | `{ cibleId: <legacyId> }` |

### `ProfileResponse`

```json
{
  "id": "uuid-du-profil",
  "legacyId": 42,
  "avatarUrl": "https://…",
  "firstName": "Awa", "lastName": "Kone",
  "age": 21, "gender": "MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY",
  "university": "…", "fieldOfStudy": "…",
  "neighborhood": "…", "city": "…",
  "bio": "…", "interests": ["Musique", "Football"]
}
```

- `id` (UUID) sert aux liens `/app/profil/:id` et à la consultation publique.
- `legacyId` (Long) sert au module temps réel : likes, matchs, messages.
  **Ne jamais les confondre.**
- `dateOfBirth` n'est jamais exposée : uniquement `age` (vie privée).
- Le frontend ajoute côté client les champs de confort `name`, `photo`,
  `faculty`, `about` (`normalizeProfile()`).

### `PUT /profiles/me` — `ProfileRequest`

`avatarUrl, firstName, lastName, gender, level, dateOfBirth, university,
fieldOfStudy, neighborhood, city, bio, interests[]`.

`gender` (enum `Gender`) et `level` (enum `StudyLevel` : `LICENCE`, `MASTER`,
`DOCTORAT`) sont persistés à la création comme à la mise à jour. Le frontend
accepte les labels français (« Licence ») et les enums (« LICENCE ») : voir
`LEVEL_TO_API` / `GENDER_TO_API` dans `profileService.js`. La colonne `level`
est incluse dans `database/schema.sql` (MySQL) et `schema.postgresql.sql`.

Le sélecteur "Niveau d'étude" est de retour à l'étape 1 de l'onboarding
(optionnel, valeur `null` = « Non précisé »).

## Recherche — `src/services/searchService.js`

`GET /profiles/search` avec paramètres optionnels :
`gender, minAge, maxAge, university, fieldOfStudy, neighborhood, city, interests`
(répété : `?interests=Football&interests=Musique` = « au moins un de ces
centres d'intérêt »), plus la pagination/tri Spring Data (`page`, `size`, `sort`
— `sort=age,desc` est traduit côté serveur en `dateOfBirth`).

Le **genre** est un filtre d'**égalité stricte** sur l'enum `Gender`
(`MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY`) : paramètre absent ou vide = tous les
genres, valeur inconnue = `400`. Exposé dans l'UI (liste « Genre » de
`SearchFilters`).

## Matchs & likes (module temps réel)

| Fonction frontend | Méthode & route | Réponse |
|---|---|---|
| `getMatches()` | `GET /matches` | `MatchResponse[]` (matchs actifs) |
| — | `GET /matches/historique` | matchs actifs + rompus |
| — | `DELETE /matches/{id}` | unmatch |
| `likeProfile(legacyId)` | `POST /likes` | `{ cibleId }` → `{ matchCree }` |
| — | `DELETE /likes/{cibleId}`, `GET /likes`, `GET /likes/recus` | likes |

### `MatchResponse`

```json
{
  "id": 10,
  "autreUtilisateurId": 42,
  "autreUtilisateurProfilId": "uuid-du-profil",
  "autreUtilisateurNom": "Awa Kone",
  "autreUtilisateurPhoto": "https://…",
  "dateMatch": "2026-10-03T20:00:00Z",
  "statut": "ACTIF|ROMPU"
}
```

`autreUtilisateurProfilId/Nom/Photo` sont résolus côté serveur via
`UserDirectoryPort` (nouveau) : sans eux, « Mes matchs » et la liste des
conversations ne pouvaient afficher aucune identité.

## Messagerie — `src/services/messagesService.js`

Une « conversation » = un match.

| Fonction | Méthode & route | Réponse |
|---|---|---|
| `getMessages(matchId)` | `GET /matches/{id}/messages?page&size` | `Page<MessageResponse>` — **du plus récent au plus ancien** (le client inverse) |
| `sendMessage(matchId, text)` | `POST /matches/{id}/messages` | `{ id, matchId, expediteurId, contenu, dateEnvoi, statutLecture }` |
| — | `POST /matches/{id}/messages/lu` | `204` |

`POST /matches/{id}/messages` (nouveau) est l'**équivalent HTTP** du point
d'entrée STOMP `/app/chat.send` : mêmes règles métier (participant au match,
match actif), il permet d'envoyer sans client WebSocket. Payload identique à
celui du WebSocket : `{ matchId, contenu }`.

Temps réel : le serveur diffuse sur `/user/queue/messages` et
`/user/queue/read-receipts` (handshake `/ws`, authentification JWT sur la trame
`CONNECT`). Le frontend s'y abonne via `src/lib/realtime.js` (client STOMP
`@stomp/stompjs`, singleton par onglet, reconnexion automatique + rejeu des
souscriptions) : `ChatConversation` reçoit les messages entrants en direct et
diffuse l'indicateur de saisie sur `/topic/matches/{id}/typing`.

## Contenu public de la landing — `src/services/*`

Toutes ces routes sont **anonymes** (listées dans
`SecurityConstants.PUBLIC_ENDPOINTS`) : un visiteur non connecté affiche la
page d'accueil sans redirection vers `/connexion`.

| Service / écran | Méthode & route | Réponse |
|---|---|---|
| `statsService.js` | `GET /stats/public` | `{ memberCount, avatarPhotos[] }` (compteur `COUNT(profiles)` + échantillon de 4 avatars) |
| `testimonialsService.js` | `GET /testimonials` | `[{ id, name, role, photo, quote }]` (témoignages `active=true`, triés par `displayOrder`) |
| `contactService.js` | `POST /contact` | `201` — payload `{ name, email, message }` validé (`@NotBlank`/`@Email`), persisté dans `contact_messages` |
| `referenceService.js` | `GET /reference/{universities|faculties|neighborhoods|interests}` | `string[]` — valeurs `DISTINCT` des profils en base |

Les référentiels sont mis en cache par onglet et **retombent sur les listes
statiques** de `src/lib/referenceData.js` si l'endpoint échoue ou renvoie une
liste vide (base vierge, backend en démarrage) : l'onboarding n'est jamais
bloqué.

### Seed administrateur (déploiement)

`AdminSeedRunner` crée le premier compte ADMIN au démarrage si les variables
`ADMIN_EMAIL` / `ADMIN_PASSWORD` (prop. `campuslink.admin.*`) sont définies.
No-op sans variables ou si le compte existe déjà ; erreur explicite si la table
`roles` est vide (exécuter `database/schema.sql` d'abord).

## OTP & divers (présents côté backend, non consommés par le frontend)

- `POST /otp/send|resend|verify`
- `GET /notifications`, `/notifications/non-lues`, `/notifications/compteur`,
  `POST /notifications/{id}/lu`, `/notifications/lu-tout`
- `POST /reports`, routes `/moderation/*`, `/admin/*`, `/admin/dashboard`
- `GET /profiles/me` … routes `/*` **non listées ici sont protégées** par JWT.

---

## Écarts connus backend ↔ frontend (reste à faire)

| Manque | Impact actuel | Proposition |
|---|---|---|
| Accusés de lecture en direct | `/user/queue/read-receipts` diffusé par le serveur mais non souscrit côté client | s'abonner dans `ChatConversation` et afficher « Lu » |
| Notifications temps réel | routes HTTP existantes, client STOMP non branché sur `/user/queue/notifications` | souscrire + badge dans `Layout` |
| Administration des témoignages / messages de contact | tables écrites mais aucune UI ni route `/admin/*` pour les gérer | routes `GET/POST/PATCH/DELETE /admin/testimonials` + `/admin/contact-messages` |
| Connexion OAuth (Google/Facebook/Apple) | boutons désactivés | implémenter OAuth2 côté backend ou retirer les boutons (nécessite des identifiants externes) |
| Lignes « Paramètres » sans action (Modifier le profil, Changer le mot de passe, Confidentialité, Notifications, Aide, Conditions, À propos) | `SettingsRow` n'a aucun `onClick` : les clics n'ont aucun effet | soit une destination réelle (ex. `/onboarding/1` pour « Modifier le profil »), soit un libellé « bientôt » explicite ; « Changer le mot de passe » exigerait un `POST /auth/change-password` côté backend |

---

## Simulation API

`python simulate_api.py` (racine du projet, backend sur :8080) rejoue les 57
opérations de `/api/v3/api-docs` en ~120 vérifications : public, auth + OTP +
forgot/reset, profils (dont **l'exclusion de soi** dans `/profiles/search`),
médias, likes/matchs/messages, notifications, signalements/modération,
administration (avec réinitialisation du mot de passe admin par le lien email)
et les contrôles d'accès 401/403. Rapport écrit dans `simulation_report.txt`.
Dernier passage : **122/122**.
