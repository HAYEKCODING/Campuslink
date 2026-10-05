-- ============================================================================
--  CampusLink — Script de création du schéma PostgreSQL
--  Périmètre : User, Profile, OtpCode, Role (+ table de jointure user_roles)
--  Compatible PostgreSQL 14+
-- ============================================================================
-- Ce script est le miroir exact du mapping JPA défini dans le package
-- com.campuslink.entity. Il peut être exécuté tel quel sur une base vierge.
-- ============================================================================

BEGIN;

-- ============================================================================
-- Extensions
-- ============================================================================
-- Nécessaire pour gen_random_uuid() utilisé comme valeur par défaut des clés
-- primaires (Hibernate génère également les UUID côté application, cette
-- valeur par défaut sert surtout aux insertions SQL manuelles / scripts de seed).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- TABLE : roles
-- ============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(30)  NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('STUDENT', 'TEACHER', 'ADMIN'))
);

COMMENT ON TABLE roles IS 'Rôles applicatifs attribuables aux utilisateurs (RBAC)';

CREATE INDEX IF NOT EXISTS idx_roles_name ON roles (name);

-- ============================================================================
-- TABLE : users
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(180) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    legacy_id       BIGSERIAL    UNIQUE, -- pont vers le module realtime (DevC), FK BIGINT
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BANNED', 'DEACTIVATED')
    )
);

COMMENT ON TABLE users IS 'Comptes utilisateurs — entité centrale d''authentification';
COMMENT ON COLUMN users.password IS 'Hash BCrypt du mot de passe — jamais stocké en clair';

CREATE INDEX IF NOT EXISTS idx_users_email  ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

-- ============================================================================
-- TABLE : profiles
-- ============================================================================
CREATE TABLE IF NOT EXISTS profiles (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    date_of_birth     DATE,
    gender            VARCHAR(30),
    bio               VARCHAR(1000),
    avatar_url        VARCHAR(500),
    phone_number      VARCHAR(20),
    university        VARCHAR(150),
    field_of_study    VARCHAR(150),
    graduation_year   INTEGER,
    city              VARCHAR(100),
    neighborhood      VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_profiles_user_id      UNIQUE (user_id),
    CONSTRAINT uk_profiles_phone_number UNIQUE (phone_number),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_profiles_gender CHECK (
        gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')
    ),
    CONSTRAINT ck_profiles_graduation_year CHECK (
        graduation_year IS NULL OR graduation_year BETWEEN 1900 AND 2100
    ),
    CONSTRAINT ck_profiles_phone_format CHECK (
        phone_number IS NULL OR phone_number ~ '^\+?[0-9]{8,15}$'
    ),
    CONSTRAINT ck_profiles_date_of_birth_past CHECK (
        date_of_birth IS NULL OR date_of_birth < CURRENT_DATE
    )
);

-- Filet de sécurité idempotent pour une base déjà provisionnée avant l'ajout
-- de cette colonne (une exécution sur schéma neuf la trouve déjà déclarée ci-dessus).
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(100);

COMMENT ON TABLE profiles IS 'Informations personnelles et académiques (1-1 avec users)';

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles (user_id);

-- ============================================================================
-- TABLE : profile_interests
-- ============================================================================
-- Table de collection (@ElementCollection JPA) : pas d'entité dédiée, un
-- centre d'intérêt n'est qu'une chaîne rattachée à un profil.
CREATE TABLE IF NOT EXISTS profile_interests (
    profile_id  UUID        NOT NULL,
    interest    VARCHAR(50) NOT NULL,

    CONSTRAINT uk_profile_interests UNIQUE (profile_id, interest),
    CONSTRAINT fk_profile_interests_profile FOREIGN KEY (profile_id)
        REFERENCES profiles (id) ON DELETE CASCADE
);

COMMENT ON TABLE profile_interests IS 'Centres d''intérêt libres associés à un profil';

CREATE INDEX IF NOT EXISTS idx_profile_interests_profile_id ON profile_interests (profile_id);

