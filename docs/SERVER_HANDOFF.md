# SOVERA VPN — server handoff and integration readiness

Updated November 9, 2026. The owner has confirmed the subscription proposal of **US$3/month**, but **does not know the VPN server provider or whether account access is available**.

## Findings verified against the connected Cloudflare account

- Cloudflare D1 `sovera-vpn-staging` exists and its initial five application tables were migrated.
- Cloudflare Worker `sovera-vpn-control-staging` exists with its D1 binding; the public Worker subdomain is disabled.
- The account currently has no Cloudflare Tunnel resources. This does **not** prove there is no VPN server at another provider.
- No real VPN server's provider, IP, ownership, WireGuard/OpenVPN configuration or valid SSH access is known.
- Cloudflare Workers and D1 are the account/control plane, **not** a general-purpose UDP or TCP VPN gateway.

## Information needed from the owner before a real connection is possible

1. Hosting provider name and control-panel URL (e.g. DigitalOcean, Contabo, Hetzner, a company VPS or a physical machine).
2. Authorized VPS public IP/domain and country, operating system, and whether the owner can open the provider dashboard or manage the machine via SSH with sudo/root privileges.
3. Installed VPN technology, if known (prefer WireGuard for a new implementation; OpenVPN may be used if already configured).
4. Server capacity, expected concurrent users, bandwidth quota and acceptable-use terms for reselling VPN access.
5. DNS/domain ownership, firewall/network policy and whether UDP is permitted.

**Do not transmit passwords, private keys, recovery codes or full VPN configuration files in chat or commit them to GitHub.** Share the provider name and non-secret inventory first; use a secure credential channel if provisioning later requires secrets.

## Integration sequence after access is verified

1. Verify the endpoint is owned/authorized, reachable and supports a documented VPN protocol; create an isolated test environment.
2. Install/configure the gateway, firewall, routing, DNS leak protections, per-user keys and revocation.
3. Integrate Android `VpnService` with a maintained protocol implementation; obtain platform permission explicitly and verify connect/disconnect, roaming, DNS and traffic accounting on actual devices.
4. Secure the Cloudflare provisioning API with authentication, rate limits and abuse protection; bind VPN client keys to a user and an approved plan.
5. Add Google Play Billing product `sovera_vpn_monthly` at proposed $3/month, server-side purchase verification, renewals, cancellations and refunds. Display the **localized price returned by Google Play**, which may differ from US$3.
6. Add AdMob App ID and ad-unit IDs and consent flow where required. Free use still requires an authorized underlying data connection; it is not a method of making all carrier data free.
7. Test privacy, Play VPN declarations, billing and ads; build/sign an AAB and run the Play Console test tracks before public release.

## Owner-facing handoff question

**Can you open the company/provider dashboard for your existing VPN server?** If unsure, send a screenshot of the server provider's dashboard showing the provider name and general server type, with credentials and sensitive IP or private configuration redacted.
