import { useCallback, useEffect, useRef, useState } from "react";
import { Camera, CameraOff, Mic, MicOff, Phone, PhoneOff, Video, X } from "lucide-react";
import { getRealtimeClient, sendCallSignal } from "../lib/realtime";
import { getCallIceServers } from "../services/callsService";
import { getMatches } from "../services/matchesService";

function newCallId() {
  if (!globalThis.crypto?.randomUUID) {
    throw new Error("Ce navigateur ne permet pas de sécuriser l'identifiant de l'appel.");
  }
  return globalThis.crypto.randomUUID();
}

export default function CallManager() {
  const [call, setCall] = useState({ status: "idle" });
  const [localStream, setLocalStream] = useState(null);
  const [remoteStream, setRemoteStream] = useState(null);
  const [microphoneEnabled, setMicrophoneEnabled] = useState(true);
  const [cameraEnabled, setCameraEnabled] = useState(true);
  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);
  const remoteAudioRef = useRef(null);
  const callRef = useRef(call);
  const peerRef = useRef(null);
  const localStreamRef = useRef(null);
  const remoteStreamRef = useRef(null);
  const iceServersRef = useRef([]);
  const pendingCandidatesRef = useRef([]);
  const disconnectTimerRef = useRef(null);
  const callTimeoutRef = useRef(null);
  const ringContextRef = useRef(null);

  const updateCall = useCallback((next) => {
    callRef.current = next;
    setCall(next);
  }, []);

  const stopRinging = useCallback(() => {
    if (ringContextRef.current) {
      ringContextRef.current.close().catch((error) => {
        console.warn("Impossible d'arrêter la sonnerie d'appel.", error);
      });
      ringContextRef.current = null;
    }
  }, []);

  const cleanupMedia = useCallback(() => {
    clearTimeout(disconnectTimerRef.current);
    disconnectTimerRef.current = null;
    clearTimeout(callTimeoutRef.current);
    callTimeoutRef.current = null;
    if (peerRef.current) {
      peerRef.current.onicecandidate = null;
      peerRef.current.ontrack = null;
      peerRef.current.onconnectionstatechange = null;
      peerRef.current.close();
      peerRef.current = null;
    }
    localStreamRef.current?.getTracks().forEach((track) => track.stop());
    localStreamRef.current = null;
    remoteStreamRef.current = null;
    pendingCandidatesRef.current = [];
    iceServersRef.current = [];
    setLocalStream(null);
    setRemoteStream(null);
    setMicrophoneEnabled(true);
    setCameraEnabled(true);
    stopRinging();
  }, [stopRinging]);

  const signal = useCallback((type, payload, mediaType) => {
    const current = callRef.current;
    if (!current.matchId || !current.callId) return false;
    return sendCallSignal({
      matchId: Number(current.matchId),
      callId: current.callId,
      type,
      mediaType: mediaType || current.mediaType,
      payload: payload || null,
    });
  }, []);

  const createPeer = useCallback((current) => {
    if (peerRef.current) return peerRef.current;
    if (!globalThis.RTCPeerConnection) {
      throw new Error("Ce navigateur ne prend pas en charge les appels WebRTC.");
    }
    const peer = new RTCPeerConnection({ iceServers: iceServersRef.current });
    peerRef.current = peer;
    localStreamRef.current?.getTracks().forEach((track) => peer.addTrack(track, localStreamRef.current));
    peer.onicecandidate = (event) => {
      if (event.candidate && !signal("ICE", event.candidate.toJSON())) {
        updateCall({ ...callRef.current, status: "error", message: "Connexion temps réel perdue." });
      }
    };
    peer.ontrack = (event) => {
      let stream = event.streams[0];
      if (!stream) {
        stream = remoteStreamRef.current || new MediaStream();
        stream.addTrack(event.track);
      }
      remoteStreamRef.current = stream;
      setRemoteStream(stream);
    };
    peer.onconnectionstatechange = () => {
      if (peer.connectionState === "connected") {
        clearTimeout(disconnectTimerRef.current);
        disconnectTimerRef.current = null;
        updateCall({ ...callRef.current, status: "active" });
      } else if (peer.connectionState === "failed") {
        signal("END");
        cleanupMedia();
        updateCall({ ...callRef.current, status: "error", message: "La connexion audio/vidéo a échoué." });
      } else if (peer.connectionState === "disconnected") {
        disconnectTimerRef.current = setTimeout(() => {
          if (peer.connectionState === "disconnected") {
            signal("END");
            cleanupMedia();
            updateCall({ ...callRef.current, status: "error", message: "La connexion avec l'interlocuteur a été perdue." });
          }
        }, 8000);
      }
    };
    updateCall({ ...current, status: "connecting" });
    return peer;
  }, [cleanupMedia, signal, updateCall]);

  const setupLocalMedia = useCallback(async (mediaType, matchId) => {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error("La caméra et le microphone nécessitent HTTPS ou localhost.");
    }
    const [config, stream] = await Promise.all([
      getCallIceServers(matchId),
      navigator.mediaDevices.getUserMedia({
        audio: true,
        video: mediaType === "VIDEO" ? { facingMode: "user" } : false,
      }),
    ]);
    if (!Array.isArray(config?.iceServers) || config.iceServers.length === 0) {
      stream.getTracks().forEach((track) => track.stop());
      throw new Error("Aucun serveur STUN/TURN n'est configuré pour établir l'appel.");
    }
    iceServersRef.current = config.iceServers;
    localStreamRef.current = stream;
    setLocalStream(stream);
    return stream;
  }, []);

  const finishCall = useCallback((status, message, notifyPeer = false) => {
    const current = callRef.current;
    if (notifyPeer && current.status !== "idle") {
      const type = current.status === "incoming" || current.status === "accepting" ? "REJECT"
        : current.status === "outgoing" ? "CANCEL"
          : current.status === "preparing" ? null : "END";
      if (type) signal(type);
    }
    cleanupMedia();
    updateCall({ ...current, status, message });
  }, [cleanupMedia, signal, updateCall]);

  const startCall = useCallback(async ({ matchId, contactName, contactPhoto, mediaType }) => {
    if (callRef.current.status !== "idle" && callRef.current.status !== "ended"
        && callRef.current.status !== "error") return;
    const next = {
      matchId: String(matchId),
      callId: newCallId(),
      contactName: contactName || "Interlocuteur",
      contactPhoto: contactPhoto || null,
      mediaType,
      status: "preparing",
    };
    updateCall(next);
    try {
      await setupLocalMedia(mediaType, matchId);
      if (callRef.current.callId !== next.callId || callRef.current.status !== "preparing") {
        cleanupMedia();
        return;
      }
      updateCall({ ...next, status: "outgoing" });
      if (!signal("INVITE", null, mediaType)) {
        throw new Error("La connexion temps réel n'est pas disponible. Réessayez.");
      }
      callTimeoutRef.current = setTimeout(() => {
        if (callRef.current.callId === next.callId && callRef.current.status === "outgoing") {
          signal("MISSED");
          cleanupMedia();
          updateCall({ ...next, status: "ended", message: "Appel manqué : aucune réponse." });
        }
      }, 45_000);
    } catch (error) {
      cleanupMedia();
      updateCall({ ...next, status: "error", message: error.message || "Impossible de démarrer l'appel." });
    }
  }, [cleanupMedia, setupLocalMedia, signal, updateCall]);

  const acceptCall = useCallback(async () => {
    const incoming = callRef.current;
    updateCall({ ...incoming, status: "accepting" });
    try {
      await setupLocalMedia(incoming.mediaType, incoming.matchId);
      if (callRef.current.callId !== incoming.callId || callRef.current.status !== "accepting") {
        cleanupMedia();
        return;
      }
      updateCall({ ...incoming, status: "connecting" });
      if (!signal("ACCEPT")) throw new Error("La connexion temps réel n'est pas disponible.");
    } catch (error) {
      cleanupMedia();
      signal("REJECT");
      updateCall({ ...incoming, status: "error", message: error.message || "Impossible d'accéder au microphone ou à la caméra." });
    }
  }, [cleanupMedia, setupLocalMedia, signal, updateCall]);

  const flushCandidates = useCallback(async (peer) => {
    const queued = pendingCandidatesRef.current.splice(0);
    await Promise.all(queued.map((candidate) => peer.addIceCandidate(new RTCIceCandidate(candidate))));
  }, []);

  const handleSignal = useCallback(async (event) => {
    const current = callRef.current;
    if (event.type === "INVITE") {
      if (current.status !== "idle" && current.status !== "ended" && current.status !== "error") {
        sendCallSignal({
          matchId: Number(event.matchId),
          callId: event.callId,
          type: "REJECT",
          mediaType: event.mediaType,
          payload: null,
        });
        return;
      }
      updateCall({
        matchId: String(event.matchId),
        callId: event.callId,
        mediaType: event.mediaType,
        contactName: event.contactName || "Appel entrant",
        contactPhoto: event.contactPhoto || null,
        status: "incoming",
      });
      getMatches().then((matches) => {
        if (callRef.current.callId !== event.callId) return;
        const match = matches.find((item) => String(item.id) === String(event.matchId));
        if (match) {
          updateCall({
            ...callRef.current,
            contactName: match.autreUtilisateurNom || "Appel entrant",
            contactPhoto: match.autreUtilisateurPhoto || null,
          });
        }
      }).catch((error) => {
        console.warn("Impossible de charger le nom de l'appelant.", error);
      });
      try {
        const context = new AudioContext();
        ringContextRef.current = context;
        const beep = () => {
          if (context.state === "closed" || callRef.current.status !== "incoming") return;
          const oscillator = context.createOscillator();
          const gain = context.createGain();
          oscillator.frequency.value = 440;
          gain.gain.value = 0.035;
          oscillator.connect(gain);
          gain.connect(context.destination);
          oscillator.start();
          oscillator.stop(context.currentTime + 0.35);
          setTimeout(beep, 1300);
        };
        beep();
      } catch (error) {
        console.warn("La sonnerie audio n'est pas disponible dans ce navigateur.", error);
      }
      return;
    }
    if (current.callId !== event.callId || String(current.matchId) !== String(event.matchId)) return;

    try {
      switch (event.type) {
        case "ACCEPT": {
          const peer = createPeer(current);
          const offer = await peer.createOffer();
          await peer.setLocalDescription(offer);
          if (!signal("OFFER", peer.localDescription.toJSON())) throw new Error("Signalisation WebSocket indisponible.");
          break;
        }
        case "OFFER": {
          const peer = createPeer(current);
          await peer.setRemoteDescription(new RTCSessionDescription(event.payload));
          await flushCandidates(peer);
          const answer = await peer.createAnswer();
          await peer.setLocalDescription(answer);
          if (!signal("ANSWER", peer.localDescription.toJSON())) throw new Error("Signalisation WebSocket indisponible.");
          break;
        }
        case "ANSWER": {
          const peer = peerRef.current;
          if (!peer) return;
          await peer.setRemoteDescription(new RTCSessionDescription(event.payload));
          await flushCandidates(peer);
          break;
        }
        case "ICE": {
          const peer = peerRef.current;
          if (!peer || !peer.remoteDescription) pendingCandidatesRef.current.push(event.payload);
          else await peer.addIceCandidate(new RTCIceCandidate(event.payload));
          break;
        }
        case "REJECT":
          finishCall("ended", "Appel refusé.");
          break;
        case "CANCEL":
          finishCall("ended", "Appel annulé.");
          break;
        case "MISSED":
          finishCall("ended", "Appel manqué.");
          break;
        case "END":
          finishCall("ended", "Appel terminé.");
          break;
        case "ERROR":
          finishCall("error", event.payload?.error || "L'appel n'a pas pu être établi.");
          break;
        default:
          break;
      }
    } catch (error) {
      signal("END");
      cleanupMedia();
      updateCall({ ...callRef.current, status: "error", message: error.message || "Erreur de connexion WebRTC." });
    }
  }, [cleanupMedia, createPeer, finishCall, flushCandidates, signal, updateCall]);

  useEffect(() => {
    const rt = getRealtimeClient();
    rt.connect();
    let subscription = { unsubscribe() {} };
    const subscribe = () => {
      subscription.unsubscribe();
      subscription = rt.subscribeUser("/queue/calls", (frame) => {
        try {
          void handleSignal(JSON.parse(frame.body));
        } catch (error) {
          console.error("Signal d'appel temps réel invalide.", error);
        }
      });
    };
    const offConnect = rt.onConnect(subscribe);
    const offDisconnect = rt.onDisconnect(() => {
      const current = callRef.current;
      if (current.status === "idle" || current.status === "ended" || current.status === "error") return;
      cleanupMedia();
      updateCall({ ...current, status: "error", message: "Connexion temps réel perdue. L'appel est terminé." });
    });
    const startListener = (event) => {
      void startCall(event.detail);
    };
    window.addEventListener("campuslink:start-call", startListener);
    return () => {
      window.removeEventListener("campuslink:start-call", startListener);
      offConnect();
      offDisconnect();
      subscription.unsubscribe();
      cleanupMedia();
    };
  }, [cleanupMedia, handleSignal, startCall, updateCall]);

  useEffect(() => {
    if (localVideoRef.current) localVideoRef.current.srcObject = localStream;
    if (call.mediaType === "VIDEO" && remoteVideoRef.current) remoteVideoRef.current.srcObject = remoteStream;
    if (call.mediaType === "AUDIO" && remoteAudioRef.current) remoteAudioRef.current.srcObject = remoteStream;
  }, [call.mediaType, localStream, remoteStream]);

  const toggleMicrophone = () => {
    const enabled = !microphoneEnabled;
    localStreamRef.current?.getAudioTracks().forEach((track) => { track.enabled = enabled; });
    setMicrophoneEnabled(enabled);
  };
  const toggleCamera = () => {
    const enabled = !cameraEnabled;
    localStreamRef.current?.getVideoTracks().forEach((track) => { track.enabled = enabled; });
    setCameraEnabled(enabled);
  };

  if (call.status === "idle") return null;

  const isVideoCall = call.mediaType === "VIDEO";
  const isIncoming = call.status === "incoming";
  const isOutgoing = call.status === "outgoing" || call.status === "preparing";
  const isActive = call.status === "active" || call.status === "connecting" || call.status === "accepting";
  const dismiss = () => updateCall({ status: "idle" });

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4" role="presentation">
      <section
        role="dialog"
        aria-modal="true"
        aria-label={isVideoCall ? "Appel vidéo" : "Appel audio"}
        className="relative flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-3xl bg-slate-900 text-white shadow-2xl"
      >
        {(call.status === "ended" || call.status === "error") && (
          <button type="button" onClick={dismiss} aria-label="Fermer" className="absolute right-3 top-3 z-20 rounded-full p-2 hover:bg-white/10">
            <X className="h-5 w-5" />
          </button>
        )}
        {isVideoCall ? (
          <div className="relative aspect-video min-h-64 bg-black">
            <video ref={remoteVideoRef} autoPlay playsInline className="h-full w-full object-cover" />
            <video ref={localVideoRef} autoPlay muted playsInline className="absolute bottom-4 right-4 aspect-video w-28 rounded-xl border border-white/50 bg-slate-800 object-cover sm:w-36" />
            {!remoteStream && <div className="absolute inset-0 flex items-center justify-center text-sm text-white/70">{isIncoming ? "Appel vidéo entrant" : "Connexion vidéo…"}</div>}
          </div>
        ) : (
          <div className="flex min-h-64 flex-col items-center justify-center gap-4 bg-gradient-to-b from-violet-950 to-slate-900 p-8">
            <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-violet-700 text-3xl font-semibold">
              {call.contactPhoto ? <img src={call.contactPhoto} alt="" className="h-full w-full object-cover" /> : call.contactName?.charAt(0)}
            </div>
            <audio ref={remoteAudioRef} autoPlay />
          </div>
        )}
        <div className="space-y-1 px-5 pt-4 text-center">
          <h2 className="text-lg font-semibold">{call.contactName || "Appel"}</h2>
          <p aria-live="polite" className={`text-sm ${call.status === "error" ? "text-rose-300" : "text-slate-300"}`}>
            {call.message || (isIncoming ? `Appel ${isVideoCall ? "vidéo" : "audio"} entrant`
              : isOutgoing ? "Appel en cours…"
                : call.status === "active" ? "En communication"
                  : call.status === "connecting" ? "Connexion à l'interlocuteur…"
                    : "Appel terminé")}
          </p>
        </div>
        <div className="flex items-center justify-center gap-4 p-5">
          {isIncoming && (
            <>
              <button type="button" onClick={() => finishCall("ended", "Appel refusé.", true)} className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-600 hover:bg-rose-700" aria-label="Refuser l'appel">
                <PhoneOff className="h-5 w-5" />
              </button>
              <button type="button" onClick={() => { stopRinging(); void acceptCall(); }} className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-600 hover:bg-emerald-700" aria-label="Accepter l'appel">
                {isVideoCall ? <Video className="h-5 w-5" /> : <Phone className="h-5 w-5" />}
              </button>
            </>
          )}
          {isOutgoing && (
            <button type="button" onClick={() => finishCall("ended", "Appel annulé.", true)} className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-600 hover:bg-rose-700" aria-label="Annuler l'appel">
              <PhoneOff className="h-5 w-5" />
            </button>
          )}
          {isActive && (
            <>
              <button type="button" onClick={toggleMicrophone} className={`flex h-11 w-11 items-center justify-center rounded-full ${microphoneEnabled ? "bg-white/15 hover:bg-white/25" : "bg-rose-600"}`} aria-label={microphoneEnabled ? "Désactiver le microphone" : "Activer le microphone"}>
                {microphoneEnabled ? <Mic className="h-5 w-5" /> : <MicOff className="h-5 w-5" />}
              </button>
              {isVideoCall && (
                <button type="button" onClick={toggleCamera} className={`flex h-11 w-11 items-center justify-center rounded-full ${cameraEnabled ? "bg-white/15 hover:bg-white/25" : "bg-rose-600"}`} aria-label={cameraEnabled ? "Désactiver la caméra" : "Activer la caméra"}>
                  {cameraEnabled ? <Camera className="h-5 w-5" /> : <CameraOff className="h-5 w-5" />}
                </button>
              )}
              <button type="button" onClick={() => finishCall("ended", "Appel terminé.", true)} className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-600 hover:bg-rose-700" aria-label="Raccrocher">
                <PhoneOff className="h-5 w-5" />
              </button>
            </>
          )}
          {(call.status === "ended" || call.status === "error") && (
            <button type="button" onClick={dismiss} className="rounded-full bg-white/15 px-5 py-2 text-sm hover:bg-white/25">Fermer</button>
          )}
        </div>
      </section>
    </div>
  );
}
