# Google Play release checklist — SOVERA VPN

| Item | State | Required action |
| --- | --- | --- |
| Original branding | Started | Generated new SOVERA VPN logo; review icon for Play asset dimensions |
| Android project | Source preview | Encrypted session persistence and server catalog prepared; build with Android SDK 36 on JDK 17, run unit/device tests |
| Actual VPN connection | **BLOCKED** | Supply authorized endpoint, protocol configuration and audited client transport |
| Cloudflare control API | Source complete / test preview | Create isolated D1, apply migration, deploy Worker, verify HTTPS and roles |
| User login | API code exists | Add rate limiting, abuse protection, security audit and real end-to-end test |
| Admin panel | Source code exists | Promote one verified admin, run protected live tests |
| Free tier ads | **DEFERRED BY OWNER** | Both tiers are ad-free; do not add AdMob or an ad UI. Reassess disclosure, consent and policy only if ads are requested later |
| Monthly VIP billing | **BLOCKED** | Create product ID `sovera_vpn_monthly` in Play Console, configure verified purchases and server subscription states |
| Billing changes | **BLOCKED** | Handle refund, cancel, grace period, account hold, expiry and Real-time Developer Notifications |
| VPN disclosure | **BLOCKED** | In-app explanation/consent, VPN use in store listing, encryption to endpoint, policy declaration and demonstration video |
| Privacy & data safety | **BLOCKED** | Publish privacy policy, disclose data handling, complete Data safety form |
| Production signing | **BLOCKED** | Secure app signing and upload key; never commit keystore |
| Google Play release | **NOT READY** | AAB generation, internal/closed testing, review credentials, release rollout |

Google Play requirements verified 2026-11-09: new apps and app updates must target API 36 or higher. See [official requirement](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en).

## Cloudflare staging checklist

1. Create isolated D1 `sovera-vpn-staging` in the intended Cloudflare account.
2. Apply `backend/migrations/0001_init.sql` using an authenticated D1 console/CLI.
3. Deploy `backend/` with the current `cf` CLI, confirm binding `DB` and enable logs/traces.
4. Test `/health`, register a test user, then elevate only that user's ID to `admin` via protected D1 administration. Example after checking the exact ID and ownership:

   ```sql
   UPDATE users SET role='admin' WHERE id='REPLACE_WITH_VERIFIED_USER_ID' AND email='REPLACE_WITH_VERIFIED_EMAIL';
   ```

5. Test `/admin` and role restrictions, block/unblock, manual VIP, and audit data.
6. Add Cloudflare rate limiting, monitoring and security controls before admitting production users.
7. Only after the VPN transport is fully authorized and working, enable the Android Connect button and subscription purchase flow.

## User-supplied information still needed

- Proof of control over a VPN endpoint (VPS/host with public IP) and preferred first location; use an approved secret-sharing path for any credentials, never plain chat.
- The Google Play Console app and monthly subscription product ID; never share Play signing private keys.
- No AdMob information is required now; advertising is explicitly out of scope for this release.
- A support email, privacy-policy URL, and required account deletion/retention rules.
- Whether the first users are internal testers or public users.
