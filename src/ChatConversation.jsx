import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Phone, Video, MoreVertical, Smile, Send } from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import { getMessages, sendMessage } from "./services/messagesService";
import { getMatches } from "./services/matchesService";
import { getMyProfile } from "./services/profileService";
import { getRealtimeClient, sendTyping } from "./lib/realtime";
import { LoadingState, ErrorState } from "./components/ui/AsyncStates";

/**
 * CampusLink — Conversation (chat)
 *
 * Une conversation = un match (`/app/messages/:matchId`) :
 *  - en-tête  : GET /matches (identité de l'interlocuteur)
 *  - historique : GET /matches/{matchId}/messages (chronologique)
 *  - envoi    : POST /matches/{matchId}/messages, avec ajout optimiste dans
 *    l'interface puis marquage de l'échec (avec renvoi) si l'API échoue.
 *    (L'envoi reste HTTP : le serveur ne rédiffe pas l'émetteur sur
 *    /user/queue/messages, seul le destinataire est notifié.)
 *  - direct  : souscription STOMP /user/queue/messages — les messages reçus
 *    pendant la conversation s'affichent en direct, dédupliqués par id.
 *
 * `expediteurId` est l'identifiant module temps réel : on compare avec le
 * `legacyId` du profil connecté pour savoir qui a écrit chaque message.
 */

