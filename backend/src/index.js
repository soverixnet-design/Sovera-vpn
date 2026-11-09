import { adminPage } from "./admin-page.js";

const MAX_BODY_BYTES = 8192;
const SESSION_SECONDS = 60 * 60 * 24 * 14;
const encoder = new TextEncoder();

function reply(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
      ...headers,
    },
  });
}

function randomHex(size) {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
}

async function digestHex(input) {
  const hash = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(input)));
  return Array.from(hash, b => b.toString(16).padStart(2, "0")).join("");
}

async function hashPassword(password, salt) {
  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt: encoder.encode(salt), iterations: 210000, hash: "SHA-256" }, key, 256);
  return Array.from(new Uint8Array(bits), b => b.toString(16).padStart(2, "0")).join("");
}

function equalHex(left, right) {
  if (typeof left !== "string" || typeof right !== "string") return false;
  const max = Math.max(left.length, right.length);
  let diff = left.length ^ right.length;
  for (let i = 0; i < max; i++) diff |= (left.charCodeAt(i) || 0) ^ (right.charCodeAt(i) || 0);
  return diff === 0;
}

async function readJson(request) {
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) throw new HttpError(415, "JSON required");
  if (!request.body) throw new HttpError(400, "Request body required");
  const reader = request.body.getReader();
  const pieces = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > MAX_BODY_BYTES) throw new HttpError(413, "Request too large");
      pieces.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(size);
  let index = 0;
  for (const piece of pieces) { bytes.set(piece, index); index += piece.byteLength; }
  try {
    const data = JSON.parse(new TextDecoder().decode(bytes));
    if (!data || Array.isArray(data) || typeof data !== "object") throw Error("invalid");
    return data;
  } catch { throw new HttpError(400, "Invalid JSON object"); }
}

class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

async function getUser(request, db) {
  const authorization = request.headers.get("authorization") || "";
  const match = /^Bearer ([0-9a-f]{64})$/.exec(authorization);
  if (!match) throw new HttpError(401, "Authentication required");
  const tokenHash = await digestHex(match[1]);
  const user = await db.prepare(
    `SELECT u.id, u.email, u.role, u.status, e.tier, e.expires_at AS vip_expires_at
     FROM sessions s JOIN users u ON u.id=s.user_id
     LEFT JOIN entitlements e ON e.user_id=u.id
     WHERE s.token_hash=? AND s.expires_at>?`
  ).bind(tokenHash, Math.floor(Date.now() / 1000)).first();
  if (!user || user.status !== "active") throw new HttpError(401, "Session expired or revoked");
  return user;
}

function activeTier(user) {
  return user.tier === "vip" && Number(user.vip_expires_at) > Math.floor(Date.now() / 1000) ? "vip" : "free";
}

async function requireAdmin(request, db) {
  const user = await getUser(request, db);
  if (user.role !== "admin") throw new HttpError(403, "Administrator permission required");
  return user;
}

async function issueSession(db, userId) {
  const token = randomHex(32);
  const now = Math.floor(Date.now() / 1000);
  await db.prepare("INSERT INTO sessions (token_hash,user_id,expires_at,created_at) VALUES (?,?,?,?)")
    .bind(await digestHex(token), userId, now + SESSION_SECONDS, now).run();
  return { token, expires_at: now + SESSION_SECONDS };
}

async function audit(db, actorId, action, targetId) {
  await db.prepare("INSERT INTO admin_audit (id,actor_id,action,target_id,created_at) VALUES (?,?,?,?,?)")
    .bind(crypto.randomUUID(), actorId, action, targetId, Math.floor(Date.now() / 1000)).run();
}

