import { useEffect, useState, useCallback } from "react";

/**
 * Hook générique pour appeler un service et suivre son état de chargement.
 *
 * const { data, loading, error, reload } = useAsyncData(() => getMatches(), [dependency]);
 *
 * - data: null tant que rien n'est chargé, puis la réponse du service
 * - loading: true pendant l'appel
 * - error: message d'erreur si l'appel échoue (ex: backend pas encore prêt)
 * - reload: relance manuellement l'appel
 */
export function useAsyncData(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetcher()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Une erreur est survenue");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => load(), [load]);

  return { data, loading, error, reload: load };
}
