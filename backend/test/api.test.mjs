import test from "node:test";
import assert from "node:assert/strict";
import api from "../src/index.js";

async function request(path, method = "GET", body) {
  const headers = body ? { "content-type": "application/json" } : {};
  return api.fetch(new Request("https://sovera.test" + path, {
    method, headers, ...(body ? { body: JSON.stringify(body) } : {}),
  }), {});
}

test("health honestly reports missing VPN transport", async () => {
  const r = await request("/health");
  assert.equal(r.status, 200);
  assert.equal((await r.json()).vpn_transport, "not_configured");
});

test("plans advertise free ads and unactivated VIP purchase", async () => {
  const r = await request("/v1/plans");
  const { plans } = await r.json();
  assert.equal(plans[0].ads, true);
  assert.equal(plans[1].purchase_enabled, false);
});

test("missing database fails closed", async () => {
  const r = await request("/v1/servers");
  assert.equal(r.status, 503);
});

test("admin page has nonce-protected scripts and no user credentials", async () => {
  const r = await request("/admin");
  const html = await r.text();
  assert.equal(r.status, 200);
  assert.match(r.headers.get("content-security-policy"), /script-src 'nonce-[a-f0-9]+'/);
  assert.match(html, /SOVERA VPN/);
  assert.doesNotMatch(html, /__NONCE__/);
});
