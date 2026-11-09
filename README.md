# SOVERA VPN

An original, clean-room Android VPN product project. It is **not** a repackaged copy of MOHIN VIP VPN or JIYAM PLUS VPN. The visual brand, Android shell and Cloudflare control plane are new work.

> **Current state — developer preview, NOT A WORKING VPN OR PUBLISHABLE BUILD.**
>
> Cloudflare manages accounts, server metadata and VIP entitlement records; it cannot replace the actual VPN tunnel endpoint. The Android Connect button is intentionally disabled until a real protocol implementation and authorized endpoint are integrated and tested. Google Play Billing and advertising are not enabled, so there are no live charges or ads.

## What has been implemented

- Royal sapphire-and-gold SOVERA VPN logo and Android Java home screen.
- Account registration and login over HTTPS with password hashing and hashed session tokens.
- Cloudflare Worker control API prepared for D1.
- D1 schema for users, sessions, server metadata, VIP entitlements and admin audit.
- Web-based admin dashboard with role-protected user management, manual VIP grants, account blocking and draft server entries.
- Free and proposed $3/month VIP plan metadata. Both are **ad-free for now**; advertising is postponed at the owner's request.
- Device-side encrypted session persistence, sign-out and a server catalog that does not claim unavailable tunnels are usable.
- Proposed VIP price is **US$3 per month**. This is not a live price or purchase offer: Google Play will supply the actual localized price after its subscription product is configured.
- API tests (`cd backend && npm test`).
- GitHub Actions checks for Cloudflare API syntax/tests and a preview Android APK build.
- New Android package `com.soverixnet.soveravpn`, targeting Android 16 / API 36.

## Repository structure

| Path | Purpose |
| --- | --- |
| `android/` | Android Studio Java application, visual shell and API client |
| `backend/src/` | Cloudflare Worker API + admin page |
| `backend/migrations/` | Cloudflare D1 database migrations |
| `backend/cloudflare.config.ts` | `cf` CLI project config (Cloudflare Workers + D1) |
| `docs/ARCHITECTURE.md` | End-to-end architecture and security boundaries |
| `docs/RELEASE_CHECKLIST.md` | Honest checklist for Play release readiness |
| `docs/SERVER_HANDOFF.md` | Server access verification and owner handoff checklist |

## Build / test

### API

```bash
cd backend
npm install
npm test
```

The official `cf` CLI configuration is in `backend/cloudflare.config.ts`. The `sovera-vpn-staging` D1 database and `sovera-vpn-control-staging` Worker were created in the owner's Cloudflare account on November 9, 2026. Its public Worker subdomain remains **disabled** until the security, abuse protections and real VPN endpoint are ready. Do not put tokens, API keys or VPN private keys into source code.

### Android

Open `android/` in Android Studio (JDK 17, Android SDK 36, Gradle 8.13). A Gradle wrapper **property file** is included, but the wrapper binary is not generated in this environment. Generate the wrapper with a trusted local Gradle installation if needed. Use `SOVERA_API_BASE_URL` as a Gradle project property to set an HTTPS Worker URL. The application does not connect to a VPN in this preview.

The GitHub Actions build can compile the debug preview and attach it as an artifact after the workflow passes; it does **not** create a working VPN or a Play Store-ready signed AAB.

### Advertising decision (November 9, 2026)

Do not show advertisements or bundle an advertising SDK in the initial release. The Free tier and VIP tier both return `ads: false` from `/v1/plans`; the Android app has no ad placement. Advertising is a future opt-in product change requiring fresh privacy and Play policy review, consent handling where required, SDK configuration and tests. It is **not** enabled automatically.

## Security and limitations

- All new accounts are standard users. Never grant admin access from a public signup endpoint.
- To create the first admin, register an account, then deliberately promote that exact user via a trusted, authenticated D1 administrative operation. See `docs/RELEASE_CHECKLIST.md`.
- A manually granted VIP entitlement is separate from verified Google Play purchases. **Never activate paid purchases until server-side verification, cancellation, renewal and refund handling are complete.**
- The existing APK was found to have four corrupt ZIP entries and must not be reused as a signing or code source.
- The project does not reproduce mobile-operator billing loopholes or promise free unrestricted data. Every user must have an authorized underlying data connection.

Reference documentation: [Android VPN](https://developer.android.com/develop/connectivity/vpn), [Google Play VpnService policy](https://support.google.com/googleplay/android-developer/answer/12564964?hl=en), [Cloudflare D1](https://developers.cloudflare.com/d1/worker-api/), [Google Play Billing](https://developer.android.com/google/play/billing/integrate).

