import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft, Phone, Video, MoreVertical, Smile, Send, Paperclip, FolderOpen,
  FileText, Download, X, ChevronLeft, ChevronRight, ExternalLink, LoaderCircle,
} from "lucide-react";
import { useAsyncData } from "./hooks/useAsyncData";
import {
  getChatMediaContent, getChatMediaLibrary, getMessages, markMessagesRead,
  sendChatMedia, sendMessage,
} from "./services/messagesService";
import { getMatches } from "./services/matchesService";
import { getMyProfile } from "./services/profileService";
import { getRealtimeClient, sendTyping } from "./lib/realtime";
import { LoadingState, ErrorState } from "./components/ui/AsyncStates";

const EMOJIS = ["😀", "😂", "🥰", "😍", "😉", "😊", "🤩", "😎", "😭", "😅", "🙏", "👏", "👍", "❤️", "🔥", "🎉", "✨", "💯"];
const MAX_CHAT_MEDIA_BYTES = 50 * 1024 * 1024;
const CHAT_MEDIA_ACCEPT = ".jpg,.jpeg,.png,.webp,.gif,.mp4,.mov,.webm,.pdf,.doc,.docx";
const URL_PATTERN = /(https?:\/\/[^\s<>]+)/gi;

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes)) return "";
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

function renderMessageText(content) {
  if (!content) return null;
  return content.split(URL_PATTERN).map((part, index) => {
    if (!/^https?:\/\//i.test(part)) return part;
    const suffix = part.match(/[.,!?;:)\]}]+$/)?.[0] || "";
    const href = suffix ? part.slice(0, -suffix.length) : part;
    return (
      <React.Fragment key={`${index}-${href}`}>
        <a href={href} target="_blank" rel="noopener noreferrer" className="underline break-all">
          {href}
        </a>
        {suffix}
      </React.Fragment>
    );
  });
}

