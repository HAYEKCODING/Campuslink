import React from "react";
import { Loader2, Inbox, AlertTriangle } from "lucide-react";

export function LoadingState({ label = "Chargement..." }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-16 text-slate-400">
      <Loader2 className="w-6 h-6 animate-spin" />
      <p className="text-sm">{label}</p>
    </div>
  );
}

export function EmptyState({ label = "Rien à afficher pour l'instant." }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-16 text-slate-400">
      <Inbox className="w-8 h-8" strokeWidth={1.5} />
      <p className="text-sm">{label}</p>
    </div>
  );
}

export function ErrorState({ label = "Impossible de charger les données.", onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-slate-400">
      <AlertTriangle className="w-8 h-8 text-amber-400" strokeWidth={1.5} />
      <p className="text-sm text-center max-w-xs">{label}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="text-sm font-semibold text-violet-600 hover:underline"
        >
          Réessayer
        </button>
      )}
    </div>
  );
}