function formatTime(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

export default function ChatConversation() {
  const { id: matchId } = useParams();
  const navigate = useNavigate();

  const { data: matchList, loading: matchesLoading, error: matchesError, reload: reloadMatches } =
    useAsyncData(() => getMatches(), []);
  const { data: history, loading, error, reload } = useAsyncData(() => getMessages(matchId), [matchId]);
  const { data: me } = useAsyncData(() => getMyProfile().catch(() => null), []);

  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [peerTyping, setPeerTyping] = useState(false);
  // Déclaré AVANT tout early return (loading/erreur) : un hook placé après un
  // return conditionnel casse la règle des hooks (« Rendered more hooks than
  // during the previous render ») et fait planter la conversation.
  const typingTimerRef = React.useRef(null);

  useEffect(() => {
    if (history) {
      setMessages(history.map((m) => ({ ...m, time: formatTime(m.dateEnvoi) })));
    }
  }, [history]);

  // Messages entrants en direct (STOMP) : on ne garde que ceux de cette
  // conversation, en évitant les doublons avec l'historique HTTP.
  useEffect(() => {
    const rt = getRealtimeClient();
    rt.connect();
    const subscription = rt.subscribeUser("/queue/messages", (frame) => {
      let incoming;
      try {
        incoming = JSON.parse(frame.body);
      } catch {
        return;
      }
      if (String(incoming.matchId) !== String(matchId)) return;
      setMessages((prev) => {
        if (incoming.id != null && prev.some((m) => m.id === incoming.id)) return prev;
        return [...prev, { ...incoming, time: formatTime(incoming.dateEnvoi) }];
      });
    });
    return () => subscription.unsubscribe();
  }, [matchId]);

  // Indicateur « en train d'écrire » de l'interlocuteur (broadcast /topic).
  // Le serveur ne fait pas le ménage : on masque après 4s sans nouvel événement.
  useEffect(() => {
    const rt = getRealtimeClient();
    rt.connect();
    let active = { unsubscribe() {} };
    // Souscription rejouée à chaque (re)connexion : les souscriptions /topic
    // ne sont pas conservées par le client après une reconnexion.
    const offConnect = rt.onConnect(() => {
      active.unsubscribe();
      active = rt.subscribe(`/topic/matches/${matchId}/typing`, (frame) => {
        try {
          const event = JSON.parse(frame.body);
          if (me && event.utilisateurId === me.legacyId) return; // le sien
          setPeerTyping(!!event.enTrainDecrire);
        } catch {
          // trame malformée : ignorée
        }
      });
    });
    return () => {
      offConnect();
      active.unsubscribe();
      setPeerTyping(false);
    };
  }, [matchId, me]);

  useEffect(() => {
    if (!peerTyping) return undefined;
    const timer = setTimeout(() => setPeerTyping(false), 4000);
    return () => clearTimeout(timer);
  }, [peerTyping]);


  if (loading || matchesLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
        <LoadingState label="Chargement de la conversation..." />
      </div>
    );
  }

  if (error || matchesError) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
        <ErrorState
          label="Impossible de charger cette conversation."
          onRetry={() => {
            reload();
            reloadMatches();
          }}
        />
      </div>
    );
  }

  const match = (matchList || []).find((m) => String(m.id) === String(matchId));
  const contact = {
    name: match?.autreUtilisateurNom || "Conversation",
    photo: match?.autreUtilisateurPhoto || null,
  };

  const handleSend = (e) => {
    e.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;

    const optimistic = {
      optimistic: true,
      pending: true,
      fromMe: true,
      contenu: text,
      time: new Date().toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, optimistic]);
    setDraft("");
    setSending(true);

    sendMessage(matchId, text)
      .then((sent) => {
        setMessages((prev) =>
          prev.map((m) =>
            m === optimistic ? { ...sent, fromMe: true, time: formatTime(sent.dateEnvoi), pending: false } : m
          )
        );
      })
      .catch(() => {
        // On conserve le message mais on le marque en échec : l'utilisateur peut
        // le renvoyer sans retaper son texte.
        setMessages((prev) =>
          prev.map((m) => (m === optimistic ? { ...m, pending: false, failed: true } : m))
        );
      })
      .finally(() => setSending(false));
  };

  // Indicateur « en train d'écrire » côté émetteur (debounce simple).
  const handleDraftChange = (value) => {
    setDraft(value);
    if (value) {
      sendTyping(matchId, true);
      clearTimeout(typingTimerRef.current);
      typingTimerRef.current = setTimeout(() => sendTyping(matchId, false), 2500);
    }
  };

  const handleRetry = (failedMessage) => {
    setMessages((prev) => prev.filter((m) => m !== failedMessage));
    setDraft(failedMessage.contenu);
  };

  const isMine = (msg) => msg.fromMe ?? (me ? msg.expediteurId === me.legacyId : false);

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm flex flex-col h-[600px] max-h-[calc(100vh-3rem)]">
        {/* header */}
        <div className="flex items-center gap-3 p-4 border-b border-slate-100">
          <button
            onClick={() => navigate("/app/messages")}
            aria-label="Retour aux messages"
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center flex-shrink-0"
          >
            <ArrowLeft className="w-4 h-4 text-slate-500" />
          </button>
          <div className="w-10 h-10 rounded-full bg-slate-100 flex-shrink-0 overflow-hidden flex items-center justify-center text-sm font-bold text-slate-400">
            {contact.photo ? (
              <img src={contact.photo} alt="" className="w-full h-full object-cover" />
            ) : (
              contact.name.charAt(0)
            )}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-bold text-slate-900 truncate">{contact.name}</p>
          </div>
          <button
            type="button"
            aria-label="Appeler"
            title="Appel audio (bientôt disponible)"
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center"
          >
            <Phone className="w-4 h-4 text-slate-500" />
          </button>
          <button
            type="button"
            aria-label="Appel vidéo"
            title="Appel vidéo (bientôt disponible)"
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center"
          >
            <Video className="w-4 h-4 text-slate-500" />
          </button>
          <button
            type="button"
            aria-label="Plus d'options"
            title="Options (bientôt disponibles)"
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center"
          >
            <MoreVertical className="w-4 h-4 text-slate-500" />
          </button>
        </div>

        {/* messages */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {peerTyping && (
            <p className="text-xs text-slate-400 italic animate-pulse">L'interlocuteur écrit…</p>
          )}
          {messages.length === 0 && (
            <p className="text-sm text-slate-400 text-center mt-8">
              Aucun message pour l'instant. Dites bonjour !
            </p>
          )}
          {messages.map((msg, i) => (
            <div
              key={msg.id ?? `optimistic-${i}`}
              className={`flex ${isMine(msg) ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-[75%] px-4 py-2.5 rounded-2xl text-sm ${
                  isMine(msg)
                    ? "bg-violet-600 text-white rounded-br-sm"
                    : "bg-slate-100 text-slate-700 rounded-bl-sm"
                }`}
              >
                {msg.contenu}
                <div
                  className={`text-[10px] mt-1 flex items-center gap-2 justify-end ${
                    isMine(msg) ? "text-white/70" : "text-slate-400"
                  }`}
                >
                  {msg.pending && <span>Envoi...</span>}
                  {msg.failed && (
                    <button
                      type="button"
                      onClick={() => handleRetry(msg)}
                      className="underline font-semibold text-amber-200 hover:text-white"
                    >
                      Échec — renvoyer
                    </button>
                  )}
                  <span>{msg.time}</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* input */}
        <form onSubmit={handleSend} className="flex items-center gap-2 p-3 border-t border-slate-100">
          <button
            type="button"
            aria-label="Ajouter un emoji"
            title="Emoji (bientôt disponible)"
            className="w-9 h-9 rounded-full hover:bg-slate-50 flex items-center justify-center flex-shrink-0"
          >
            <Smile className="w-5 h-5 text-slate-400" />
          </button>
          <input
            type="text"
            value={draft}
            onChange={(e) => handleDraftChange(e.target.value)}
            placeholder="Écrire un message..."
            aria-label="Écrire un message"
            className="flex-1 px-4 py-2.5 text-sm rounded-full bg-slate-50 border border-transparent focus:outline-none focus:ring-2 focus:ring-violet-400"
          />
          <button
            type="submit"
            disabled={sending || !draft.trim()}
            className="w-9 h-9 rounded-full bg-violet-600 hover:bg-violet-700 transition-colors flex items-center justify-center flex-shrink-0 disabled:opacity-50"
            aria-label="Envoyer le message"
          >
            <Send className="w-4 h-4 text-white" />
          </button>
        </form>
      </div>
    </div>
  );
}