-- ============================================================================
-- TABLE : otp_codes
-- ============================================================================
CREATE TABLE IF NOT EXISTS otp_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    code          VARCHAR(8)   NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used          BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts      INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_otp_codes_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_otp_codes_type CHECK (
        type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'PHONE_VERIFICATION', 'TWO_FACTOR_AUTH')
    ),
    CONSTRAINT ck_otp_codes_code_format CHECK (code ~ '^[0-9]{4,8}$'),
    CONSTRAINT ck_otp_codes_attempts_positive CHECK (attempts >= 0)
);

COMMENT ON TABLE otp_codes IS 'Codes à usage unique (vérification email/téléphone, reset mot de passe, 2FA)';

CREATE INDEX IF NOT EXISTS idx_otp_codes_user_id         ON otp_codes (user_id);
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_type_used  ON otp_codes (user_id, type, used);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at      ON otp_codes (expires_at);

-- ============================================================================
-- TABLE DE JOINTURE : user_roles (Many-to-Many entre users et roles)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id   UUID NOT NULL,
    role_id   UUID NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE
);

COMMENT ON TABLE user_roles IS 'Table de jointure Many-to-Many entre users et roles';

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles (role_id);

-- ============================================================================
-- TABLE : refresh_tokens
-- ============================================================================
-- Ne stocke jamais le JWT lui-même : uniquement l'identifiant technique (id),
-- embarqué comme claim standard "jti" dans le refresh token signé côté
-- application. Permet la révocation (logout) et la rotation (refresh).
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE refresh_tokens IS 'Suivi de révocation des refresh tokens (aucun JWT stocké)';

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ============================================================================
-- TABLE : password_reset_tokens
-- ============================================================================
-- Ne stocke jamais le token en clair : uniquement son empreinte SHA-256
-- (64 caractères hexadécimaux). Le token brut (32 octets aléatoires encodés
-- en base64url) n'existe que dans l'email envoyé au client.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE password_reset_tokens IS 'Liens de réinitialisation de mot de passe (aucun token en clair stocké)';

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id    ON password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);

-- ============================================================================
-- TRIGGERS : mise à jour automatique de updated_at
-- ============================================================================
-- Filet de sécurité complémentaire à l'audit applicatif Hibernate
-- (@LastModifiedDate) pour toute modification effectuée en SQL direct.

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_roles_updated_at ON roles;
CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_otp_codes_updated_at ON otp_codes;
CREATE TRIGGER trg_otp_codes_updated_at
    BEFORE UPDATE ON otp_codes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_refresh_tokens_updated_at ON refresh_tokens;
CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_password_reset_tokens_updated_at ON password_reset_tokens;
CREATE TRIGGER trg_password_reset_tokens_updated_at
    BEFORE UPDATE ON password_reset_tokens
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- SEED : rôles applicatifs de référence
-- ============================================================================
INSERT INTO roles (name, description) VALUES
    ('STUDENT', 'Étudiant inscrit sur la plateforme CampusLink'),
    ('TEACHER', 'Enseignant ou encadrant académique'),
    ('ADMIN',   'Administrateur de la plateforme')
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- ============================================================================
-- MODULE TEMPS RÉEL (DevC) — likes, matches, messages, notifications,
-- reports, moderation_logs. Référencent users via legacy_id (BIGINT),
-- pont ajouté sur la table users pour ce module (voir plus haut).
-- ============================================================================

CREATE TABLE IF NOT EXISTS likes (
    id            BIGSERIAL PRIMARY KEY,
    emetteur_id   BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    cible_id      BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    date_action   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_like_emetteur_cible UNIQUE (emetteur_id, cible_id),
    CONSTRAINT ck_like_pas_soi_meme CHECK (emetteur_id <> cible_id)
);
CREATE INDEX IF NOT EXISTS idx_like_emetteur ON likes(emetteur_id);
CREATE INDEX IF NOT EXISTS idx_like_cible    ON likes(cible_id);

