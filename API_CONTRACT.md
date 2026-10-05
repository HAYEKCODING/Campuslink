# CampusLink — Contrat API réel (frontend ↔ backend)

Ce document décrit **l'API réellement implémentée** par `campuslink-backend` et
consommée par le frontend. Il remplace l'ancienne version, qui décrivait des
endpoints inexistants (`/auth/signup`, `/search`, `/conversations`, `/reference/*`,
`/stats/public`, `/testimonials`, `/contact`) : le frontend qui s'y conformait
affichait des écrans vides ou en erreur.

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
  (redirection vers `/connexion`).
- **Erreurs** : corps JSON `{ message, fieldErrors? }` → affiché tel quel s'il
  est présent, sinon message générique côté client.

---

## Authentification — `src/services/authService.js`

| Fonction | Méthode & route | Payload | Réponse |
|---|---|---|---|
| `signup(data)` | `POST /auth/register` | `{ email, password, confirmPassword, firstName, lastName }` | `201` (aucun token) — le client se connecte ensuite via `/auth/login` |
| `login(credentials)` | `POST /auth/login` | `{ email, password }` | `{ accessToken, refreshToken, tokenType, ... }` |
| — | `POST /auth/refresh-token` | `{ refreshToken }` | nouvelle paire de tokens (rotation) |
| — | `POST /auth/logout` | `{ refreshToken }` | révoque le refresh token |
| `requestPasswordReset(email)` | `POST /auth/forgot-password` | `{ email }` | `204`/enveloppe vide (message identique que le compte existe ou non) |
| — | `POST /auth/reset-password` | `{ token, password }` | `200` |

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

`avatarUrl, firstName, lastName, gender, dateOfBirth, university, fieldOfStudy,
neighborhood, city, bio, interests[]`.

**Champs de l'onboarding sans équivalent en base** (non envoyés) :
`level` (Licence/Master/Doctorat) — `Profile` ne possède aucun champ
correspondant. Le sélecteur "Niveau d'étude" a été retiré de l'étape 1 plutôt
que de perdre silencieusement la saisie.

## Recherche — `src/services/searchService.js`

`GET /profiles/search` avec paramètres optionnels :
`minAge, maxAge, university, fieldOfStudy, neighborhood, city, interests`
(répété : `?interests=Football&interests=Musique` = « au moins un de ces
centres d'intérêt »), plus la pagination/tri Spring Data (`page`, `size`, `sort`
— `sort=age,desc` est traduit côté serveur en `dateOfBirth`).

Le **genre n'est pas supporté** par l'endpoint : le filtre correspondant a été
retiré de l'interface.

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
`CONNECT`). Le frontend ne s'y abonne pas encore — voir « reste à faire ».

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
| `GET /reference/universities|faculties|neighborhoods|interests` | listes de démarrage côté client (`src/lib/referenceData.js`) + saisie libre | exposer ces routes alimentées par la base, puis supprimer le fichier local |
| `GET /stats/public` (landing) | compteur d'inscrits masqué si erreur | endpoint agrégé public |
| `GET /testimonials` | section témoignages en état d'erreur | table + endpoint public |
| `POST /contact` | formulaire de contact en état d'erreur | endpoint public (email ou file d'attente) |
| Recherche par genre | filtre retiré de l'UI | ajouter `gender` à `ProfileSearchCriteria` si le besoin existe |
| Champ `level` (Licence/Master/Doctorat) | sélecteur retiré de l'onboarding | colonne `level` sur `profiles` + DTO |
| Envoi temps réel (WebSocket) | l'envoi passe par HTTP, pas de messages entrants en direct | client STOMP côté frontend (subscription `/user/queue/messages`) |
| Connexion OAuth (Google/Facebook/Apple) | boutons désactivés | implémenter OAuth2 côté backend ou retirer les boutons |
