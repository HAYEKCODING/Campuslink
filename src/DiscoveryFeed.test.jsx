import React from "react";
import { describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import DiscoveryFeed from "./DiscoveryFeed";

vi.mock("./Layout", () => ({
  default: ({ children }) => children,
}));

vi.mock("./hooks/useAsyncData", () => ({
  useAsyncData: () => ({
    data: {
      profiles: [{
        id: "profile-1",
        legacyId: 12,
        name: "Aminata Dao",
        interests: [],
      }],
      subscribedProfileIds: new Set([12]),
    },
    loading: false,
    error: null,
    reload: () => {},
  }),
}));

vi.mock("./services/profileService", () => ({
  getDiscoveryProfiles: vi.fn(),
  getSubscribedProfileIds: vi.fn(),
  subscribeToProfile: vi.fn(),
}));

describe("DiscoveryFeed", () => {
  it("shows an abonnement status instead of the subscribe icon for an existing subscription", () => {
    const markup = renderToStaticMarkup(
      <MemoryRouter>
        <DiscoveryFeed />
      </MemoryRouter>
    );

    expect(markup).toContain("Déjà abonné au profil de Aminata Dao");
    expect(markup).toContain(">Abonné</span>");
    expect(markup).not.toContain("S'abonner au profil de Aminata Dao");
  });
});
