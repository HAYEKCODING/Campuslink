-- ============================================================================
--  CampusLink — Script de création du schéma MySQL
--  Périmètre : roles, users, profiles, profile_interests, otp_codes,
--              user_roles, refresh_tokens, password_reset_tokens
--              + module temps réel (likes, matches, messages, notifications,
--                reports, moderation_logs)
--  Compatible MySQL 5.6+ / 8.x (WampServer), charset utf8mb4
-- ============================================================================
-- Miroir exact du mapping JPA défini dans les packages
-- com.campuslink.entity et com.campuslink.realtime.entity.
--
-- Utilisation (depuis la racine du backend) :
--     mysql -u root < database/schema.sql
-- ou, depuis phpMyAdmin : onglet « Importer » et choisir ce fichier.
--
-- Le script crée la base si elle n'existe pas puis la sélectionne :
-- inutile d'exécuter CREATE DATABASE à la main. Adapter le nom de la base
-- (campuslink_db) si DB_NAME est différent.
--
-- Équivalences avec l'ancien script PostgreSQL (database/schema.postgresql.sql) :--   UUID          -> BINARY(16)    (Hibernate 6 mappe UUID vers binary(16) sur
--                                   MySQL ; valeurs générées côté application,
--                                   GenerationType.UUID)
--   TIMESTAMPTZ   -> DATETIME(6)
--   BIGSERIAL     -> BIGINT AUTO_INCREMENT
--   BOOLEAN       -> TINYINT(1)
--   DEFAULT now() -> DEFAULT CURRENT_TIMESTAMP(6)
--   trigger set_updated_at() -> colonne updated_at
--                         DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
--   ON CONFLICT (name) DO NOTHING -> INSERT IGNORE
--   Extensions pgcrypto / BEGIN-COMMIT / COMMENT ON : sans équivalent, retirés
--
-- Les contraintes CHECK sont ignorées par MySQL < 8.0.16 (mais valides
-- syntaxiquement) et appliquées telles quelles par MySQL 8.0.16+.
-- Exécuter sur une base vierge : les CREATE INDEX n'existent pas en
-- « IF NOT EXISTS » sur MySQL (un second jeu échouerait sur un index dupliqué).
-- ============================================================================

CREATE DATABASE IF NOT EXISTS campuslink_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE campuslink_db;

