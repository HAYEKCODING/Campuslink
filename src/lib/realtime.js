import { Client } from "@stomp/stompjs";

/**
 * Client STOMP temps réel (messagerie, accusés de lecture, notifications).
 *
 * Handshake : WebSocket sur `/ws` de l'origine du backend (le context-path
 * `/api` est inclus, d'où la dérivation depuis VITE_API_URL). SockJS n'est pas
 * nécessaire : le backend expose aussi l'endpoint natif `/ws`.
 *
 * Authentification : le JWT est envoyé sur la trame CONNECT via l'en-tête
 * `Authorization: Bearer …` (voir `StompAuthChannelInterceptor`, qui accepte
 * aussi l'en-tête natif `token`). Le Principal résultant porte le `legacyId`
 * de l'utilisateur — seul identifiant attendu par le module temps réel.
 *
 * Destinations (voir WebSocketConfig) :
 *  - client -> serveur  : `/app/chat.send`, `/app/chat.typing`
 *  - serveur -> client  : `/user/queue/messages`, `/user/queue/read-receipts`,
 *    `/user/queue/notifications` (privées), `/topic/matches/{id}/typing` (broadcast)
 *
 * Usage :
 *   const rt = getRealtimeClient();          // singleton
 *   rt.connect();                            // no-op si déjà connecté / déconnecté
 *   const sub = rt.subscribeUser("/queue/messages", (msg) => { … });
 *   rt.publish("/app/chat.send", { matchId, contenu });
 *
 * Le client se reconnecte automatiquement (reconnexion propre après un
 * redémarrage du backend) ; les souscriptions sont reprises par SockJS/STOMP
 * via `onConnect` — voir `subscribeUser` qui rejoue ses abonnements actifs.
 */

/** Construit l'URL du handshake à partir de la base API. */
function resolveWsUrl() {
  const base = import.meta.env.VITE_API_URL || "/api";
  if (base.startsWith("http")) {
    // http(s)://host:port/api -> ws(s)://host:port/api/ws
    return base.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";
  }
  // Base relative : en dev on passe par l'origine courante (le proxy Vite
  // ne relaie que /api, pas /ws) — on cible donc directement le backend.
  if (import.meta.env.DEV) {
    const target = import.meta.env.VITE_PROXY_TARGET || "http://localhost:8080";
    return target.replace(/^http/, "ws").replace(/\/$/, "") + "/api/ws";
  }
  return `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}${base}/ws`;
}

function readAccessToken() {
  return localStorage.getItem("campuslink_token");
}

class RealtimeClient {
  constructor() {
    this.client = null;
    this.connected = false;
    /** Abonnements actifs : Map destination -> callback, rejoués à chaque connexion. */
    this.userSubscriptions = new Map();
    this.subscriptions = new Map();
    this.listeners = { connect: [], disconnect: [] };
  }

  /** L'endpoint temps réel n'est utile que connecté et identifié. */
  get enabled() {
    return !!readAccessToken();
  }

  connect() {
    if (!this.enabled || (this.client && this.client.active)) return;

    this.client = new Client({
      webSocketFactory: () => new WebSocket(resolveWsUrl()),
      connectHeaders: { Authorization: `Bearer ${readAccessToken()}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onConnect: () => {
        this.connected = true;
        // Rejoue les souscriptions demandées avant la (re)connexion.
        this.userSubscriptions.forEach((callback, destination) => {
          this.subscriptions.set(destination, this.client.subscribe(destination, callback));
        });
        this.listeners.connect.forEach((cb) => cb());
      },
      onDisconnect: () => {
        this.connected = false;
        this.subscriptions.clear();
        this.listeners.disconnect.forEach((cb) => cb());
      },
      onStompError: (frame) => {
        // Token refusé (expiré) : on laisse la reconnexion retenter après
        // un éventuel refresh ; aucune donnée n'est lue sans session valide.
        console.warn("[realtime] Erreur STOMP :", frame.headers?.message);
      },
    });

    this.client.activate();
  }

  deactivate() {
    if (!this.client) return;
    this.client.deactivate();
    this.client = null;
    this.connected = false;
    this.subscriptions.clear();
    this.userSubscriptions.clear();
  }

  /**
   * Souscrit une destination privée (`/user` implicite : passer
   * `/queue/messages` ou `/user/queue/messages`, les deux fonctionnent).
   * L'abonnement est rejoué automatiquement après une reconnexion.
   */
  subscribeUser(destination, callback) {
    const full = destination.startsWith("/user") ? destination : `/user${destination}`;
    this.userSubscriptions.set(full, callback);
    if (this.connected && this.client) {
      this.subscriptions.set(full, this.client.subscribe(full, callback));
    }
    return {
      unsubscribe: () => {
        this.userSubscriptions.delete(full);
        const sub = this.subscriptions.get(full);
        if (sub) sub.unsubscribe();
        this.subscriptions.delete(full);
      },
    };
  }

  /** Souscrit une destination publique (`/topic/…`), sans rejeu automatique. */
  subscribe(destination, callback) {
    if (!this.connected || !this.client) return { unsubscribe: () => {} };
    const sub = this.client.subscribe(destination, callback);
    return { unsubscribe: () => sub.unsubscribe() };
  }

  /** Publie sur une destination client -> serveur (`/app/…`). */
  publish(destination, payload) {
    if (!this.connected || !this.client) return false;
    this.client.publish({
      destination,
      body: JSON.stringify(payload),
      headers: { "content-type": "application/json" },
    });
    return true;
  }

  /** Enregistre un callback appelé à chaque (re)connexion — renvoie sa désinscription. */
  onConnect(cb) {
    this.listeners.connect.push(cb);
    if (this.connected) cb();
    return () => {
      this.listeners.connect = this.listeners.connect.filter((fn) => fn !== cb);
    };
  }

  onDisconnect(cb) {
    this.listeners.disconnect.push(cb);
  }
}

/** Singleton par onglet. */
let instance = null;

export function getRealtimeClient() {
  if (!instance) instance = new RealtimeClient();
  return instance;
}

/**
 * Décode sans vérification (côté navigateur) le payload utile du JWT —
 * utilisé pour connaître son propre `legacyId` et reconnaître ses messages.
 */
export function readTokenPayload() {
  const token = readAccessToken();
  if (!token) return null;
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

/** Raccourci : envoie un message de chat via `/app/chat.send`. */
export function sendChatMessage(matchId, contenu) {
  return getRealtimeClient().publish("/app/chat.send", {
    matchId: Number(matchId),
    contenu,
  });
}

/** Raccourci : signale (ou non) la saisie en cours dans une conversation. */
export function sendTyping(matchId, enTrainDecrire) {
  return getRealtimeClient().publish("/app/chat.typing", {
    matchId: Number(matchId),
    enTrainDecrire: !!enTrainDecrire,
  });
}

