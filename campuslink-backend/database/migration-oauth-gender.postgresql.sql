BEGIN;

CREATE TABLE IF NOT EXISTS oauth_identities (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider          VARCHAR(20)  NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    user_id           UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_oauth_identity_provider_subject UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_oauth_identity_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_oauth_identity_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_oauth_identity_user_id ON oauth_identities (user_id);

CREATE OR REPLACE FUNCTION campuslink_restrict_new_profile_gender()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.gender IS NOT NULL
       AND NEW.gender NOT IN ('MALE', 'FEMALE')
       AND (TG_OP = 'INSERT' OR NEW.gender IS DISTINCT FROM OLD.gender) THEN
        RAISE EXCEPTION 'gender must be MALE or FEMALE'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_gender_values ON profiles;
CREATE TRIGGER trg_profiles_gender_values
    BEFORE INSERT OR UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION campuslink_restrict_new_profile_gender();

COMMIT;
