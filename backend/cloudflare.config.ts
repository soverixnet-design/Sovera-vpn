import { bindings, defineConfig } from "cf/config";

// A D1 database named sovera-vpn-staging must be created before deployment.
// Authentication and VPN transport credentials must never be committed here.
export default defineConfig({
  worker: {
    name: "sovera-vpn-control-staging",
    compatibilityDate: "2026-11-09",
    entrypoint: "./src/index.js",
    env: {
      DB: bindings.d1({ name: "sovera-vpn-staging" }),
    },
    observability: {
      enabled: true,
      traces: { enabled: true, headSamplingRate: 0.01 },
    },
  },
});
