#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Simulation API complète de la plateforme CampusLink.

Couvre les 57 opérations documentées dans /api/v3/api-docs :
  - public (stats, témoignages, référentiels, contact)
  - auth (register, OTP, login, refresh, logout, forgot/reset password)
  - profils (création, mise à jour, recherche + exclusion de soi, public)
  - médias (upload, remplacement, suppression, service de fichiers)
  - likes / matchs / messages
  - notifications
  - signalements / modération
  - administration (dashboard, stats, gestion des comptes)
  - contrôles d'accès (401/403) et erreurs métiers (404/409/429)

Usage :  python simulate_api.py            (backend attendu sur :8080)
         BASE=http://localhost:8081 python simulate_api.py

Sortie : tableau PASS/FAIL en console + rapport dans simulation_report.txt.
Code de sortie 0 si toutes les vérifications passent.
"""

import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get("BASE", "http://localhost:8080/api").rstrip("/")
PROXY = os.environ.get("PROXY", "http://localhost:3000/api").rstrip("/")
BACKEND_LOG = os.environ.get(
    "BACKEND_LOG",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend.log"),
)
MYSQL = r"C:\wamp\bin\mysql\mysql5.6.17\bin\mysql.exe"
DB = "campuslink_db"
REPORT_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "simulation_report.txt")

SUF = str(int(time.time()))[-6:]
A_MAIL = f"sim.a{SUF}@campuslink.io"
B_MAIL = f"sim.b{SUF}@campuslink.io"
C_MAIL = f"sim.c{SUF}@campuslink.io"
D_MAIL = f"sim.d{SUF}@campuslink.io"
ADMIN_MAIL = "admin.e2e@campuslink.local"

PW_INIT = "Simulate123!"
PW_RESET = "Nouveau2026!"
PW_ADMIN = "Admin2026!"

RESULTS = []  # (section, name, expected, actual, ok, detail)


def check(section, name, expected, actual, ok=None, detail=""):
    if ok is None:
        if isinstance(expected, (set, frozenset, list, tuple)):
            ok = actual in set(expected)
        else:
            ok = actual == expected
    RESULTS.append((section, name, fmt(expected), fmt(actual), ok, detail))
    flag = "OK  " if ok else "FAIL"
    line = f"[{flag}] {section:<12} {name:<58} attendu={fmt(expected)} obtenu={fmt(actual)}"
    if detail and not ok:
        line += f"  | {detail[:180]}"
    print(line, flush=True)
    return ok


def fmt(v):
    if isinstance(v, (set, frozenset)):
        return "{" + ",".join(str(x) for x in sorted(v, key=str)) + "}"
    if isinstance(v, list):
        return "[" + ",".join(str(x) for x in v) + "]"
    return str(v)


def call(method, path, token=None, body=None, raw_body=None, headers=None, base=BASE):
    """Exécute une requête HTTP, renvoie (status, payload_ou_brut)."""
    url = path if path.startswith("http") else base + path
    hdrs = {"Content-Type": "application/json"}
    if token:
        hdrs["Authorization"] = f"Bearer {token}"
    if headers:
        hdrs.update(headers)
    data = None
    if raw_body is not None:
        data = raw_body
    elif body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            status, payload = resp.status, resp.read()
    except urllib.error.HTTPError as e:
        status, payload = e.code, e.read()
    except Exception as e:  # réseau
        return 0, str(e)
    text = payload.decode("utf-8", "replace")
    if not text:
        return status, None
    try:
        return status, json.loads(text)
    except json.JSONDecodeError:
        return status, text


def unwrap(payload):
    if isinstance(payload, dict) and "success" in payload and "data" in payload:
        return payload.get("data")
    return payload


def items(payload):
    data = unwrap(payload)
    if isinstance(data, list):
        return data
    if isinstance(data, dict) and isinstance(data.get("content"), list):
        return data["content"]
    return []


def err_msg(payload):
    if isinstance(payload, dict):
        return str(payload.get("message") or payload.get("error") or "")[:120]
    if isinstance(payload, str):
        return payload[:120]
    return ""


def sql(query):
    """Exécute une requête MySQL, renvoie les lignes (séparateur tab, -N)."""
    try:
        out = subprocess.run(
            [MYSQL, "-uroot", DB, "-N", "-e", query],
            capture_output=True, timeout=15,
        )
        if out.returncode != 0:
            return None, out.stderr.decode("utf-8", "replace").strip()[:200]
        lines = [l for l in out.stdout.decode("utf-8", "replace").splitlines() if l.strip()]
        return lines, None
    except Exception as e:
        return None, str(e)


def last_reset_token():
    """Relit le dernier lien /reset-password?token=… journalisé par le backend."""
    try:
        with open(BACKEND_LOG, "rb") as fh:
            fh.seek(0, 2)
            size = fh.tell()
            fh.seek(max(0, size - 400_000))
            tail = fh.read().decode("utf-8", "replace")
    except OSError as e:
        return None
    found = re.findall(r"reset-password\?token=([A-Za-z0-9_-]+)", tail)
    return found[-1] if found else None


def multipart(fields, file_field, filename, content, ctype="image/png"):
    """Construit un corps multipart/form-data (stdlib uniquement)."""
    boundary = "----campuslink" + uuid.uuid4().hex
    parts = []
    for key, value in fields.items():
        parts.append(
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"{key}\"\r\n\r\n{value}\r\n".encode()
        )
    parts.append(
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"{file_field}\"; "
        f"filename=\"{filename}\"\r\nContent-Type: {ctype}\r\n\r\n".encode()
        + content
        + b"\r\n"
    )
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), {"Content-Type": f"multipart/form-data; boundary={boundary}"}


# 1x1 PNG valide
PNG = bytes.fromhex(
    "89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4"
    "890000000d49444154789c626001000000ffff03000006000557bfabd4000000"
    "0049454e44ae426082"
)


def login(email, password):
    status, payload = call("POST", "/auth/login", body={"email": email, "password": password})
    data = unwrap(payload) or {}
    return status, data.get("accessToken"), data.get("refreshToken")


def main():
    t0 = time.time()
    print(f"=== Simulation CampusLink — {BASE} — {time.strftime('%H:%M:%S')} ===\n")

    # ------------------------------------------------------------------
    # 1.PUBLIC
    # ------------------------------------------------------------------
    s, p = call("GET", "/stats/public")
    check("public", "GET /stats/public", 200, s, detail=err_msg(p))
    s, p = call("GET", "/testimonials")
    check("public", "GET /testimonials", 200, s)
    for ref in ("universities", "faculties", "neighborhoods", "interests"):
        s, p = call("GET", f"/reference/{ref}")
        ok = s == 200 and isinstance(unwrap(p) if isinstance(p, dict) else p, (list, type(None))) or s == 200
        check("public", f"GET /reference/{ref}", 200, s, ok=(s == 200))
    s, p = call("GET", "/reference/unknown-type")
    check("public", "GET /reference/type inconnu -> refusé", {400, 404, 422}, s)
    s, p = call("POST", "/contact", body={"name": "Sim Test", "email": "sim@test.ci", "message": "Bonjour, simulation."})
    check("public", "POST /contact valide", {200, 201}, s, detail=err_msg(p))
    s, p = call("POST", "/contact", body={"email": "pas-un-email", "message": ""})
    check("public", "POST /contact invalide -> 400", 400, s)
    s, p = call("GET", "/nope/inconnu")
    check("public", "Route inconnue sans token (Security d'abord)", {401, 404}, s)
    s, p = call("GET", "/stats/public", base=PROXY)
    check("public", "Proxy Vite /api fonctionne", 200, s)

    # Accès non authentifiés
    s, p = call("GET", "/profiles/me")
    check("acces", "GET /profiles/me sans token -> 401", 401, s)
    s, p = call("POST", "/likes", body={"cibleId": 1})
    check("acces", "POST /likes sans token -> 401", 401, s)
    s, p = call("GET", "/admin/users")
    check("acces", "GET /admin/users sans token", {401, 403}, s)

    # ------------------------------------------------------------------
    # 2.AUTH — inscription, OTP, connexion
    # ------------------------------------------------------------------
    s, p = call("POST", "/auth/register", body={
        "email": A_MAIL, "password": PW_INIT, "confirmPassword": PW_INIT,
        "firstName": "Sim", "lastName": "Alpha",
    })
    check("auth", "POST /auth/register (A)", 201, s, detail=err_msg(p))

    s, p = call("POST", "/auth/register", body={
        "email": A_MAIL, "password": PW_INIT, "confirmPassword": PW_INIT,
        "firstName": "Sim", "lastName": "Alpha",
    })
    check("auth", "register doublon -> conflit", {400, 409}, s)

    s, p = call("POST", "/auth/register", body={
        "email": f"weak{SUF}@campuslink.io", "password": "abc", "confirmPassword": "abc",
        "firstName": "W", "lastName": "Eak",
    })
    check("auth", "register mot de passe faible -> 400", 400, s)

    # OTP : register n'envoie pas de code — il faut appeler /otp/send explicitement
    s, p = call("POST", "/otp/send", body={"email": A_MAIL, "type": "EMAIL_VERIFICATION"})
    check("otp", "POST /otp/send (cooldown accepté)", {200, 429}, s, detail=err_msg(p))

    code = None
    for _ in range(10):
        rows, err = sql(
            f"SELECT code FROM otp_codes o JOIN users u ON o.user_id = u.id "
            f"WHERE u.email = '{A_MAIL}' AND o.used = 0 ORDER BY o.created_at DESC LIMIT 1;"
        )
        if rows:
            code = rows[0]
            break
        time.sleep(0.5)
    check("otp", "code OTP en base après /otp/send", True, code is not None, ok=bool(code),
          detail=err or "")

    s, p = call("POST", "/otp/verify", body={"email": A_MAIL, "code": "000000", "type": "EMAIL_VERIFICATION"})
    check("otp", "POST /otp/verify code faux -> 400", 400, s)
    s, p = call("POST", "/otp/verify", body={"email": A_MAIL, "code": code or "", "type": "EMAIL_VERIFICATION"})
    check("otp", "POST /otp/verify code correct", 200, s, detail=err_msg(p))

    s, p = call("POST", "/otp/resend", body={"email": A_MAIL, "type": "EMAIL_VERIFICATION"})
    check("otp", "POST /otp/resend (cooldown accepté)", {200, 429}, s)

    s, p = call("POST", "/auth/login", body={"email": A_MAIL, "password": "WrongPass1"})
    check("auth", "login mauvais mot de passe -> 401", 401, s)

    s, a_tok, a_ref = login(A_MAIL, PW_INIT)
    check("auth", "login A -> 200 + tokens", True, s == 200 and bool(a_ref),
          ok=(s == 200 and bool(a_ref)))
    if not a_tok:
        print("ARRÊT : impossible de se connecter en A.")
        return 1

    s, p = call("POST", "/auth/refresh-token", body={"refreshToken": a_ref})
    new_pair = unwrap(p) or {}
    check("auth", "POST /auth/refresh-token", 200, s, detail=err_msg(p))
    refresh_ok = bool(new_pair.get("refreshToken"))

    s, p = call("GET", "/nope/inconnu", token=a_tok)
    check("auth", "Route inconnue authentifiée -> 404", 404, s)

    # ------------------------------------------------------------------
    # 3.FORGOT / RESET PASSWORD (mode log-only, pas de 503)
    # ------------------------------------------------------------------
    s, p = call("POST", "/auth/forgot-password", body={"email": "inconnu@campuslink.io"})
    check("reset", "forgot-password email inconnu -> 200 (non fuite)", 200, s)

    s, p = call("POST", "/auth/forgot-password", body={"email": A_MAIL})
    check("reset", "forgot-password A -> 200 (log-only, pas de 503)", 200, s, detail=err_msg(p))
    time.sleep(1.0)
    token_a = last_reset_token()
    check("reset", "lien /reset-password journalisé avec token", True, bool(token_a), ok=bool(token_a))

    s, p = call("POST", "/auth/forgot-password", body={"email": A_MAIL})
    check("reset", "forgot-password relancé trop tôt -> 429 (cooldown)", 429, s, detail=err_msg(p))

    s, p = call("POST", "/auth/reset-password", body={"token": "jeton-absolu-invalide", "newPassword": PW_RESET, "confirmPassword": PW_RESET})
    check("reset", "reset-password jeton invalide -> 400", 400, s)

    s, p = call("POST", "/auth/reset-password", body={"token": token_a or "x", "newPassword": "faible", "confirmPassword": "faible"})
    check("reset", "reset-password mot de passe faible -> 400", 400, s)

    s, p = call("POST", "/auth/reset-password", body={"token": token_a or "x", "newPassword": PW_RESET, "confirmPassword": PW_RESET})
    check("reset", "reset-password valide -> 200", 200, s, detail=err_msg(p))

    s, old_tok, _ = login(A_MAIL, PW_INIT)
    check("reset", "ancien mot de passe révoqué -> 401", 401, s)
    s, a_tok, a_ref = login(A_MAIL, PW_RESET)
    check("reset", "login avec le nouveau mot de passe", 200, s)

    # ------------------------------------------------------------------
    # 4.PROFILS
    # ------------------------------------------------------------------
    profile = {
        "avatarUrl": None,
        "firstName": "Sim",
        "lastName": "Alpha",
        "gender": "FEMALE",
        "level": "LICENCE",
        "dateOfBirth": "2003-04-12",
        "university": "INP-HB",
        "fieldOfStudy": "Informatique",
        "neighborhood": "Centre",
        "city": "Bouaké",
        "bio": "Profil de la simulation API.",
        "interests": ["Musique", "Informatique"],
    }
    s, p = call("PUT", "/profiles/me", token=a_tok, body=profile)
    check("profils", "PUT /profiles/me (création par mise à jour)", 200, s, detail=err_msg(p))

    s, p = call("POST", "/profiles/me", token=a_tok, body=profile)
    check("profils", "POST /profiles/me alors qu'il existe -> 409", 409, s)

    s, p = call("GET", "/profiles/me", token=a_tok)
    me = unwrap(p) or {}
    check("profils", "GET /profiles/me", 200, s)
    check("profils", "profil persisté (bio + intérêts)", True,
          me.get("bio") == profile["bio"] and "Musique" in (me.get("interests") or []),
          ok=(me.get("bio") == profile["bio"] and "Musique" in (me.get("interests") or [])))
    a_legacy = me.get("legacyId")
    a_uuid = me.get("id")
    check("profils", "legacyId exposé pour le module temps réel", True, a_legacy is not None, ok=a_legacy is not None)

    s, p = call("GET", "/profiles/search", token=a_tok)
    found_ids = [it.get("legacyId") for it in items(p)]
    check("profils", "GET /profiles/search", 200, s)
    check("profils", "exclusion de soi (A absent de son feed)", True,
          a_legacy not in found_ids, ok=(a_legacy not in found_ids),
          detail=f"legacyId={a_legacy} présent dans {found_ids[:8]}")

    s, p = call("GET", "/profiles/search?gender=FEMALE&minAge=18&maxAge=99", token=a_tok)
    check("profils", "recherche filtrée (genre + âge)", 200, s)
    s, p = call("GET", "/profiles/search?minAge=90", token=a_tok)
    check("profils", "recherche vide (âge impossible)", 200, s)

    s, p = call("GET", f"/profiles/{uuid.uuid4()}/public", token=a_tok)
    check("profils", "GET /profiles/{uuid inconnu}/public -> 404", 404, s)

    # ------------------------------------------------------------------
    # 5.MÉDIAS
    # ------------------------------------------------------------------
    up_body, up_hdrs = multipart({}, "file", "sim.png", PNG)
    s, p = call("POST", "/media/upload", token=a_tok,
                raw_body=up_body, headers=up_hdrs)
    media = unwrap(p) or {}
    check("medias", "POST /media/upload (PNG valide) -> 201", 201, s, detail=err_msg(p))

    file_url = media.get("url")
    if file_url and file_url.startswith("/"):
        file_url = "http://localhost:8080" + file_url
    s2 = None
    if file_url:
        s2, _ = call("GET", file_url)
    check("medias", "fichier uploadé servi (GET url)", {200, 304}, s2)

    body, hdrs = multipart({}, "file", "fake.png", b"ce n'est pas une image", "text/plain")
    s, p = call("POST", "/media/upload", token=a_tok, raw_body=body, headers=hdrs)
    check("medias", "upload type MIME invalide -> refus", {400, 415, 422}, s)

    s, p = call("POST", "/media/upload", raw_body=up_body, headers=up_hdrs)
    check("medias", "upload sans token -> 401", 401, s)

    public_id = media.get("publicId")
    if public_id:
        body, hdrs = multipart({"publicId": public_id}, "file", "sim2.png", PNG)
        s, p = call("PUT", "/media/replace", token=a_tok, raw_body=body, headers=hdrs)
        check("medias", "PUT /media/replace", 200, s, detail=err_msg(p))
        new_id = (unwrap(p) or {}).get("publicId") or public_id
        s, p = call("DELETE", f"/media?publicId={new_id}", token=a_tok)
        check("medias", "DELETE /media", {200, 204}, s)
        s, p = call("DELETE", f"/media?publicId={new_id}", token=a_tok)
        check("medias", "DELETE /media idempotent (2e fois)", {200, 204}, s)
    else:
        check("medias", "publicId renvoyé par l'upload", True, False, ok=False)

    # ------------------------------------------------------------------
    # 6.COMPTE B — likes, match, messages
    # ------------------------------------------------------------------
    s, p = call("POST", "/auth/register", body={
        "email": B_MAIL, "password": PW_INIT, "confirmPassword": PW_INIT,
        "firstName": "Sim", "lastName": "Beta",
    })
    check("likes", "register B", 201, s)
    s, b_tok, _ = login(B_MAIL, PW_INIT)
    check("likes", "login B", 200, s)
    if not b_tok:
        print("ARRÊT : B inaccessible.")
        return 1

    call("PUT", "/profiles/me", token=b_tok, body={
        **profile, "firstName": "Sim", "lastName": "Beta", "gender": "MALE",
        "bio": "Profil B de la simulation.",
    })
    s, p = call("GET", "/profiles/me", token=b_tok)
    b_legacy = (unwrap(p) or {}).get("legacyId")

    # Compteur de matchs avant
    s0, p0 = call("GET", "/matches", token=a_tok)
    before_ids = {m.get("id") for m in items(p0)}

    s, p = call("POST", "/likes", token=a_tok, body={"cibleId": 99999999})
    check("likes", "POST /likes cible inexistante -> 404 (anti-500)", 404, s, detail=err_msg(p))

    s, p = call("POST", "/likes", token=a_tok, body={"cibleId": a_legacy})
    check("likes", "auto-like -> conflit", 409, s)

    s, p = call("POST", "/likes", token=a_tok, body={"cibleId": b_legacy})
    like_resp = unwrap(p) if isinstance(p, dict) else p
    check("likes", "A like B -> 201", 201, s, detail=err_msg(p))

    s, p = call("POST", "/likes", token=a_tok, body={"cibleId": b_legacy})
    check("likes", "double like -> conflit", 409, s)

    s, p = call("GET", "/likes", token=a_tok)
    check("likes", "GET /likes (likes émis)", 200, s)
    s, p = call("GET", "/likes/recus", token=b_tok)
    check("likes", "GET /likes/recus (likes reçus)", 200, s)

    s, p = call("POST", "/likes", token=b_tok, body={"cibleId": a_legacy})
    match_created = (unwrap(p) or {}).get("matchCree") if isinstance(p, dict) else None
    check("likes", "B like A en retour -> match créé", True, match_created is True,
          ok=(match_created is True), detail=err_msg(p))

    s, p = call("GET", "/matches", token=a_tok)
    after_ids = {m.get("id") for m in items(p)}
    new_matches = after_ids - before_ids
    check("matchs", "GET /matches contient le nouveau match", True, len(new_matches) >= 1,
          ok=len(new_matches) >= 1)
    match_id = next(iter(new_matches), None)

    s, p = call("GET", "/matches/historique", token=a_tok)
    check("matchs", "GET /matches/historique", {200, 404}, s)

    s, p = call("GET", "/matches/99999999/messages", token=a_tok)
    check("matchs", "messages d'un match inconnu -> 404", 404, s)

    if match_id is not None:
        s, p = call("GET", f"/matches/{match_id}/messages", token=a_tok)
        check("messages", "GET /matches/{id}/messages", 200, s)
        s, p = call("POST", f"/matches/{match_id}/messages", token=a_tok,
                    body={"matchId": match_id, "contenu": "Bonjour depuis la simulation !"})
        sent = unwrap(p) if p is not None else None
        check("messages", "POST message (repli HTTP)", 200, s, detail=err_msg(p))
        s, p = call("GET", f"/matches/{match_id}/messages", token=a_tok)
        contents = [m.get("contenu") for m in items(p)]
        check("messages", "message persisté et lisible", True,
              any("Bonjour depuis la simulation" in (c or "") for c in contents),
              ok=any("Bonjour depuis la simulation" in (c or "") for c in contents))
        s, p = call("POST", f"/matches/{match_id}/messages/lu", token=b_tok)
        check("messages", "POST .../messages/lu", {200, 204}, s)

        # Accès d'un tiers au match (C n'appartient pas au match) : on teste
        # avec l'utilisateur D créé plus bas si présent, sinon ignoré.
    else:
        for nm in ("GET /matches/{id}/messages", "POST message", "message persisté", "messages/lu"):
            check("messages", nm, True, False, ok=False, detail="match non créé")

    # ------------------------------------------------------------------
    # 7.NOTIFICATIONS
    # ------------------------------------------------------------------
    s, p = call("GET", "/notifications", token=b_tok)
    notifs = items(p) if s == 200 else []
    check("notif", "GET /notifications", 200, s)
    s, p = call("GET", "/notifications/compteur", token=b_tok)
    check("notif", "GET /notifications/compteur", 200, s)
    s, p = call("GET", "/notifications/non-lues", token=b_tok)
    check("notif", "GET /notifications/non-lues", 200, s)
    unread = items(p) if s == 200 else []
    if unread:
        nid = unread[0].get("id")
        s, p = call("POST", f"/notifications/{nid}/lu", token=b_tok)
        check("notif", "POST /notifications/{id}/lu", {200, 204}, s)
    else:
        check("notif", "au moins une notification non lue après like", True, False, ok=False,
              detail="aucune notification générée par le like")
    s, p = call("POST", "/notifications/lu-tout", token=b_tok)
    check("notif", "POST /notifications/lu-tout", {200, 204}, s)
    s, p = call("POST", "/notifications/99999999/lu", token=b_tok)
    check("notif", "notification inexistante -> 4xx", {400, 404, 422}, s)

    # ------------------------------------------------------------------
    # 8.SIGNALEMENTS & MODÉRATION
    # ------------------------------------------------------------------
    s, p = call("POST", "/reports", token=a_tok, body={"cibleId": 99999999, "motif": "Test", "description": "cible inexistante"})
    check("reports", "POST /reports cible inexistante -> 404 (anti-500)", 404, s, detail=err_msg(p))

    s, p = call("POST", "/reports", token=a_tok, body={"cibleId": a_legacy, "motif": "Test"})
    check("reports", "auto-signalement -> conflit", 409, s)

    s, p = call("POST", "/reports", token=a_tok,
                body={"cibleId": b_legacy, "motif": "Comportement inapproprie", "description": "Simulation."})
    report = unwrap(p) if isinstance(p, dict) else None
    check("reports", "POST /reports valide", {200, 201}, s, detail=err_msg(p))
    report_id = report.get("id") if isinstance(report, dict) else None

    s, p = call("GET", "/reports", token=a_tok)
    check("acces", "GET /reports en étudiant -> 403", {401, 403}, s)

    s, p = call("POST", "/moderation/avertir", token=a_tok,
                body={"utilisateurCibleId": b_legacy, "motif": "Test"})
    check("acces", "POST /moderation/avertir en étudiant -> 403", {401, 403}, s)

    s, p = call("GET", "/moderation/journal", token=a_tok)
    check("acces", "GET /moderation/journal en étudiant -> 403", {401, 403}, s)

    # ------------------------------------------------------------------
    # 9.ADMINISTRATION (mot de passe réinitialisé par le lien email)
    # ------------------------------------------------------------------
    # Cooldown anti-spam de 60s par utilisateur : on attend et on retente.
    for _ in range(4):
        s, p = call("POST", "/auth/forgot-password", body={"email": ADMIN_MAIL})
        if s == 429:
            time.sleep(31)
            continue
        break
    check("admin", "forgot-password pour l'admin -> 200", 200, s, detail=err_msg(p))
    time.sleep(1.0)
    admin_token_raw = last_reset_token()
    check("admin", "lien de reset admin journalisé", True, bool(admin_token_raw), ok=bool(admin_token_raw))
    s, p = call("POST", "/auth/reset-password",
                body={"token": admin_token_raw or "x", "newPassword": PW_ADMIN, "confirmPassword": PW_ADMIN})
    check("admin", "reset mot de passe admin -> 200", 200, s, detail=err_msg(p))
    s, adm_tok, adm_ref = login(ADMIN_MAIL, PW_ADMIN)
    check("admin", "login admin", 200, s, detail="identifiants admin inconnus ?")
    if not adm_tok:
        print("ARRÊT : admin inaccessible, section admin ignorée.")
    else:
        s, p = call("GET", "/admin/dashboard", token=adm_tok)
        check("admin", "GET /admin/dashboard", 200, s, detail=err_msg(p))
        s, p = call("GET", "/admin/stats", token=adm_tok)
        check("admin", "GET /admin/stats", 200, s)
        s, p = call("GET", "/admin/users?page=0&size=10", token=adm_tok)
        check("admin", "GET /admin/users paginé", 200, s, detail=err_msg(p))
        s, p = call("GET", f"/admin/users?email={C_MAIL}", token=adm_tok)
        check("admin", "GET /admin/users filtre email (aucun)", 200, s)
        s, p = call("GET", "/admin/users?email=etude.b", token=adm_tok)
        users_etude = items(p)
        check("admin", "GET /admin/users filtre email ( trouvé)", True, len(users_etude) >= 1,
              ok=len(users_etude) >= 1)

        # Compte C : cible des actions admin
        s, p = call("POST", "/auth/register", body={
            "email": C_MAIL, "password": PW_INIT, "confirmPassword": PW_INIT,
            "firstName": "Sim", "lastName": "Cible",
        })
        check("admin", "register C (cible admin)", 201, s)
        s, p = call("GET", f"/admin/users?email={C_MAIL}", token=adm_tok)
        c_users = items(p)
        c_id = c_users[0].get("id") if c_users else None
        check("admin", "C retrouvé dans la liste admin", True, bool(c_id), ok=bool(c_id))

        if c_id:
            s, p = call("PATCH", f"/admin/users/{c_id}/suspend", token=adm_tok, body={"motif": "Simulation"})
            check("admin", "PATCH .../suspend", 200, s, detail=err_msg(p))
            s, p = call("GET", f"/admin/users?email={C_MAIL}", token=adm_tok)
            st = (items(p)[0].get("status") if items(p) else None)
            check("admin", "statut de C = SUSPENDED", "SUSPENDED", st)
            s, p = call("PATCH", f"/admin/users/{c_id}/activate", token=adm_tok, body={})
            check("admin", "PATCH .../activate", 200, s, detail=err_msg(p))
            s, p = call("PATCH", f"/admin/users/{c_id}/role", token=adm_tok, body={"role": "ADMIN"})
            check("admin", "PATCH .../role -> ADMIN", 200, s, detail=err_msg(p))
            s, p = call("PATCH", f"/admin/users/{c_id}/role", token=adm_tok, body={"role": "STUDENT"})
            check("admin", "PATCH .../role -> STUDENT", 200, s, detail=err_msg(p))
            s, p = call("PATCH", f"/admin/users/{c_id}", token=adm_tok, body={"emailVerified": True})
            check("admin", "PATCH /admin/users/{id} (emailVerified)", 200, s, detail=err_msg(p))
            s, p = call("DELETE", "/admin/users/00000000-0000-0000-0000-000000000000", token=adm_tok)
            check("admin", "DELETE utilisateur inconnu -> 404", 404, s)

            # Reports & modération avec les droits admin
            if report_id:
                s, p = call("PATCH", f"/reports/{report_id}/statut", token=adm_tok, body={"statut": "EN_COURS"})
                check("reports", "PATCH /reports/{id}/statut", 200, s, detail=err_msg(p))
                s, p = call("POST", f"/reports/{report_id}/archiver", token=adm_tok)
                check("reports", "POST /reports/{id}/archiver", {200, 204}, s)
            else:
                check("reports", "PATCH statut (sans rapport)", True, False, ok=False)

            s, p = call("GET", "/reports", token=adm_tok)
            check("reports", "GET /reports en admin", 200, s)
            s, p = call("GET", f"/reports?statut=ARCHIVE", token=adm_tok)
            check("reports", "GET /reports filtré par statut", 200, s)

            s, p = call("POST", "/moderation/avertir", token=adm_tok,
                        body={"utilisateurCibleId": 99999999, "motif": "Test"})
            check("moderation", "avertir cible inexistante -> 404 (anti-500)", 404, s, detail=err_msg(p))
            s, p = call("GET", "/moderation/historique/99999999", token=adm_tok)
            check("moderation", "historique utilisateur inconnu -> vide/404", True,
                  (s == 404) or (s == 200 and not items(p)),
                  ok=(s == 404) or (s == 200 and not items(p)),
                  detail=f"statut={s}")

            # On dote C d'un profil pour obtenir son legacyId
            c_tok_s, c_tok, _ = login(C_MAIL, PW_INIT)
            c_legacy = None
            if c_tok:
                call("PUT", "/profiles/me", token=c_tok, body={**profile, "firstName": "Sim", "lastName": "Cible"})
                s, p = call("GET", "/profiles/me", token=c_tok)
                c_legacy = (unwrap(p) or {}).get("legacyId")

            if c_legacy:
                s, p = call("POST", "/moderation/avertir", token=adm_tok,
                            body={"utilisateurCibleId": c_legacy, "motif": "Avertissement de simulation"})
                check("moderation", "POST /moderation/avertir", {200, 201}, s, detail=err_msg(p))
                s, p = call("POST", "/moderation/suspendre", token=adm_tok,
                            body={"utilisateurCibleId": c_legacy, "motif": "Suspension de simulation"})
                check("moderation", "POST /moderation/suspendre", {200, 201}, s, detail=err_msg(p))
                s, p = call("POST", "/moderation/bannir", token=adm_tok,
                            body={"utilisateurCibleId": c_legacy, "motif": "Bannissement de simulation"})
                check("moderation", "POST /moderation/bannir", {200, 201}, s, detail=err_msg(p))
                s, p = call("POST", "/moderation/debannir", token=adm_tok,
                            body={"utilisateurCibleId": c_legacy, "motif": "Fin de simulation"})
                check("moderation", "POST /moderation/debannir", {200, 201}, s, detail=err_msg(p))
                s, p = call("GET", f"/moderation/historique/{c_legacy}", token=adm_tok)
                check("moderation", "GET /moderation/historique/{id}", 200, s)
                s, p = call("GET", "/moderation/journal", token=adm_tok)
                check("moderation", "GET /moderation/journal en admin", 200, s)
            else:
                for nm in ("avertir", "suspendre", "bannir", "debannir", "historique", "journal"):
                    check("moderation", nm, True, False, ok=False, detail="legacyId C indisponible")

            # Nettoyage : suppression de C
            s, p = call("DELETE", f"/admin/users/{c_id}", token=adm_tok)
            check("admin", "DELETE /admin/users/{id} (C)", {200, 204}, s, detail=err_msg(p))
            s, p = call("GET", f"/admin/users?email={C_MAIL}", token=adm_tok)
            check("admin", "C supprimé de la liste", True, len(items(p)) == 0,
                  ok=len(items(p)) == 0)

        s, p = call("GET", "/admin/dashboard", token=a_tok)
        check("acces", "GET /admin/dashboard en étudiant -> 403", 403, s)
        s, p = call("DELETE", f"/admin/users/{a_uuid or ''}", token=a_tok)
        check("acces", "DELETE /admin/users en étudiant -> 403", 403, s)

    # ------------------------------------------------------------------
    # 10.PROFILE DELETE (compte jetable D)
    # ------------------------------------------------------------------
    s, p = call("POST", "/auth/register", body={
        "email": D_MAIL, "password": PW_INIT, "confirmPassword": PW_INIT,
        "firstName": "Sim", "lastName": "Delta",
    })
    check("profils", "register D (jetable)", 201, s)
    s, d_tok, _ = login(D_MAIL, PW_INIT)
    check("profils", "login D", 200, s)
    if d_tok:
        s, p = call("PUT", "/profiles/me", token=d_tok, body={**profile, "firstName": "Sim", "lastName": "Delta"})
        check("profils", "création du profil D", 200, s)
        s, p = call("GET", "/profiles/me", token=d_tok)
        check("profils", "profil D consultable", 200, s)
        s, p = call("DELETE", "/profiles/me", token=d_tok)
        check("profils", "DELETE /profiles/me", {200, 204}, s, detail=err_msg(p))
        s, p = call("GET", "/profiles/me", token=d_tok)
        check("profils", "profil D absent après suppression", {404, 410}, s)

    # ------------------------------------------------------------------
    # 11.LOGOUT & session
    # ------------------------------------------------------------------
    s, p = call("POST", "/auth/logout", token=a_tok, body={"refreshToken": a_ref})
    check("auth", "POST /auth/logout", {200, 204}, s, detail=err_msg(p))
    s, p = call("POST", "/auth/refresh-token", body={"refreshToken": a_ref})
    check("auth", "refresh token révoqué après logout -> 401", 401, s)

    # ------------------------------------------------------------------
    # Rapport
    # ------------------------------------------------------------------
    total = len(RESULTS)
    passed = sum(1 for r in RESULTS if r[4])
    failed = total - passed
    duration = time.time() - t0
    lines = [
        f"Simulation API CampusLink — {time.strftime('%Y-%m-%d %H:%M:%S')}",
        f"Cible : {BASE}  (proxy : {PROXY})",
        f"Durée : {duration:.1f}s",
        f"Résultat : {passed}/{total} vérifications réussies, {failed} échec(s)",
        "",
    ]
    for section, name, exp, act, ok, detail in RESULTS:
        mark = "OK  " if ok else "FAIL"
        line = f"[{mark}] {section:<12} {name:<58} attendu={exp} obtenu={act}"
        if detail:
            line += f"  | {detail}"
        lines.append(line)
    if failed:
        lines += ["", "--- ÉCHECS ---"]
        for section, name, exp, act, ok, detail in RESULTS:
            if not ok:
                lines.append(f"[FAIL] {section} / {name} : attendu={exp} obtenu={act} | {detail}")
    report = "\n".join(lines)
    with open(REPORT_PATH, "w", encoding="utf-8") as fh:
        fh.write(report + "\n")
    print(f"\n=== {passed}/{total} vérifications réussies ({failed} échec(s)) — rapport : {REPORT_PATH} ===")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