function ChatMediaPreview({ matchId, media, compact = false }) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const isImage = media?.contentType?.startsWith("image/");
  const isVideo = media?.contentType?.startsWith("video/");

  useEffect(() => {
    let active = true;
    let objectUrl = "";
    setLoading(true);
    setError("");
    getChatMediaContent(matchId, media.messageId)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch((err) => {
        if (active) setError(err.message || "Impossible de charger ce fichier.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [matchId, media.messageId]);

  const download = () => {
    if (!url) return;
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = media.fileName;
    anchor.click();
  };

  const open = () => {
    if (url) window.open(url, "_blank", "noopener,noreferrer");
  };

  if (loading) {
    return <div className="flex items-center gap-2 py-2 text-xs text-current/70"><LoaderCircle className="h-4 w-4 animate-spin" />Chargement du fichier…</div>;
  }
  if (error) return <p role="alert" className="py-2 text-xs text-rose-200">{error}</p>;

  return (
    <div className={`space-y-2 ${compact ? "max-w-56" : "w-full"}`}>
      {isImage && <img src={url} alt={media.fileName} className="max-h-64 w-full rounded-lg object-contain" />}
      {isVideo && <video src={url} controls preload="metadata" className="max-h-64 w-full rounded-lg" />}
      {!isImage && !isVideo && (
        <div className="flex items-center gap-2 rounded-lg bg-black/5 p-2 text-xs">
          <FileText className="h-5 w-5 flex-shrink-0" />
          <span className="min-w-0 flex-1 truncate">{media.fileName}</span>
          <span>{formatFileSize(media.sizeBytes)}</span>
        </div>
      )}
      <div className="flex items-center gap-3 text-xs">
        {url && <button type="button" onClick={open} className="inline-flex items-center gap-1 underline"><ExternalLink className="h-3.5 w-3.5" />Ouvrir</button>}
        <button type="button" onClick={download} className="inline-flex items-center gap-1 underline"><Download className="h-3.5 w-3.5" />Télécharger</button>
      </div>
    </div>
  );
}

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
  const [emojiPickerOpen, setEmojiPickerOpen] = useState(false);
  const [messageError, setMessageError] = useState("");
  const [mediaUploading, setMediaUploading] = useState(false);
  const [mediaLibraryOpen, setMediaLibraryOpen] = useState(false);
  const [mediaCategory, setMediaCategory] = useState("MEDIA");
  const [mediaPage, setMediaPage] = useState(0);
  const [mediaLibrary, setMediaLibrary] = useState(null);
  const [mediaLibraryLoading, setMediaLibraryLoading] = useState(false);
  const [mediaLibraryError, setMediaLibraryError] = useState("");
  // Déclaré AVANT tout early return (loading/erreur) : un hook placé après un
  // return conditionnel casse la règle des hooks (« Rendered more hooks than
  // during the previous render ») et fait planter la conversation.
  const typingTimerRef = useRef(null);
  const messagesEndRef = useRef(null);
  const uploadInputRef = useRef(null);

  useEffect(() => {
    if (!mediaLibraryOpen) return undefined;
    let active = true;
    setMediaLibraryLoading(true);
    setMediaLibraryError("");
    getChatMediaLibrary(matchId, mediaCategory, mediaPage)
      .then((result) => {
        if (active) setMediaLibrary(result);
      })
      .catch((err) => {
        if (active) setMediaLibraryError(err.message || "Impossible de charger la bibliothèque.");
      })
      .finally(() => {
        if (active) setMediaLibraryLoading(false);
      });
    return () => { active = false; };
  }, [matchId, mediaCategory, mediaLibraryOpen, mediaPage]);

  useEffect(() => {
    if (!history) return;
    setMessages((current) => {
      const merged = new Map();
      [...current, ...history].forEach((message, index) => {
        const key = message.id == null ? `pending-${index}` : String(message.id);
        merged.set(key, { ...message, time: formatTime(message.dateEnvoi) });
      });
      return [...merged.values()].sort(
        (left, right) => new Date(left.dateEnvoi || 0) - new Date(right.dateEnvoi || 0)
      );
    });
    markMessagesRead(matchId).catch((err) => {
      setMessageError(err.message || "Impossible de mettre les messages à jour comme lus.");
    });
  }, [history, matchId]);

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
        if (incoming.id != null && prev.some((m) => String(m.id) === String(incoming.id))) return prev;
        return [...prev, { ...incoming, time: formatTime(incoming.dateEnvoi) }];
      });
      markMessagesRead(matchId).catch((err) => {
        setMessageError(err.message || "Impossible de mettre les messages à jour comme lus.");
      });
    });
    const receipts = rt.subscribeUser("/queue/read-receipts", (frame) => {
      try {
        const receipt = JSON.parse(frame.body);
        if (String(receipt.matchId) !== String(matchId)) return;
        setMessages((prev) =>
          prev.map((message) =>
            (message.fromMe ?? (me && message.expediteurId === me.legacyId)) &&
            String(message.matchId) === String(matchId)
              ? { ...message, statutLecture: "LU" }
              : message
          )
        );
      } catch {
        setMessageError("Un accusé de lecture reçu est invalide.");
      }
    });
    return () => {
      subscription.unsubscribe();
      receipts.unsubscribe();
    };
  }, [matchId, me]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

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

  useEffect(() => () => clearTimeout(typingTimerRef.current), []);


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
      matchId: Number(matchId),
      statutLecture: "ENVOYE",
      contenu: text,
      time: new Date().toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, optimistic]);
    setDraft("");
    setSending(true);
    setMessageError("");

    sendMessage(matchId, text)
      .then((sent) => {
        setMessages((prev) =>
          prev.map((m) =>
            m === optimistic
              ? {
                  ...sent,
                  fromMe: true,
                  time: formatTime(sent.dateEnvoi),
                  pending: false,
                  statutLecture: m.statutLecture === "LU" ? "LU" : sent.statutLecture,
                }
              : m
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

  const handleUpload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || mediaUploading) return;
    if (file.size > MAX_CHAT_MEDIA_BYTES) {
      setMessageError("Le fichier dépasse la taille maximale autorisée de 50 Mo.");
      return;
    }

    setMediaUploading(true);
    setMessageError("");
    try {
      const sent = await sendChatMedia(matchId, file);
      setMessages((prev) => [...prev, { ...sent, time: formatTime(sent.dateEnvoi) }]);
    } catch (err) {
      setMessageError(err.message || "Échec de l’envoi du fichier. Vérifiez le fichier et réessayez.");
    } finally {
      setMediaUploading(false);
    }
  };

  const addEmoji = (emoji) => {
    handleDraftChange(`${draft}${emoji}`);
    setEmojiPickerOpen(false);
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
            aria-label="Ouvrir la bibliothèque de la conversation"
            title="Médias, documents et liens"
            onClick={() => {
              setMediaCategory("MEDIA");
              setMediaPage(0);
              setMediaLibraryOpen(true);
            }}
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center"
          >
            <FolderOpen className="w-4 h-4 text-slate-500" />
          </button>
          <button
            type="button"
            aria-label={`Appeler ${contact.name} en audio`}
            title="Appel audio"
            onClick={() => window.dispatchEvent(new CustomEvent("campuslink:start-call", {
              detail: { matchId, contactName: contact.name, contactPhoto: contact.photo, mediaType: "AUDIO" },
            }))}
            className="w-8 h-8 rounded-full hover:bg-slate-50 flex items-center justify-center"
          >
            <Phone className="w-4 h-4 text-slate-500" />
          </button>
          <button
            type="button"
            aria-label={`Appeler ${contact.name} en vidéo`}
            title="Appel vidéo"
            onClick={() => window.dispatchEvent(new CustomEvent("campuslink:start-call", {
              detail: { matchId, contactName: contact.name, contactPhoto: contact.photo, mediaType: "VIDEO" },
            }))}
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
          {messageError && <p role="alert" className="text-xs text-rose-600">{messageError}</p>}
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
                {msg.media && (
                  <ChatMediaPreview
                    matchId={matchId}
                    media={{ ...msg.media, messageId: msg.id }}
                    compact
                  />
                )}
                {renderMessageText(msg.contenu)}
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
                  {isMine(msg) && msg.statutLecture === "LU" && <span>Lu</span>}
                  <span>{msg.time}</span>
                </div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* input */}
        <form onSubmit={handleSend} className="flex items-center gap-2 p-3 border-t border-slate-100">
          <input
            ref={uploadInputRef}
            type="file"
            accept={CHAT_MEDIA_ACCEPT}
            onChange={handleUpload}
            className="hidden"
            aria-label="Choisir une photo, vidéo ou un document"
          />
          <button
            type="button"
            aria-label="Joindre un fichier"
            title="Photo, vidéo, PDF, DOC ou DOCX (50 Mo maximum)"
            disabled={mediaUploading || sending}
            onClick={() => uploadInputRef.current?.click()}
            className="w-9 h-9 rounded-full hover:bg-slate-50 flex items-center justify-center disabled:opacity-50"
          >
            {mediaUploading
              ? <LoaderCircle className="w-5 h-5 text-violet-600 animate-spin" />
              : <Paperclip className="w-5 h-5 text-slate-400" />}
          </button>
          <div className="relative flex-shrink-0">
            <button
              type="button"
              aria-label="Ajouter un emoji"
              aria-expanded={emojiPickerOpen}
              aria-controls="chat-emoji-picker"
              onClick={() => setEmojiPickerOpen((open) => !open)}
              className="w-9 h-9 rounded-full hover:bg-slate-50 flex items-center justify-center"
            >
              <Smile className="w-5 h-5 text-slate-400" />
            </button>
            {emojiPickerOpen && (
              <div
                id="chat-emoji-picker"
                role="group"
                aria-label="Sélecteur d’emojis"
                className="absolute bottom-12 left-0 z-20 grid grid-cols-6 gap-1 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
              >
                {EMOJIS.map((emoji) => (
                  <button
                    key={emoji}
                    type="button"
                    aria-label={`Ajouter l’emoji ${emoji}`}
                    onClick={() => addEmoji(emoji)}
                    className="w-9 h-9 rounded-lg text-xl hover:bg-slate-100"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            )}
          </div>
          <input
            type="text"
            value={draft}
            maxLength={2000}
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
      {mediaLibraryOpen && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/40 p-4"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setMediaLibraryOpen(false);
          }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="chat-media-library-title"
            className="flex max-h-[min(80vh,700px)] w-full max-w-xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl"
          >
            <header className="flex items-center justify-between border-b border-slate-100 p-4">
              <div>
                <h2 id="chat-media-library-title" className="text-base font-bold text-slate-900">Bibliothèque de la conversation</h2>
                <p className="text-xs text-slate-500">{contact.name}</p>
              </div>
              <button type="button" onClick={() => setMediaLibraryOpen(false)} aria-label="Fermer la bibliothèque" className="rounded-full p-2 hover:bg-slate-100">
                <X className="h-4 w-4" />
              </button>
            </header>
            <div className="flex gap-1 border-b border-slate-100 px-3">
              {[
                ["MEDIA", "Médias"],
                ["DOCUMENTS", "Documents"],
                ["LINKS", "Liens"],
              ].map(([category, label]) => (
                <button
                  key={category}
                  type="button"
                  onClick={() => { setMediaCategory(category); setMediaPage(0); }}
                  className={`border-b-2 px-3 py-3 text-sm ${mediaCategory === category ? "border-violet-600 font-semibold text-violet-700" : "border-transparent text-slate-500"}`}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto p-4">
              {mediaLibraryLoading && <p className="py-8 text-center text-sm text-slate-500">Chargement…</p>}
              {mediaLibraryError && <p role="alert" className="py-8 text-center text-sm text-rose-600">{mediaLibraryError}</p>}
              {!mediaLibraryLoading && !mediaLibraryError && mediaLibrary?.content?.length === 0 && (
                <p className="py-8 text-center text-sm text-slate-500">Aucun élément dans cette catégorie.</p>
              )}
              {!mediaLibraryLoading && !mediaLibraryError && mediaLibrary?.content?.map((item) => (
                <article key={item.messageId} className="mb-3 rounded-xl border border-slate-100 p-3">
                  {item.category === "LINKS" ? (
                    <div className="space-y-2">
                      {item.links?.map((link) => (
                        <a key={link} href={link} target="_blank" rel="noopener noreferrer" className="block break-all text-sm text-violet-700 underline">
                          <ExternalLink className="mr-1 inline h-3.5 w-3.5" />{link}
                        </a>
                      ))}
                      <p className="text-xs text-slate-400">{item.senderId === me?.legacyId ? "Vous" : contact.name} · {formatTime(item.dateEnvoi)}</p>
                    </div>
                  ) : (
                    <>
                      <ChatMediaPreview
                        matchId={matchId}
                        media={{ ...item, messageId: item.messageId }}
                      />
                      <p className="mt-2 text-xs text-slate-400">{item.senderId === me?.legacyId ? "Vous" : contact.name} · {formatTime(item.dateEnvoi)}</p>
                    </>
                  )}
                </article>
              ))}
            </div>
            <footer className="flex items-center justify-between border-t border-slate-100 p-3">
              <span className="text-xs text-slate-500">
                {mediaLibrary?.totalElements ?? 0} élément{mediaLibrary?.totalElements === 1 ? "" : "s"}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={!mediaLibrary || mediaPage <= 0 || mediaLibraryLoading}
                  onClick={() => setMediaPage((page) => Math.max(0, page - 1))}
                  aria-label="Page précédente"
                  className="rounded-lg p-2 hover:bg-slate-100 disabled:opacity-40"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  disabled={!mediaLibrary || mediaPage + 1 >= mediaLibrary.totalPages || mediaLibraryLoading}
                  onClick={() => setMediaPage((page) => page + 1)}
                  aria-label="Page suivante"
                  className="rounded-lg p-2 hover:bg-slate-100 disabled:opacity-40"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
