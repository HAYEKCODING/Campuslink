import { api } from "../lib/api";

export function getCallIceServers(matchId) {
  return api.get(`/matches/${matchId}/calls/ice-servers`);
}
