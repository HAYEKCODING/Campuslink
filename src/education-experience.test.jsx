import React from "react";
import { describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import CampusLinkLanding from "./CampusLinkLanding";
import MyMatches from "./MyMatches";
import ResetPasswordPage from "./ResetPasswordPage";
import tailwindConfig from "../tailwind.config.js";

vi.mock("./hooks/useAsyncData", () => ({
  useAsyncData: () => ({ data: null, loading: false, error: null, reload: () => {} }),
}));

function renderInRouter(Component, initialEntry = "/") {
  return renderToStaticMarkup(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Component />
    </MemoryRouter>
  );
}

describe("CampusLink educational experience", () => {
  it("uses a vivid blue and cyan palette", () => {
    const colors = tailwindConfig.theme.extend.colors;

    expect(colors.brand[600]).toBe("#2563eb");
    expect(colors.accent[500]).toBe("#0891b2");
  });

  it("presents the platform as a place for study and collaboration", () => {
    const markup = renderInRouter(CampusLinkLanding);

    expect(markup).toContain("Recherche par centres");
    expect(markup).toContain("Trouvez des binômes de révision et des partenaires de projet");
    expect(markup).not.toMatch(/amour|match|like|❤|♥|💖|💕/i);
  });

  it("shows reciprocal subscriptions as contacts without heart branding", () => {
    const contactsMarkup = renderInRouter(MyMatches);
    const resetMarkup = renderInRouter(ResetPasswordPage, "/reset-password?token=test");

    expect(contactsMarkup).toContain("Mes contacts");
    expect(contactsMarkup).toContain("abonnez-vous");
    expect(contactsMarkup).not.toMatch(/mes matchs|nouveau match/i);
    expect(resetMarkup).not.toContain("lucide-heart");
  });
});
