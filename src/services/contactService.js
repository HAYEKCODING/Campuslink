import { api } from "../lib/api";

/**
 * Service contact (formulaire public de la landing page).
 * payload attendu: { name, email, message }
 */
export function sendContactMessage(payload) {
  return api.post("/contact", payload);
}
