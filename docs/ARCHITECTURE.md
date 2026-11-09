# SOVERA VPN — Architecture

## Roles of the components

1. **Android app:** account signup/login, visible server list, VPN permission and connection controls. It must not claim a tunnel is connected before a proven encrypted session and traffic tests.
2. **Cloudflare Worker control API:** secure authentication, role enforcement, entitlement lookup, server catalog and admin actions. HTTPS only.
3. **D1:** users, password hashes, session hashes, entitlements, server metadata and audit records.
4. **VPN endpoint(s), not yet provided:** Linux hosts running an authorized WireGuard/OpenVPN or other audited engine. The endpoint actually receives and forwards encrypted user traffic. It must be deployed and protected separately.
5. **Google Play Billing:** monthly subscription sold through Play when product `sovera_vpn_monthly` is configured. Purchase state must be verified on the server with the Google Play Developer API, and subscription notifications must be handled.
6. **Advertising (deferred):** neither tier shows ads now. Any future in-app ads require an explicit product decision, policy review, appropriate user consent and separate implementation. Never modify or inject ads into tunneled network traffic.

## Trust boundary

Android never receives administrative credentials. Cloudflare Workers do not store raw VPN private keys in D1. A future provisioning connector must authenticate to the user's own VPN endpoints using secrets stored in protected Cloudflare bindings, not committed values.

Public account registration creates users with role `user`. Only an existing admin can manage accounts or servers. There is deliberately no public `makeAdmin` API. Session tokens are random and stored as SHA-256 digests; the app sends tokens in the Authorization header.

## Recommended connection flow (pending transport integration)

1. Sign in to the Cloudflare API.
2. Fetch approved server metadata; filter by valid VIP entitlement.
3. Request a short-lived, per-user VPN profile from a secure provisioning API (not implemented).
4. Obtain Android `VpnService` consent and establish a real tunnel using a vetted protocol library.
5. Validate encrypted connectivity, DNS, leak prevention, disconnect, background behavior and revocation before showing Connected.
6. Log only minimal operational metadata permitted by the disclosed privacy policy.

## Current API endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | Honest service state; VPN transport not configured |
| GET | `/v1/plans` | Ad-free Free tier and proposed monthly VIP plan; purchases disabled |
| POST | `/v1/auth/register` | Create standard account |
| POST | `/v1/auth/login` | Obtain session token |
| POST | `/v1/auth/logout` | Revoke session |
| GET | `/v1/me` | Signed-in account and entitlement |
| GET | `/v1/servers` | Approved server metadata, no live profiles |
| GET | `/v1/admin/users` | Admin-only user list |
| PATCH | `/v1/admin/users/:id` | Admin block/unblock |
| POST | `/v1/admin/users/:id/vip` | Manual VIP grant, 1-365 days |
| GET/POST | `/v1/admin/servers` | Admin server catalog metadata |
| GET | `/admin` | Responsive admin control panel |

## Important blockers

The original APK and the JIYAM screenshot are references only. They do not prove ownership of an existing VPN service or reveal an authorized reusable server contract. A Cloudflare Worker cannot on its own create carrier-sponsored mobile data or turn application-specific data allowances into general free Internet access.
