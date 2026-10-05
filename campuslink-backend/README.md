# CampusLink — Backend

Backend officiel de la plateforme **CampusLink**, construit avec Spring Boot 3.5 / Java 17.

> ⚠️ Ce dépôt contient actuellement uniquement le **socle technique** du projet
> (architecture, sécurité, configuration). Aucune logique métier n'a encore été implémentée.

---

## Stack technique

| Composant            | Technologie                          |
|-----------------------|---------------------------------------|
| Langage                | Java 17                              |
| Framework               | Spring Boot 3.5.x                    |
| Build                   | Maven                                |
| Base de données          | MySQL (WampServer) par défaut — PostgreSQL via Docker/Render |
| Persistance              | Spring Data JPA / Hibernate          |
| Sécurité                 | Spring Security + JWT (Access/Refresh) |
| Validation                | Jakarta Bean Validation             |
| Mapping DTO ↔ Entité       | MapStruct                          |
| Boilerplate               | Lombok                              |
| Documentation API          | Swagger / springdoc-openapi         |
| E-mails                    | Spring Mail                         |
| Stockage médias             | Cloudinary                         |
| Tests                       | JUnit 5, Mockito, H2                |

---

## Architecture du projet

Architecture en couches, respectant les principes **SOLID** et une séparation stricte des responsabilités :

```
src/main/java/com/campuslink/
├── config/          # Configuration transverse (Swagger, CORS, Cloudinary, JPA Auditing, properties)
├── security/         # Spring Security : filter chain, JWT provider/filter, handlers
├── controller/        # Couche API REST (points d'entrée HTTP)
├── service/            # Contrats métier (interfaces)
├── service/impl/        # Implémentations des services
├── repository/            # Interfaces Spring Data JPA
├── entity/                 # Entités JPA (modèle de persistance)
├── dto/request/             # Objets de transfert entrants (payloads API)
├── dto/response/              # Objets de transfert sortants (réponses API)
├── mapper/                      # Mappers MapStruct (Entity <-> DTO)
├── exception/                     # Exceptions métier + gestionnaire global
├── validation/                      # Annotations et validateurs personnalisés
├── util/                              # Classes utilitaires transverses
├── constant/                            # Constantes applicatives
└── enums/                                 # Énumérations du domaine
```

### Principes appliqués

- **SRP** : chaque couche (controller / service / repository) a une responsabilité unique et clairement délimitée.
- **DIP** : les controllers dépendent d'interfaces `service`, jamais des implémentations (`service/impl`).
- **OCP** : les mappers (MapStruct) et le `GlobalExceptionHandler` permettent d'étendre le comportement sans modifier le code existant.
- **ISP** : les interfaces de service seront découpées par domaine métier (une interface = un domaine).
- **Stateless JWT** : aucune session serveur — authentification et autorisations portées entièrement par le token.

---

## Prérequis