-- Index conditionnels : MySQL 5.6/8 n'accepte pas `CREATE INDEX IF NOT EXISTS`,
-- le script doit donc rester ré-exécutable sur une base déjà provisionnée
-- (les CREATE TABLE utilisent déjà IF NOT EXISTS).
DROP PROCEDURE IF EXISTS campuslink_create_index;
DELIMITER //
CREATE PROCEDURE campuslink_create_index(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = tbl
                     AND index_name = idx) THEN
        SET @sql = CONCAT('CREATE INDEX ', idx, ' ON ', tbl, ' (', cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- ============================================================================
-- TABLE : roles
-- ============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id              BINARY(16)     PRIMARY KEY,
    name            VARCHAR(30)  NOT NULL,
    description     VARCHAR(255),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                     ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('STUDENT', 'TEACHER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Rôles applicatifs attribuables aux utilisateurs (RBAC)';

CALL campuslink_create_index('roles', 'idx_roles_name', 'name');

-- ============================================================================
-- TABLE : users
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BINARY(16)     PRIMARY KEY,
    email           VARCHAR(180) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified  TINYINT(1)   NOT NULL DEFAULT 0,
    -- pont vers le module realtime (DevC), FK BIGINT — auto incrémenté à
    -- l'insertion comme le faisait BIGSERIAL côté PostgreSQL.
    legacy_id       BIGINT       NOT NULL AUTO_INCREMENT,
    last_login_at   DATETIME(6),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                     ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_legacy_id UNIQUE (legacy_id),
    CONSTRAINT ck_users_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BANNED', 'DEACTIVATED')
    )
    -- password : hash BCrypt du mot de passe — jamais stocké en clair
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Comptes utilisateurs — entité centrale d''authentification';

CALL campuslink_create_index('users', 'idx_users_email', 'email');
CALL campuslink_create_index('users', 'idx_users_status', 'status');

-- ============================================================================
-- TABLE : profiles
-- ============================================================================
CREATE TABLE IF NOT EXISTS profiles (
    id                BINARY(16)     PRIMARY KEY,
    user_id           BINARY(16)     NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    date_of_birth     DATE,
    gender            VARCHAR(30),
    level             VARCHAR(20),
    bio               VARCHAR(1000),
    avatar_url        VARCHAR(500),
    phone_number      VARCHAR(20),
    university        VARCHAR(150),
    field_of_study    VARCHAR(150),
    graduation_year   INTEGER,
    city              VARCHAR(100),
    neighborhood      VARCHAR(100),
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                       ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_profiles_user_id      UNIQUE (user_id),
    CONSTRAINT uk_profiles_phone_number UNIQUE (phone_number),
    CONSTRAINT ck_profiles_gender CHECK (
        gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')
    ),
    CONSTRAINT ck_profiles_level CHECK (
        level IS NULL OR level IN ('LICENCE', 'MASTER', 'DOCTORAT')
    ),
    CONSTRAINT ck_profiles_graduation_year CHECK (
        graduation_year IS NULL OR graduation_year BETWEEN 1900 AND 2100
    ),
    CONSTRAINT ck_profiles_phone_format CHECK (
        phone_number IS NULL OR phone_number REGEXP '^\\+?[0-9]{8,15}$'
    ),
    -- Pas de CHECK « date_of_birth < CURRENT_DATE » : CURRENT_DATE serait rejetée
    -- par MySQL 8.0.16+ (fonction non déterministe). Contrôlée côté application.
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Informations personnelles et académiques (1-1 avec users)';

CALL campuslink_create_index('profiles', 'idx_profiles_user_id', 'user_id');

-- ============================================================================
-- TABLE : profile_interests
-- ============================================================================
-- Table de collection (@ElementCollection JPA) : pas d'entité dédiée, un
-- centre d'intérêt n'est qu'une chaîne rattachée à un profil.
CREATE TABLE IF NOT EXISTS profile_interests (
    profile_id  BINARY(16)     NOT NULL,
    interest    VARCHAR(50)  NOT NULL,

    CONSTRAINT uk_profile_interests UNIQUE (profile_id, interest),
    CONSTRAINT fk_profile_interests_profile FOREIGN KEY (profile_id)
        REFERENCES profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Centres d''intérêt libres associés à un profil';

CALL campuslink_create_index('profile_interests', 'idx_profile_interests_profile_id', 'profile_id');

-- ============================================================================
-- TABLE : otp_codes
-- ============================================================================
CREATE TABLE IF NOT EXISTS otp_codes (
    id            BINARY(16)    PRIMARY KEY,
    user_id       BINARY(16)    NOT NULL,
    code          VARCHAR(8)  NOT NULL,
    type          VARCHAR(30) NOT NULL,
    expires_at    DATETIME(6) NOT NULL,
    used          TINYINT(1)  NOT NULL DEFAULT 0,
    attempts      INTEGER     NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_otp_codes_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_otp_codes_type CHECK (
        type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'PHONE_VERIFICATION', 'TWO_FACTOR_AUTH')
    ),
    CONSTRAINT ck_otp_codes_code_format CHECK (code REGEXP '^[0-9]{4,8}$'),
    CONSTRAINT ck_otp_codes_attempts_positive CHECK (attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Codes à usage unique (vérification email/téléphone, reset mot de passe, 2FA)';

CALL campuslink_create_index('otp_codes', 'idx_otp_codes_user_id', 'user_id');
CALL campuslink_create_index('otp_codes', 'idx_otp_codes_user_type_used', 'user_id, type, used');
CALL campuslink_create_index('otp_codes', 'idx_otp_codes_expires_at', 'expires_at');

-- ============================================================================
-- TABLE DE JOINTURE : user_roles (Many-to-Many entre users et roles)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id   BINARY(16) NOT NULL,
    role_id   BINARY(16) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Table de jointure Many-to-Many entre users et roles';

CALL campuslink_create_index('user_roles', 'idx_user_roles_user_id', 'user_id');
CALL campuslink_create_index('user_roles', 'idx_user_roles_role_id', 'role_id');

-- ============================================================================
-- TABLE : refresh_tokens
-- ============================================================================
-- Ne stocke jamais le JWT lui-même : uniquement l'identifiant technique (id),
-- embarqué comme claim standard "jti" dans le refresh token signé côté
-- application. Permet la révocation (logout) et la rotation (refresh).
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            BINARY(16)    PRIMARY KEY,
    user_id       BINARY(16)    NOT NULL,
    expires_at    DATETIME(6) NOT NULL,
    revoked       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Suivi de révocation des refresh tokens (aucun JWT stocké)';

CALL campuslink_create_index('refresh_tokens', 'idx_refresh_tokens_user_id', 'user_id');
CALL campuslink_create_index('refresh_tokens', 'idx_refresh_tokens_expires_at', 'expires_at');

-- ============================================================================
-- TABLE : password_reset_tokens
-- ============================================================================
-- Ne stocke jamais le token en clair : uniquement son empreinte SHA-256
-- (64 caractères hexadécimaux). Le token brut (32 octets aléatoires encodé
-- en base64url) n'existe que dans l'email envoyé au client.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id            BINARY(16)    PRIMARY KEY,
    user_id       BINARY(16)    NOT NULL,
    token_hash    VARCHAR(64) NOT NULL,
    expires_at    DATETIME(6) NOT NULL,
    used          TINYINT(1)  NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Liens de réinitialisation de mot de passe (aucun token en clair stocké)';

CALL campuslink_create_index('password_reset_tokens', 'idx_password_reset_tokens_user_id', 'user_id');
CALL campuslink_create_index('password_reset_tokens', 'idx_password_reset_tokens_expires_at', 'expires_at');

-- ============================================================================
-- SEED : rôles applicatifs de référence
-- ============================================================================
-- Indispensable : l'inscription (AuthServiceImpl) échoue si le rôle STUDENT
-- est absent. INSERT IGNORE = équivalent de ON CONFLICT DO NOTHING (idempotent).
INSERT IGNORE INTO roles (id, name, description) VALUES
    (UNHEX(REPLACE(UUID(), '-', '')), 'STUDENT', 'Étudiant inscrit sur la plateforme CampusLink'),
    (UNHEX(REPLACE(UUID(), '-', '')), 'TEACHER', 'Enseignant ou encadrant académique'),
    (UNHEX(REPLACE(UUID(), '-', '')), 'ADMIN',   'Administrateur de la plateforme');

-- ============================================================================
-- MODULE TEMPS RÉEL (DevC) — likes, matches, messages, notifications,
-- reports, moderation_logs. Référencent users via legacy_id (BIGINT),
-- pont ajouté sur la table users pour ce module (voir plus haut).
-- ============================================================================

CREATE TABLE IF NOT EXISTS likes (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    emetteur_id   BIGINT        NOT NULL,
    cible_id      BIGINT        NOT NULL,
    date_action   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_like_emetteur_cible UNIQUE (emetteur_id, cible_id),
    -- Pas de CHECK « emetteur_id <> cible_id » : MySQL 8.0.16+ interdit tout CHECK
    -- sur une colonne portant une action de clé étrangère (ON DELETE ici).
    -- La règle « ne pas s'aimer soi-même » appartient au service métier.
    CONSTRAINT fk_like_emetteur FOREIGN KEY (emetteur_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_like_cible FOREIGN KEY (cible_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('likes', 'idx_like_emetteur', 'emetteur_id');
CALL campuslink_create_index('likes', 'idx_like_cible', 'cible_id');

CREATE TABLE IF NOT EXISTS matches (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    utilisateur1_id  BIGINT       NOT NULL,
    utilisateur2_id  BIGINT       NOT NULL,
    date_match       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    statut           VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    date_rupture     DATETIME(6),
    CONSTRAINT uk_match_utilisateurs UNIQUE (utilisateur1_id, utilisateur2_id),
    CONSTRAINT ck_match_statut CHECK (statut IN ('ACTIF', 'ROMPU')),
    -- idem : pas de CHECK sur utilisateur1_id/utilisateur2_id (colonnes FK).
    CONSTRAINT fk_match_utilisateur1 FOREIGN KEY (utilisateur1_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_utilisateur2 FOREIGN KEY (utilisateur2_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('matches', 'idx_match_utilisateur1', 'utilisateur1_id');
CALL campuslink_create_index('matches', 'idx_match_utilisateur2', 'utilisateur2_id');
CALL campuslink_create_index('matches', 'idx_match_statut', 'statut');

CREATE TABLE IF NOT EXISTS messages (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    match_id         BIGINT       NOT NULL,
    expediteur_id    BIGINT       NOT NULL,
    contenu          TEXT         NOT NULL,
    date_envoi       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    statut_lecture   VARCHAR(20)  NOT NULL DEFAULT 'ENVOYE',
    date_lecture     DATETIME(6),
    CONSTRAINT ck_message_statut CHECK (statut_lecture IN ('ENVOYE', 'RECU', 'LU')),
    CONSTRAINT fk_message_match FOREIGN KEY (match_id)
        REFERENCES matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_expediteur FOREIGN KEY (expediteur_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('messages', 'idx_message_match', 'match_id');
CALL campuslink_create_index('messages', 'idx_message_expediteur', 'expediteur_id');
CALL campuslink_create_index('messages', 'idx_message_date_envoi', 'date_envoi');

CREATE TABLE IF NOT EXISTS notifications (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    contenu        VARCHAR(500) NOT NULL,
    reference_id   BIGINT,
    lu             TINYINT(1)   NOT NULL DEFAULT 0,
    date_creation  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_notification_type CHECK (type IN ('LIKE', 'MATCH', 'MESSAGE', 'SIGNALEMENT')),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('notifications', 'idx_notification_user', 'user_id');
CALL campuslink_create_index('notifications', 'idx_notification_lu', 'lu');

CREATE TABLE IF NOT EXISTS reports (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    emetteur_id      BIGINT       NOT NULL,
    cible_id         BIGINT       NOT NULL,
    match_id         BIGINT,
    motif            VARCHAR(100) NOT NULL,
    description      VARCHAR(1000),
    statut           VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
    date_creation    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    date_traitement  DATETIME(6),
    CONSTRAINT ck_report_statut CHECK (statut IN ('EN_ATTENTE', 'EN_COURS', 'RESOLU', 'REJETE', 'ARCHIVE')),
    CONSTRAINT fk_report_emetteur FOREIGN KEY (emetteur_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_report_cible FOREIGN KEY (cible_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_report_match FOREIGN KEY (match_id)
        REFERENCES matches (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('reports', 'idx_report_cible', 'cible_id');
CALL campuslink_create_index('reports', 'idx_report_emetteur', 'emetteur_id');
CALL campuslink_create_index('reports', 'idx_report_statut', 'statut');

CREATE TABLE IF NOT EXISTS moderation_logs (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    utilisateur_cible_id  BIGINT       NOT NULL,
    moderateur_id         BIGINT       NOT NULL,
    action                VARCHAR(20)  NOT NULL,
    motif                 VARCHAR(500),
    report_id             BIGINT,
    date_action           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_moderation_action CHECK (action IN ('AVERTISSEMENT', 'SUSPENSION', 'BANNISSEMENT', 'DEBANNISSEMENT')),
    CONSTRAINT fk_moderation_cible FOREIGN KEY (utilisateur_cible_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_moderateur FOREIGN KEY (moderateur_id)
        REFERENCES users (legacy_id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_report FOREIGN KEY (report_id)
        REFERENCES reports (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CALL campuslink_create_index('moderation_logs', 'idx_moderationlog_cible', 'utilisateur_cible_id');
CALL campuslink_create_index('moderation_logs', 'idx_moderationlog_moderateur', 'moderateur_id');

-- ============================================================================
-- TABLE : testimonials  (GET /testimonials — landing page, endpoint public)
-- ============================================================================
-- Contenu éditorial géré côté administration ; `active` permet de masquer un
-- témoignage sans le supprimer, `display_order` contrôle l'ordre d'affichage.
CREATE TABLE IF NOT EXISTS testimonials (
    id             BINARY(16)     PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    role           VARCHAR(100),
    photo          VARCHAR(500),
    quote          VARCHAR(1000) NOT NULL,
    display_order  INTEGER      NOT NULL DEFAULT 0,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Témoignages affichés sur la landing page';

CALL campuslink_create_index('testimonials', 'idx_testimonials_active_order', 'active, display_order');

-- ============================================================================
-- TABLE : contact_messages  (POST /contact — file d'attente publique)
-- ============================================================================
-- Champs alignés sur ContactRequest (name, email, message). `handled` signale
-- qu'un administrateur a traité le message (réponse envoyée).
CREATE TABLE IF NOT EXISTS contact_messages (
    id          BINARY(16)     PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(180) NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    handled     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Messages du formulaire de contact de la landing page';

CALL campuslink_create_index('contact_messages', 'idx_contact_messages_created_at', 'created_at');
CALL campuslink_create_index('contact_messages', 'idx_contact_messages_handled', 'handled');

DROP PROCEDURE IF EXISTS campuslink_create_index;
