import { api } from "../lib/api";

export const NOTIFICATIONS_CHANGED_EVENT = "campuslink:notifications-changed";

export function notifyNotificationsChanged(notification = null) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(NOTIFICATIONS_CHANGED_EVENT, { detail: notification }));
  }
}

export function getUserPreferences() {
  return api.get("/settings/me");
}

export function updateUserPreferences(preferences) {
  return api.patch("/settings/me", preferences);
}

export function getNotifications() {
  return api.get("/notifications");
}

export function getUnreadNotificationCount() {
  return api.get("/notifications/compteur").then((response) => response.nonLues);
}

export function markNotificationRead(id) {
  return api.post(`/notifications/${encodeURIComponent(id)}/lu`).then((result) => {
    notifyNotificationsChanged();
    return result;
  });
}

export function markAllNotificationsRead() {
  return api.post("/notifications/lu-tout").then((result) => {
    notifyNotificationsChanged();
    return result;
  });
}
