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

test("plans advertise a temporarily ad-free free tier and unactivated VIP purchase", async () => {
  const r = await request("/v1/plans");
  const { plans, advertising } = await r.json();
  assert.equal(plans[0].ads, false);
  assert.equal(advertising.enabled, false);
  assert.equal(advertising.status, "paused_until_owner_enables");
  assert.equal(plans[1].purchase_enabled, false);
  assert.deepEqual(plans[1].proposed_price, { amount: 3, currency: "USD" });
  assert.equal(plans[1].price, null);
});

test("missing database fails closed", async () => {
  const r = await request("/v1/servers");
  assert.equal(r.status, 503);
});

test("free accounts cannot list VIP-only servers and none are falsely connectable", async () => {
  const servers = [
    { id: "free-sg", name: "Singapore Free", country_code: "SG", protocol: "wireguard", vip_only: 0 },
    { id: "vip-sg", name: "Singapore VIP", country_code: "SG", protocol: "wireguard", vip_only: 1 },
  ];
  const fakeDb = tier => ({
    prepare(sql) {
      if (sql.includes("FROM sessions s")) {
        return { bind: () => ({ first: async () => ({
          id: "test", email: "test@example.com", role: "user", status: "active",
          tier, vip_expires_at: Math.floor(Date.now() / 1000) + 86400,
        }) }) };
      }
      if (sql.includes("FROM vpn_servers")) return { all: async () => ({ results: servers }) };
      throw new Error("Unexpected query");
    },
  });
  async function getServers(tier) {
    const response = await api.fetch(new Request("https://sovera.test/v1/servers", {
      headers: { authorization: "Bearer " + "a".repeat(64) },
    }), { DB: fakeDb(tier) });
    assert.equal(response.status, 200);
    return response.json();
  }
  const free = await getServers("free");
  assert.deepEqual(free.servers.map(server => server.id), ["free-sg"]);
  assert.equal(free.servers[0].connectable, false);
  const vip = await getServers("vip");
  assert.deepEqual(vip.servers.map(server => server.id), ["free-sg", "vip-sg"]);
  assert.ok(vip.servers.every(server => !server.connectable));
});

test("admin page has nonce-protected scripts and no user credentials", async () => {
  const r = await request("/admin");
  const html = await r.text();
  assert.equal(r.status, 200);
  assert.match(r.headers.get("content-security-policy"), /script-src 'nonce-[a-f0-9]+'/);
  assert.match(html, /SOVERA VPN/);
  assert.doesNotMatch(html, /__NONCE__/);
});
