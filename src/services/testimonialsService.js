import { api } from "../lib/api";

/**
 * Service témoignages (section publique de la landing page).
 * Réponse attendue: [{ id, name, role, quote, photo }]
 */
export function getTestimonials() {
  return api.get("/testimonials");
}
