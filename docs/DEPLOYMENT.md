# Deployment disclosure notes

## Operator-managed public edge

Patrick confirms that SceneProof is publicly deployed. The Caddy edge configuration
is intentionally outside this repository on the VPS at `~/caddy-edge`. It proxies
`/api/*` to the API and all other paths to the SceneProof web container. The web
container's internal static server provides SPA fallback, according to Patrick.

The external reverse proxy and running web-container configuration were **not
inspected from this repository**. This task neither adds nor changes Caddy,
hosting, TLS, access controls or deployment configuration. The repo's historical
local-only docs do not describe that operator-managed deployment.

## Legal routes and build

Privacy Policy: `/privacy`. Terms of Use: `/terms`. Both are React application
routes served by the same built HTML/JS as the library. Their links remain in the
global footer. Direct navigation and refresh require the existing SPA fallback
to serve `index.html`, while `/api/*` remains API traffic.

Build with `npm.cmd run build`. The focused production-bundle check is:

```powershell
npx.cmd playwright test --config playwright.legal.config.ts
```

This serves `apps/web/dist` with local Vite preview on loopback port 5187 and tests
direct navigation/reload at desktop/mobile sizes. It starts no backend and blocks
API/external requests in the legal-page browser tests. It proves repository build
and frontend fallback behavior, not the VPS edge configuration. After deployment,
the operator must verify `/privacy` and `/terms` directly and after browser reload
on the actual host, plus that `/api/*` still returns API responses.

## Required disclosure inputs

Set only confirmed public values from `apps/web/.env.example` in the **frontend
build environment**, then rebuild. A root Compose `.env` is not automatically
the web workspace's Vite environment. Never put API keys or private credentials in
`VITE_*`. Missing fields display explicit TODOs.

The proposed privacy mailbox is not confirmed operational and is intentionally
absent from source/defaults. Confirm delivery, monitoring and responsibility before
setting `VITE_PRIVACY_CONTACT_EMAIL`. SceneProof does not operate an SMTP server;
mail infrastructure is external and was not inspected. A `mailto:` link only opens
the visitor's mail client; these pages do not collect/send a contact form.

OpenAI is the verified AI recipient. Exact hosting recipients/locations, OpenAI
account retention/residency, transfer arrangements and infrastructure logs/backups
need operator confirmation. The application retains retired demo copies after
reset; reset is not deletion. Public project isolation is not provided by PR10.
See [disclosure review](PRIVACY-DISCLOSURE-REVIEW.md) for evidence, TODOs and launch gates.