CREATE TABLE IF NOT EXISTS matches (
    id               BIGSERIAL PRIMARY KEY,
    utilisateur1_id  BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    utilisateur2_id  BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    date_match       TIMESTAMP NOT NULL DEFAULT now(),
    statut           VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    date_rupture     TIMESTAMP,
    CONSTRAINT uk_match_utilisateurs UNIQUE (utilisateur1_id, utilisateur2_id),
    CONSTRAINT ck_match_statut CHECK (statut IN ('ACTIF', 'ROMPU')),
    CONSTRAINT ck_match_pas_soi_meme CHECK (utilisateur1_id <> utilisateur2_id)
);
CREATE INDEX IF NOT EXISTS idx_match_utilisateur1 ON matches(utilisateur1_id);
CREATE INDEX IF NOT EXISTS idx_match_utilisateur2 ON matches(utilisateur2_id);
CREATE INDEX IF NOT EXISTS idx_match_statut       ON matches(statut);

CREATE TABLE IF NOT EXISTS messages (
    id               BIGSERIAL PRIMARY KEY,
    match_id         BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    expediteur_id    BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    contenu          TEXT NOT NULL,
    date_envoi       TIMESTAMP NOT NULL DEFAULT now(),
    statut_lecture   VARCHAR(20) NOT NULL DEFAULT 'ENVOYE',
    date_lecture     TIMESTAMP,
    CONSTRAINT ck_message_statut CHECK (statut_lecture IN ('ENVOYE', 'RECU', 'LU'))
);
CREATE INDEX IF NOT EXISTS idx_message_match       ON messages(match_id);
CREATE INDEX IF NOT EXISTS idx_message_expediteur  ON messages(expediteur_id);
CREATE INDEX IF NOT EXISTS idx_message_date_envoi  ON messages(date_envoi);

CREATE TABLE IF NOT EXISTS notifications (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    type           VARCHAR(20) NOT NULL,
    contenu        VARCHAR(500) NOT NULL,
    reference_id   BIGINT,
    lu             BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_type CHECK (type IN ('LIKE', 'MATCH', 'MESSAGE', 'SIGNALEMENT'))
);
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_lu   ON notifications(lu);

CREATE TABLE IF NOT EXISTS reports (
    id               BIGSERIAL PRIMARY KEY,
    emetteur_id      BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    cible_id         BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    match_id         BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    motif            VARCHAR(100) NOT NULL,
    description      VARCHAR(1000),
    statut           VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_creation    TIMESTAMP NOT NULL DEFAULT now(),
    date_traitement  TIMESTAMP,
    CONSTRAINT ck_report_statut CHECK (statut IN ('EN_ATTENTE', 'EN_COURS', 'RESOLU', 'REJETE', 'ARCHIVE'))
);
CREATE INDEX IF NOT EXISTS idx_report_cible    ON reports(cible_id);
CREATE INDEX IF NOT EXISTS idx_report_emetteur ON reports(emetteur_id);
CREATE INDEX IF NOT EXISTS idx_report_statut   ON reports(statut);

CREATE TABLE IF NOT EXISTS moderation_logs (
    id                    BIGSERIAL PRIMARY KEY,
    utilisateur_cible_id  BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    moderateur_id         BIGINT NOT NULL REFERENCES users(legacy_id) ON DELETE CASCADE,
    action                VARCHAR(20) NOT NULL,
    motif                 VARCHAR(500),
    report_id             BIGINT REFERENCES reports(id) ON DELETE SET NULL,
    date_action           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_moderation_action CHECK (action IN ('AVERTISSEMENT', 'SUSPENSION', 'BANNISSEMENT', 'DEBANNISSEMENT'))
);
CREATE INDEX IF NOT EXISTS idx_moderationlog_cible       ON moderation_logs(utilisateur_cible_id);
CREATE INDEX IF NOT EXISTS idx_moderationlog_moderateur  ON moderation_logs(moderateur_id);
