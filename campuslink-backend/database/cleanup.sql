-- ============================================================================
-- CampusLink — Purge des données de test / développement (MySQL)
-- ============================================================================
-- Objectif : repartir d'une base vierge exploitable pour le déploiement
-- (aucun compte de test, aucun like/match/message fictif).
--
-- CONSERVÉS :
--   * roles (+ son seed) — obligatoire : sans rôles, l'inscription échoue
--                          (« Rôle STUDENT introuvable »).
--   * testimonials       — contenu éditorial éventuellement saisi par un
--                          administrateur (vider manuellement si besoin :
--                          TRUNCATE TABLE testimonials;).
--
-- USAGE (WampServer, user root sans mot de passe) :
--   C:/wamp/bin/mysql/mysql5.6.17/bin/mysql.exe -u root < database/cleanup.sql
--
-- Idempotent et robuste aux bases plus anciennes : chaque table n'est
-- tronquée que si elle existe (les tables nouvelles comme contact_messages
-- sont créées par database/schema.sql ou par Hibernate ddl-auto:update).
-- ============================================================================

USE campuslink_db;

-- Troncature conditionnelle : une table absente est simplement ignorée,
-- le script n'échoue jamais sur une base non encore à jour.
DROP PROCEDURE IF EXISTS campuslink_truncate_if_exists;
DELIMITER //
CREATE PROCEDURE campuslink_truncate_if_exists(IN tbl VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.tables
               WHERE table_schema = DATABASE()
                 AND table_name = tbl) THEN
        SET @sql = CONCAT('TRUNCATE TABLE `', tbl, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- Les FK sont tronquées sans tenir compte de l'ordre : désactivation
-- temporaire des contraintes (elles reprennent ensuite intactes).
SET FOREIGN_KEY_CHECKS = 0;

-- Données des visiteurs / comptes de test
CALL campuslink_truncate_if_exists('contact_messages');
CALL campuslink_truncate_if_exists('notifications');
CALL campuslink_truncate_if_exists('password_reset_tokens');
CALL campuslink_truncate_if_exists('otp_codes');
CALL campuslink_truncate_if_exists('refresh_tokens');

-- Module temps réel (likes, matchs, messages)
CALL campuslink_truncate_if_exists('messages');
CALL campuslink_truncate_if_exists('matches');
CALL campuslink_truncate_if_exists('likes');

-- Comptes et profils de test (y compris les orphelins des tests interrompus)
CALL campuslink_truncate_if_exists('profile_interests');
CALL campuslink_truncate_if_exists('profiles');
CALL campuslink_truncate_if_exists('user_roles');
CALL campuslink_truncate_if_exists('users');

-- Modération
CALL campuslink_truncate_if_exists('reports');
CALL campuslink_truncate_if_exists('moderation_logs');

SET FOREIGN_KEY_CHECKS = 1;

DROP PROCEDURE IF EXISTS campuslink_truncate_if_exists;

-- ============================ Vérification ================================
-- Résultat attendu : 3 rôles (ADMIN, STUDENT, TEACHER), 0 utilisateur,
-- 0 profil. `legacy_id` des utilisateurs repart à 1 (auto-incrément réinitialisé).
SELECT (SELECT COUNT(*) FROM roles)    AS roles_restants,
       (SELECT COUNT(*) FROM users)    AS utilisateurs,
       (SELECT COUNT(*) FROM profiles) AS profils;