- JDK 17+
- Maven 3.9+ (ou [maven-mvnd](https://maven.apache.org/mvnd/), utilisé dans ce dépôt)
- **MySQL 5.6+ / 8.x via [WampServer](https://wampserver.aviatechno.net/)**
  (recommandé en local — aucune donnée Docker requise)
  *ou* PostgreSQL 14+ avec Docker (voir « Option B/C »)
- Un compte Cloudinary (pour l'upload de fichiers)
- Un compte SMTP (Gmail, Mailtrap, etc.) pour l'envoi d'e-mails

---

## Démarrage rapide

### Option A — MySQL via WampServer (recommandé en local)

Aucun Docker ni PostgreSQL : par défaut le backend se connecte à
`jdbc:mysql://localhost:3306/campuslink_db` avec l'utilisateur `root` **sans mot
 de passe** (configuration par défaut de WampServer). Variables `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` : inutiles tant qu'on reste sur ces
valeurs.

#### 1. Démarrer WampServer

Cliquer sur l'icône WampServer pour qu'elle devienne **verte** : Apache et MySQL
sont actifs (MySQL écoute sur le port `3306`).

#### 2. Créer la base et l'initialiser (une seule fois)

Depuis la racine du backend :

```bash
# Windows — chemin complet du client MySQL fourni par Wamp :
C:\wamp\bin\mysql\mysql5.6.17\bin\mysql.exe -u root < database/schema.sql

# Si mysql est dans le PATH :
mysql -u root < database/schema.sql
```

Autre possibilité : **phpMyAdmin** → onglet *Importer* → choisir
`database/schema.sql`. Le script crée `campuslink_db` (utf8mb4) si elle
n'existe pas, crée les 14 tables et **seed les 3 rôles**
(`STUDENT`, `TEACHER`, `ADMIN`).

> ⚠️ Ce seed est obligatoire : sans lui, l'inscription échoue avec
> « Rôle STUDENT introuvable en base ».

#### 3. Lancer l'application

```bash
mvnd spring-boot:run            # ou : mvn spring-boot:run
```

L'application démarre sur `http://localhost:8080/api` (profil `dev`,
`ddl-auto: update` complète le schéma si les entités évoluent).

---

### Option B — Tout via Docker Compose (backend + PostgreSQL)

```bash
cp .env.example .env
# Renseigner au minimum JWT_SECRET (obligatoire), le reste a des valeurs par défaut
docker compose up -d --build
```

Services démarrés :
- `campuslink-postgres` : PostgreSQL 16, port `5432`, healthcheck `pg_isready`
- `campuslink-backend` : API Spring Boot, port `8080` (`http://localhost:8080/api`), healthcheck sur `/api/actuator/health`

Le backend attend que PostgreSQL soit `healthy` avant de démarrer (`depends_on: condition: service_healthy`),
et les deux services communiquent via le réseau Docker dédié `campuslink-network`.

Variables d'environnement principales (voir `.env.example` pour la liste complète) :
`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`/`MAIL_PASSWORD`, `CLOUDINARY_*`.

Arrêter les conteneurs :

```bash
docker compose down          # conserve les données PostgreSQL (volume)
docker compose down -v       # supprime aussi le volume de données
```

### Option C — PostgreSQL en Docker, backend en local

#### 1. Cloner le projet et configurer l'environnement

```bash
cp .env.example .env
# Renseigner les variables (DB, JWT_SECRET, Cloudinary, Mail...)
```

#### 2. Lancer PostgreSQL (via Docker)

```bash
docker compose up -d campuslink-postgres
```

#### 3. Lancer l'application (en pointant sur PostgreSQL)

Le défaut de `application.yml` est MySQL : pour ce mode, il faut surcharger
l'URL et le driver (Git Bash / Linux) :

```bash
DB_URL=jdbc:postgresql://localhost:5432/campuslink_db \
DB_DRIVER=org.postgresql.Driver \
DB_USERNAME=postgres DB_PASSWORD=postgres \
mvnd spring-boot:run
```

L'application démarre par défaut sur `http://localhost:8080/api`.

> Les variables de `.env` ne sont lues que par `docker-compose.yml` : dans ce
> mode « backend en local », on les passe à la commande, pas au fichier `.env`.

### 4. Documentation Swagger

Une fois l'application démarrée :

- Swagger UI : `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON : `http://localhost:8080/api/v3/api-docs`

---

## Profils Spring

| Profil  | Usage                                   |
|---------|------------------------------------------|
| `dev`   | Développement local (logs verbeux, `ddl-auto: update`) |
| `prod`  | Production (`ddl-auto: validate`, logs réduits)         |
| `test`  | Tests automatisés (H2 en mémoire)                       |

Activation via la variable d'environnement `SPRING_PROFILES_ACTIVE`.

---

## Sécurité — Vue d'ensemble

Authentification **stateless** basée sur JWT (Access Token courte durée + Refresh Token longue durée),
avec rechargement de l'utilisateur depuis la base à chaque requête (révocation immédiate en cas de
bannissement/suspension, sans attendre l'expiration du token).

| Composant                    | Rôle                                                              |
|-------------------------------|--------------------------------------------------------------------|
| `JwtService`                   | Génère et valide les tokens (access + refresh), extrait les claims |
| `JwtAuthenticationFilter`       | Intercepte chaque requête, authentifie via le token Bearer         |
| `CustomUserDetailsService`       | Charge un `User` depuis la base (MySQL/PostgreSQL) (login + revalidation par requête) |
| `UserPrincipal`                   | Adapte l'entité `User` au contrat `UserDetails` de Spring Security |
| `AuthenticationProvider`           | `DaoAuthenticationProvider` — vérifie email/mot de passe (BCrypt)  |
| `SecurityConfig`                    | Chaîne de filtres, CORS, CSRF, routes publiques/protégées        |
| `JwtAuthenticationEntryPoint`         | Réponse JSON homogène en cas de 401 (non authentifié)           |
| `JwtAccessDeniedHandler`               | Réponse JSON homogène en cas de 403 (droits insuffisants)     |

Mots de passe hashés via `BCryptPasswordEncoder`. Endpoints publics définis dans
`SecurityConstants.PUBLIC_ENDPOINTS` (`/v1/auth/**`, Swagger) — tout le reste exige un
access token JWT valide.

> Les endpoints d'authentification eux-mêmes (`POST /v1/auth/login`, `/register`,
> `/refresh`) ainsi que le `AuthController` qui orchestrera `AuthenticationManager` et
> `JwtService` seront implémentés dans une itération ultérieure.

---

## Module Administration

Gestion des utilisateurs réservée aux administrateurs. **Tous les endpoints exigent
ROLE_ADMIN**, à deux niveaux : règle de chemin (`/admin/** → hasRole("ADMIN")` dans
`SecurityConfig`) et `@PreAuthorize("hasRole('ADMIN')")` sur `AdminController`
(`@EnableMethodSecurity`, activé pour la première fois ici).

| Méthode  | Endpoint                         | Fonction |
|----------|-------------------------------------|-----------|
| `GET`    | `/api/admin/users`                     | Liste / recherche (mêmes filtres, un seul endpoint) |
| `PATCH`  | `/api/admin/users/{userId}`               | Modifier (email, emailVerified) |
| `DELETE` | `/api/admin/users/{userId}`                  | Supprimer |
| `PATCH`  | `/api/admin/users/{userId}/suspend`             | Suspendre (+ révoque les sessions actives) |
| `PATCH`  | `/api/admin/users/{userId}/activate`               | Réactiver |
| `PATCH`  | `/api/admin/users/{userId}/role`                      | Changer le rôle (remplace, n'ajoute pas) |
| `GET`    | `/api/admin/stats`                                       | Statistiques globales |

**Garde-fous anti-auto-sabotage** : un admin ne peut pas se supprimer, se suspendre,
ni retirer son propre rôle ADMIN via ce module.

**Tests** : `UserSpecificationIntegrationTest` (H2), `AdminUserServiceImplTest`
(Mockito), `AdminControllerTest` (contrat HTTP), et surtout
`AdminSecurityIntegrationTest` (`@SpringBootTest`, chaîne de sécurité **réelle** —
le seul test qui prouve que ROLE_ADMIN bloque effectivement qui il doit bloquer).

---

## Module Recherche (moteur de filtres dynamiques)

Recherche de profils par combinaison de filtres, tous optionnels, avec pagination et tri.

| Méthode | Endpoint             | Description                        | Auth requise |
|---------|------------------------|---------------------------------------|:---:|
| `GET`   | `/api/profiles/search`   | Rechercher des profils par filtres      | Non |

**Filtres disponibles** (tous combinables, sémantique `AND` entre filtres) :

| Paramètre       | Type      | Comportement |
|------------------|-----------|---------------|
| `minAge`           | Integer   | Âge minimum (inclus) — traduit en borne sur `dateOfBirth` |
| `maxAge`             | Integer   | Âge maximum (inclus) — idem |
| `university`           | String    | Contient, insensible à la casse |
| `fieldOfStudy`            | String    | Contient, insensible à la casse |
| `neighborhood`               | String    | Contient, insensible à la casse |
| `city`                          | String    | Contient, insensible à la casse |
| `interests`                        | String[]  | Correspond à **au moins un** des intérêts listés (`OR`), pas nécessairement tous |

**Pagination et tri** via les paramètres standards Spring Data : `page`, `size`, `sort`
(ex. `sort=age,desc`, `sort=city,asc`, répétable pour un tri multi-critères).
Réponse enveloppée dans `PageResponse<ProfileResponse>` (contenu, page, taille, total,
`first`/`last`) — pas le `Page` Spring Data brut, dont la forme JSON interne n'est pas
un contrat d'API stable à exposer tel quel.

**Âge = filtre et tri calculés, jamais une colonne** : `Profile` ne stocke pas l'âge
(voir le module Profile — seul `dateOfBirth` est persisté). `ProfileSpecification`
traduit `minAge`/`maxAge` en comparaisons de dates via la Criteria API. Trier par
`sort=age` poserait problème de la même façon (`age` n'est pas un attribut JPA) :
`ProfileSearchServiceImpl` intercepte ce cas et le traduit en tri sur `dateOfBirth`,
**direction inversée** (âge croissant = date de naissance décroissante).

**Implémentation technique** : `ProfileSpecification` (Criteria API, package
`specification/`) construit dynamiquement les prédicats — seuls les critères
effectivement renseignés participent à la requête, combinés en `AND`. Le filtre
"centres d'intérêt" nécessite une jointure sur la table de collection
`profile_interests` ; `query.distinct(true)` évite qu'un profil ayant plusieurs des
intérêts demandés apparaisse en double.

**Tests** : `ProfileSpecificationIntegrationTest` (`@DataJpaTest` + H2 — vérifie le
comportement réel des prédicats générés, pas seulement leur construction) et
`ProfileSearchServiceImplTest` (Mockito — validation des bornes d'âge, traduction du tri).

**Endpoint public**, comme `/profiles/{id}/public` : la recherche n'expose rien de
plus que ce qui est déjà consultable profil par profil individuellement.

---

## Module Média (Cloudinary)

Service générique d'upload, remplacement et suppression de fichiers image, indépendant
de tout module métier — réutilisable partout où une image doit être stockée (photo de
profil, bannière d'événement, etc.).

| Méthode  | Endpoint            | Description                                   |
|----------|-----------------------|--------------------------------------------------|
| `POST`   | `/api/media/upload`     | Uploader un fichier image                           |
| `PUT`    | `/api/media/replace`       | Remplacer un fichier existant par un nouveau           |
| `DELETE` | `/api/media`                   | Supprimer un fichier                                       |

Toutes les routes exigent une authentification — contrairement à `/otp/**` ou
`/auth/**`, rien n'est ouvert ici : n'importe qui pourrait sinon uploader ou
supprimer des fichiers à volonté.

**`publicId` en paramètre de requête, jamais dans l'URL** : les identifiants Cloudinary
contiennent des slashes (`campuslink/profiles/abc123`), incompatibles avec un simple
`@PathVariable`. `PUT /media/replace?publicId=...` et `DELETE /media?publicId=...`.

**Compression appliquée à l'upload** : chaque image est redimensionnée (`crop: limit` —
jamais d'agrandissement, seulement une réduction si l'image dépasse `maxDimension`) et
compressée (`quality: auto:good`, `fetch_format: auto` — Cloudinary choisit le meilleur
format de livraison, ex. WebP) directement dans la transformation d'entrée : le fichier
stocké est déjà optimisé, aucun retraitement à chaque affichage.

**Validation à deux niveaux** : `MediaFileValidator` rejette immédiatement (400, sans
appel réseau) un fichier vide, trop volumineux, ou d'un type MIME non autorisé.
Cloudinary lui-même constitue un second filet : tout contenu qui ne se décode pas comme
une image valide est rejeté côté service, traduit en `MediaUploadException` (502).

**Remplacement résilient** : `replace()` uploade toujours le nouveau fichier *avant*
de tenter de supprimer l'ancien. Si la suppression échoue, l'opération n'échoue pas
pour autant — le nouveau fichier reste en ligne, un fichier orphelin occasionnel sur
Cloudinary étant préférable à un utilisateur sans image du tout.

**Configuration** (voir `.env.example`) :

| Variable                        | Défaut                              | Description |
|------------------------------------|----------------------------------------|--------------|
| `MEDIA_FOLDER`                        | `campuslink`                          | Dossier Cloudinary de rangement |
| `MEDIA_MAX_FILE_SIZE_BYTES`             | `5242880` (5 Mo)                    | Taille maximale acceptée |
| `MEDIA_ALLOWED_CONTENT_TYPES`             | `image/jpeg,image/png,image/webp` | Types MIME autorisés |
| `MEDIA_IMAGE_QUALITY`                       | `auto:good`                     | Niveau de compression Cloudinary |
| `MEDIA_MAX_DIMENSION`                         | `1080`                        | Largeur/hauteur maximale (px) |

> **Périmètre volontaire** : ce module ne fait pas le lien entre un `publicId` et son
> "propriétaire" (quel utilisateur a uploadé quoi) — cette vérification d'appartenance
> revient au module métier qui l'utilise. Il n'est pas non plus câblé automatiquement
> dans `ProfileController` (`avatarUrl`) : ce sera la prochaine intégration naturelle
> si tu le souhaites.

---

## Module Profile

CRUD complet sur le profil utilisateur, avec vue privée ("mon profil") et vue publique.

| Méthode  | Endpoint                     | Description                              | Auth requise |
|----------|--------------------------------|---------------------------------------------|:---:|
| `POST`   | `/api/profiles/me`               | Créer le profil de l'utilisateur authentifié   | Oui |
| `PUT`    | `/api/profiles/me`                  | Remplacer intégralement le profil                | Oui |
| `DELETE` | `/api/profiles/me`                     | Supprimer le profil                                 | Oui |
| `GET`    | `/api/profiles/me`                        | Afficher son propre profil                             | Oui |
| `GET`    | `/api/profiles/{profileId}/public`           | Afficher un profil public                                  | Non |

**Champs du profil** : photo (`avatarUrl`), prénom, nom, âge (calculé), établissement
(`university`), filière (`fieldOfStudy`), quartier (`neighborhood`), ville (`city`), bio,
centres d'intérêt (`interests`).

**Âge calculé, jamais stocké** : l'entité conserve `dateOfBirth` (déjà présent depuis le
modèle de données initial) ; `ProfileMapper.dateOfBirthToAge` calcule l'âge à la volée
via `java.time.Period`. La date de naissance brute n'est **jamais** exposée dans les
réponses — seul l'âge l'est, par souci de vie privée.

**Identification par token, jamais par paramètre** : les routes `/me` déterminent
l'utilisateur via `@AuthenticationPrincipal` (le JWT), pas via un id fourni par le
client — impossible de modifier le profil de quelqu'un d'autre en changeant un
paramètre d'URL.

**Suppression via cascade** : `DELETE /profiles/me` ne fait pas de `DELETE SQL` direct ;
il détache le profil de l'utilisateur (`user.setProfile(null)`) et laisse
`orphanRemoval = true` (déjà configuré sur `User.profile`) supprimer physiquement la ligne.

**Nouveaux champs ajoutés à l'entité `Profile`** : `neighborhood` (colonne) et `interests`
(`@ElementCollection`, table dédiée `profile_interests` — jusqu'à 20 valeurs, 50
caractères chacune).

> Le champ "photo" est une simple URL (`avatarUrl`, déjà présent, pensé pour Cloudinary) :
> ce module ne couvre pas l'upload de fichier lui-même — non demandé explicitement ici.

---

## Module Forgot Password

Réinitialisation de mot de passe par lien sécurisé (distinct du code OTP à 6 chiffres
du module précédent — voir la justification dans `PasswordResetToken`).

| Méthode | Endpoint                    | Description                                        |
|---------|-------------------------------|------------------------------------------------------|
| `POST`  | `/api/auth/forgot-password`     | Envoyer un lien de réinitialisation par email          |
| `POST`  | `/api/auth/reset-password`         | Consommer le lien et définir un nouveau mot de passe      |

**Token, pas OTP** : un token aléatoire de 256 bits (`SecureRandom`, encodé en
base64url) est généré côté serveur, embarqué dans un lien, et envoyé par email.
Seule son empreinte **SHA-256** est stockée en base (`password_reset_tokens.token_hash`)
— jamais le token en clair. BCrypt n'est pas utilisé ici : il est réservé au hash du
**nouveau mot de passe** (BCrypt intègre un sel aléatoire par valeur, incompatible
avec une recherche exacte indexée en base ; SHA-256 est le bon outil pour un secret
déjà aléatoire à 256 bits).

**Anti-énumération** : même principe que le module OTP — `/forgot-password` renvoie
toujours le même message de succès, qu'un compte existe ou non pour l'email fourni.

**Révocation des sessions** : une réinitialisation réussie révoque immédiatement
tous les refresh tokens actifs de l'utilisateur (`RefreshTokenRepository.revokeAllActiveForUser`)
— une session déjà ouverte ailleurs (potentiellement par un attaquant) est coupée.

**Configuration** (voir `.env.example`) :

| Variable                          | Défaut | Description |
|-------------------------------------|--------|--------------|
| `PASSWORD_RESET_EXPIRATION_MINUTES`   | 30     | Durée de validité du lien |
| `PASSWORD_RESET_COOLDOWN_SECONDS`       | 60     | Délai anti-spam entre deux demandes |
| `PASSWORD_RESET_URL`                      | `http://localhost:3000/reset-password` | Base du lien envoyé (le token est ajouté en `?token=`) |
| `PASSWORD_RESET_CLEANUP_CRON`               | `0 */15 * * * *` | Fréquence de purge automatique |

---

## Module OTP

Codes à usage unique génériques (`OtpType` : `EMAIL_VERIFICATION`, `PASSWORD_RESET`,
`PHONE_VERIFICATION`, `TWO_FACTOR_AUTH`), envoyés par email via Spring Mail.

| Méthode | Endpoint         | Description                                        |
|---------|-------------------|------------------------------------------------------|
| `POST`  | `/api/otp/send`     | Générer et envoyer un code                            |
| `POST`  | `/api/otp/resend`     | Renvoyer un code (même opération que `/send`, voir plus bas) |
| `POST`  | `/api/otp/verify`       | Vérifier un code soumis par le client                    |

**`/send` et `/resend` appellent la même méthode de service.** Le délai anti-spam se
calcule contre "quand le dernier code a été émis pour cet (utilisateur, type)", ce
qui est vrai peu importe l'endpoint appelé — les deux existent pour la clarté de
l'API côté client (bouton "Envoyer" vs "Renvoyer"), pas parce que la logique diffère.

**Anti-énumération d'emails** : `/send` et `/resend` renvoient toujours la même
réponse de succès, qu'un compte existe ou non pour l'email fourni. Seul un compte
existant reçoit réellement un email — indispensable pour `PASSWORD_RESET`, où un
attaquant ne doit pas pouvoir déduire quels emails sont enregistrés.

**Rotation et suppression automatique** : un seul code actif à la fois par
(utilisateur, type) — tout code non utilisé est supprimé dès qu'un nouveau est généré.
`OtpCleanupScheduler` purge en tâche de fond (cron configurable, `application.otp.cleanup-cron`,
15 min par défaut) les codes expirés ou déjà utilisés.

**Protection contre le brute-force** : après `application.otp.max-attempts` tentatives
incorrectes (5 par défaut), le code est immédiatement invalidé, même s'il n'a pas expiré.

**Intégration avec l'inscription** : `AuthServiceImpl.register()` déclenche automatiquement
l'envoi d'un OTP `EMAIL_VERIFICATION`. Cet appel est isolé dans sa propre transaction
(`Propagation.REQUIRES_NEW` côté `OtpServiceImpl`) pour qu'un échec d'envoi d'email
(SMTP indisponible, etc.) ne fasse jamais échouer la création du compte.

**Configuration** (voir `.env.example`) :

| Variable                     | Défaut | Description |
|-------------------------------|--------|--------------|
| `OTP_LENGTH`                     | 6      | Nombre de chiffres du code |
| `OTP_EXPIRATION_MINUTES`           | 10     | Durée de validité |
| `OTP_RESEND_COOLDOWN_SECONDS`        | 60     | Délai anti-spam entre deux envois |
| `OTP_MAX_ATTEMPTS`                     | 5      | Tentatives de vérification avant invalidation |
| `OTP_CLEANUP_CRON`                       | `0 */15 * * * *` | Fréquence de purge automatique |

> ⚠️ L'envoi d'email nécessite des identifiants SMTP valides dans `.env`
> (`MAIL_USERNAME` / `MAIL_PASSWORD`). En développement, un service comme
> [Mailtrap](https://mailtrap.io) permet de tester sans envoyer de vrais emails.

---

## Module Authentication

Premier module métier complet : inscription, connexion, rafraîchissement et déconnexion.
Aucun préfixe de version dans les chemins (`/auth/...`, sous le context-path `/api`).

| Méthode | Endpoint              | Description                                      | Auth requise |
|---------|------------------------|---------------------------------------------------|:---:|
| `POST`  | `/api/auth/register`     | Créer un compte (statut initial `PENDING_VERIFICATION`) | Non |
| `POST`  | `/api/auth/login`          | Authentifier et émettre access + refresh token      | Non |
| `POST`  | `/api/auth/refresh-token`    | Échanger un refresh token contre une nouvelle paire (rotation) | Non |
| `POST`  | `/api/auth/logout`             | Révoquer un refresh token                              | Non |

**Rotation des refresh tokens** : chaque appel à `/refresh-token` révoque immédiatement
le token présenté et en émet un nouveau. Le suivi de révocation est assuré par
l'entité `RefreshToken` (table `refresh_tokens`), qui ne stocke jamais le JWT
lui-même — uniquement son identifiant technique (claim JWT standard `jti`).

**Validation croisée** : `RegisterRequest` utilise une annotation de validation
personnalisée `@FieldMatch` (package `validation/`) pour vérifier que `password`
et `confirmPassword` correspondent, avec l'erreur remontée sur le bon champ dans
la réponse JSON.

Toutes les réponses de succès sont enveloppées dans `ApiResponse<T>` et les erreurs
dans `ErrorResponse` (voir `GlobalExceptionHandler`) — format homogène sur tous les
endpoints.

### Tests

```bash
mvnd test        # ou : mvn test
```

- `AuthServiceImplTest` — logique métier (register, login, refresh, logout, rotation) via Mockito
- `AuthControllerTest` — contrat HTTP (statuts, forme JSON, validation) via `@WebMvcTest` + MockMvc

---

## Modèle de données

Le périmètre actuel du modèle relationnel couvre l'authentification et l'identité :
`User`, `Profile`, `OtpCode`, `Role`. Les modules `Match`, `Message`, `Notification`
et `Report` sont hors périmètre de ce socle et seront développés séparément.

| Entité     | Table         | Relation                                              |
|------------|---------------|--------------------------------------------------------|
| `User`     | `users`       | 1-1 avec `Profile`, 1-N avec `OtpCode`, N-N avec `Role` |
| `Profile`  | `profiles`    | 1-1 avec `User` (porte la FK `user_id`, `UNIQUE`)       |
| `OtpCode`  | `otp_codes`   | N-1 avec `User`                                         |
| `Role`     | `roles`       | N-N avec `User` (table de jointure `user_roles`)        |

Le script SQL complet (DDL, contraintes, index, colonne `updated_at` auto mise à
jour, seed des rôles) se trouve dans [`database/schema.sql`](./database/schema.sql)
— version **MySQL** utilisée par défaut. La version d'origine est conservée dans
[`database/schema.postgresql.sql`](./database/schema.postgresql.sql) pour le
chemin Docker/Render.

### Générer le schéma en base

```bash
# MySQL (WampServer) — défaut
mysql -u root < database/schema.sql

# PostgreSQL (Docker/Render uniquement)
psql -U postgres -d campuslink_db -f database/schema.postgresql.sql
```

> En développement, `ddl-auto: update` (profil `dev`) peut aussi générer le schéma
> automatiquement à partir des entités JPA. `database/schema.sql` reste la source
> de vérité pour la production (profil `prod`, `ddl-auto: validate`).

---

## Tests

```bash
mvnd test        # ou : mvn test
```

Les tests utilisent une base **H2 en mémoire** (profil `test`), isolée de la base
applicative (MySQL ou PostgreSQL) : ils ne nécessitent aucun serveur de bases.

---

## Variables d'environnement principales

Voir le fichier [`.env.example`](./.env.example) pour la liste complète.

| Variable            | Description                          |
|----------------------|----------------------------------------|
| `DB_URL`              | URL JDBC — défaut MySQL (`jdbc:mysql://localhost:3306/campuslink_db`) ; PostgreSQL via Docker/Render |
| `DB_DRIVER`            | Driver JDBC (défaut `com.mysql.cj.jdbc.Driver`) |
| `JWT_SECRET`            | Clé secrète de signature des tokens |
| `JWT_ACCESS_EXPIRATION`   | Durée de vie de l'access token (ms) |
| `JWT_REFRESH_EXPIRATION`   | Durée de vie du refresh token (ms) |
| `CLOUDINARY_CLOUD_NAME`      | Identifiant du cloud Cloudinary  |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Identifiants SMTP           |

---

## Déploiement (Render + Neon)

1. Créer une base sur [Neon](https://neon.tech), copier la connection string et **ajouter `?sslmode=require`** à la fin.
2. Sur [Render](https://render.com) : New → Web Service → connecter le repo. Runtime **Docker** (le `Dockerfile` existant est utilisé tel quel).
3. Variables d'environnement à définir dans Render (ne pas définir `PORT`, Render l'injecte automatiquement) :

   | Variable | Valeur |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | connection string Neon (avec `?sslmode=require`) |
   | `DB_USERNAME` / `DB_PASSWORD` | identifiants Neon |
   | `JWT_SECRET` | secret fort dédié à la prod |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | identifiants SMTP |
   | `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | identifiants Cloudinary |
   | `CORS_ALLOWED_ORIGINS` | domaine(s) du frontend en prod |

4. Déployer. Render construit l'image via le `Dockerfile`, lance le conteneur sur le port qu'il fournit via `PORT`
   (déjà pris en compte : `server.port: ${PORT:${SERVER_PORT:8080}}`), et route le trafic HTTPS vers le conteneur
   (`server.forward-headers-strategy: framework` en profil `prod` pour une détection correcte du schéma).
5. `ddl-auto: validate` en prod : exécuter `database/schema.postgresql.sql` sur la base Neon **avant** le premier déploiement.

---

## Feuille de route (prochaines étapes)

1. Modélisation du domaine (entités : Utilisateur, Rôles, etc.)
2. Module d'authentification (inscription, connexion, refresh, mot de passe oublié)
3. Endpoints métier CampusLink (à définir selon le cahier des charges)
4. Tests unitaires et d'intégration
5. CI/CD

---

## Licence

Projet propriétaire — Tous droits réservés.