async function route(request, env) {
  const url = new URL(request.url);
  const { pathname } = url;
  const method = request.method;

  if (pathname === "/health" && method === "GET") {
    return reply({ name: "SOVERA VPN", api: "ready", vpn_transport: "not_configured", production_ready: false });
  }
  if (pathname === "/v1/plans" && method === "GET") {
    return reply({ plans: [
      { id: "free", ads: true, duration: "unlimited", vpn_access: "when_server_configured" },
      {
        id: "vip_monthly", ads: false, duration: "P1M",
        product_id: "sovera_vpn_monthly", price: null,
        proposed_price: { amount: 3, currency: "USD" },
        price_source: "proposal_only_google_play_not_configured",
        purchase_enabled: false,
      },
    ] });
  }
  if (pathname === "/admin" && method === "GET") {
    const nonce = randomHex(16);
    return new Response(adminPage(nonce), {
      headers: {
        "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-frame-options": "DENY",
        "content-security-policy": `default-src 'none'; script-src 'nonce-${nonce}'; style-src 'nonce-${nonce}'; connect-src 'self'; base-uri 'none'; form-action 'self'`,
      },
    });
  }

  if (!env.DB) throw new HttpError(503, "Database binding is not configured");
  const db = env.DB;

  if (pathname === "/v1/auth/register" && method === "POST") {
    const data = await readJson(request);
    const email = String(data.email || "").trim().toLowerCase();
    const password = String(data.password || "");
    if (!/^[^\s@]{1,64}@[^\s@]{1,128}\.[^\s@]{2,24}$/.test(email) || password.length < 12 || password.length > 128) {
      throw new HttpError(400, "Valid email and 12-128 character password required");
    }
    const id = crypto.randomUUID();
    const salt = randomHex(16);
    const passwordHash = await hashPassword(password, salt);
    const now = Math.floor(Date.now() / 1000);
    try {
      await db.batch([
        db.prepare("INSERT INTO users (id,email,password_salt,password_hash,created_at) VALUES (?,?,?,?,?)").bind(id, email, salt, passwordHash, now),
        db.prepare("INSERT INTO entitlements (user_id,tier,source,updated_at) VALUES (?,?,?,?)").bind(id, "free", "system", now),
      ]);
    } catch (e) {
      if (/UNIQUE|constraint/i.test(String(e))) throw new HttpError(409, "Account already exists");
      throw e;
    }
    return reply({ id, email, tier: "free", message: "Account created" }, 201);
  }

  if (pathname === "/v1/auth/login" && method === "POST") {
    const data = await readJson(request);
    const email = String(data.email || "").trim().toLowerCase();
    const password = String(data.password || "");
    if (email.length > 256 || password.length > 128 || !email || !password) throw new HttpError(401, "Incorrect email or password");
    const user = await db.prepare("SELECT id,email,password_salt,password_hash,status FROM users WHERE email=?").bind(email).first();
    if (!user || user.status !== "active" || !equalHex(await hashPassword(password, user.password_salt), user.password_hash)) {
      throw new HttpError(401, "Incorrect email or password");
    }
    return reply(await issueSession(db, user.id));
  }

  if (pathname === "/v1/me" && method === "GET") {
    const user = await getUser(request, db);
    return reply({ id: user.id, email: user.email, role: user.role, tier: activeTier(user), vip_expires_at: user.vip_expires_at });
  }
  if (pathname === "/v1/auth/logout" && method === "POST") {
    const user = await getUser(request, db);
    const match = /^Bearer ([0-9a-f]{64})$/.exec(request.headers.get("authorization") || "");
    await db.prepare("DELETE FROM sessions WHERE user_id=? AND token_hash=?")
      .bind(user.id, await digestHex(match[1])).run();
    return reply({ logged_out: true });
  }

  if (pathname === "/v1/servers" && method === "GET") {
    const user = await getUser(request, db);
    const rows = await db.prepare("SELECT id,name,country_code,protocol,vip_only,endpoint_ready FROM vpn_servers WHERE enabled=1 ORDER BY country_code,name").all();
    return reply({ tier: activeTier(user), servers: (rows.results || []).map(s => ({
      id: s.id, name: s.name, country_code: s.country_code, protocol: s.protocol,
      vip_only: Boolean(s.vip_only), connectable: false, reason: "VPN endpoint provisioning not integrated",
    })) });
  }

  if (pathname === "/v1/admin/users" && method === "GET") {
    await requireAdmin(request, db);
    const rows = await db.prepare(`SELECT u.id,u.email,u.role,u.status,e.tier,e.expires_at
      FROM users u LEFT JOIN entitlements e ON e.user_id=u.id ORDER BY u.created_at DESC LIMIT 100`).all();
    return reply({ users: rows.results || [] });
  }
  const userAction = /^\/v1\/admin\/users\/([0-9a-f-]{36})$/.exec(pathname);
  if (userAction && method === "PATCH") {
    const admin = await requireAdmin(request, db);
    const data = await readJson(request);
    if (data.status !== "active" && data.status !== "blocked") throw new HttpError(400, "Invalid user status");
    if (admin.id === userAction[1] && data.status === "blocked") throw new HttpError(400, "Cannot block own administrator account");
    const result = await db.prepare("UPDATE users SET status=? WHERE id=? AND role='user'").bind(data.status, userAction[1]).run();
    if (!result.meta?.changes) throw new HttpError(404, "User not found or protected");
    await db.prepare("DELETE FROM sessions WHERE user_id=? AND ?='blocked'").bind(userAction[1], data.status).run();
    await audit(db, admin.id, "user_status_" + data.status, userAction[1]);
    return reply({ updated: true });
  }
  const vipAction = /^\/v1\/admin\/users\/([0-9a-f-]{36})\/vip$/.exec(pathname);
  if (vipAction && method === "POST") {
    const admin = await requireAdmin(request, db);
    const data = await readJson(request);
    const days = Number(data.days);
    if (!Number.isInteger(days) || days < 1 || days > 365) throw new HttpError(400, "days must be 1-365");
    const found = await db.prepare("SELECT id FROM users WHERE id=? AND status='active'").bind(vipAction[1]).first();
    if (!found) throw new HttpError(404, "User not found");
    const now = Math.floor(Date.now() / 1000);
    const current = await db.prepare("SELECT expires_at FROM entitlements WHERE user_id=?").bind(vipAction[1]).first();
    const expires = Math.max(Number(current?.expires_at || 0), now) + days * 86400;
    await db.prepare(`INSERT INTO entitlements (user_id,tier,expires_at,source,updated_at) VALUES (?,?,?,?,?)
      ON CONFLICT(user_id) DO UPDATE SET tier=excluded.tier,expires_at=excluded.expires_at,source=excluded.source,updated_at=excluded.updated_at`)
      .bind(vipAction[1], "vip", expires, "admin", now).run();
    await audit(db, admin.id, "manual_vip_" + days + "d", vipAction[1]);
    return reply({ user_id: vipAction[1], vip_expires_at: expires, source: "admin" });
  }

  if (pathname === "/v1/admin/servers" && method === "GET") {
    await requireAdmin(request, db);
    const rows = await db.prepare("SELECT id,name,country_code,protocol,vip_only,enabled,endpoint_ready FROM vpn_servers ORDER BY created_at DESC").all();
    return reply({ servers: rows.results || [] });
  }
  if (pathname === "/v1/admin/servers" && method === "POST") {
    const admin = await requireAdmin(request, db);
    const data = await readJson(request);
    const name = String(data.name || "").trim();
    const country = String(data.country_code || "").trim().toUpperCase();
    const protocol = String(data.protocol || "").trim().toLowerCase();
    if (name.length < 3 || name.length > 70 || !/^[A-Z]{2}$/.test(country) || !["wireguard","openvpn","ssh","v2ray","udp"].includes(protocol)) {
      throw new HttpError(400, "Invalid server name, country or protocol");
    }
    const id = crypto.randomUUID();
    await db.prepare("INSERT INTO vpn_servers (id,name,country_code,protocol,vip_only,created_at) VALUES (?,?,?,?,?,?)")
      .bind(id, name, country, protocol, data.vip_only ? 1 : 0, Math.floor(Date.now() / 1000)).run();
    await audit(db, admin.id, "server_added_unconfigured", id);
    return reply({ id, configured: false, enabled: false, message: "Server metadata saved; VPN transport must be configured separately" }, 201);
  }

  throw new HttpError(404, "Endpoint not found");
}

export default {
  async fetch(request, env) {
    try {
      return await route(request, env);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      if (status >= 500) console.error(JSON.stringify({ message: "sovera_api_error", path: new URL(request.url).pathname, error: String(error) }));
      return reply({ error: status >= 500 ? "Service unavailable" : error.message }, status);
    }
  },
};
