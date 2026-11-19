# MZ Botanicals web storefront

This module is the browser storefront for the existing MZ Botanicals Firebase
project. The repository-root Dockerfile builds `webStorefront/`; it does not try
to run the unfinished Kotlin `webApp` or the empty Ktor server module.

The public container contains only the generated storefront and a small static
HTTP server. The Docker context excludes unrelated application files, local
configuration, private documents, functions, credentials and build artifacts.
The final image runs as the unprivileged Node user and listens on `0.0.0.0` using
Cloud Run's `PORT`. `/healthz` identifies the real storefront.

## Local build

Use Node 24.12 or later:

```sh
cd webStorefront
npm ci --ignore-scripts
npm test
npm run build
npm start
```

Open `http://localhost:8080`. Firebase's existing authorized domains include
localhost. The browser uses the existing web app's public Firebase client
configuration. This configuration identifies the app; it is not a privileged
server credential. No Gmail app password, service-account key, payment key or
Firebase Admin SDK is used in this container.

The November 19, 2026 dependency audit reports two server-side advisories in
Firebase's Node-only `@grpc/grpc-js` dependency. Its patched versions do not fit
Firestore's declared `~1.9.0` range. The reviewed browser bundle has no gRPC or
Node Firestore inputs, and the final container has no `node_modules`; the
affected server features are absent from this release. Recheck upstream fixes
before reusing these dependencies in a Node backend. Do not force an incompatible
override or downgrade Firebase to silence the audit.

## Supported customer flows

- Published catalog browsing, search/filtering, product details and pack selection.
- Google sign-in and existing-profile preservation.
- Shared saved cart with transactional quantity, pack-size and stock checks.
- Customer-owned order history, including explicit legacy/unverified records.
- Customer profile updates and immutable account-deletion requests.

Checkout is intentionally paused, matching the Android/desktop release
candidate and the deployed server switches. There are no browser-created
orders, payment claims, refunds or invented tax/shipping charges. Administrative
catalog and fulfillment workflows remain in the existing company apps.

The browser SDK relies on the existing deployed Firestore rules. Signing out or
changing accounts clears private subscriptions and visible state. Client code
does not grant itself admin rights. This container deployment does not deploy
or replace Firestore/Storage rules or the order/email workers.

## Existing Git deployment

- Repository: private `MajiinZ/MzbotanicalV1`, branch `main`.
- Build trigger: `d7e8a505-df8b-42a2-99fb-07beb07869d2`, `europe-west1`.
- Service: `mzbotanicalv1-git`, `us-west2`, project `killrsgenetics-461300`.
- The trigger already builds from the root with `-f Dockerfile`.

Before this fix, Docker failed with `lstat /workspace/Dockerfile: no such file or
directory`; the service consequently kept Google's placeholder image. Add the
root Dockerfile and storefront together, then verify a successful build, real
image digest/revision, health endpoint and browser rendering.

For public storefront hosting, only this service needs public invocation. Add
only its two observed hostnames to Firebase Authentication's authorized domains:

- `mzbotanicalv1-git-72yste2ewa-wl.a.run.app`
- `mzbotanicalv1-git-623171350158.us-west2.run.app`

Preserve existing authorized domains and IAM bindings. Public static hosting
does not grant public access to customer profiles, carts, orders or deletion
requests. Do not expose the private backend workers or change their provider
switches as part of a storefront deployment.

## Google sign-in verification

The Firebase Google provider must reference an active Google OAuth web client
with its matching secret. A deleted client can produce `401: deleted_client`
even when the container is healthy. Keep the secret in the provider's private
configuration; never add it to the browser config or repository.

The web client needs the Firebase redirect URI
`https://killrsgenetics-461300.firebaseapp.com/__/auth/handler`. Preserve the
existing desktop callback `http://localhost:8080/callback`. Authorized JavaScript
origins contain origins only (scheme, host and optional port), one per row:
the Firebase auth origin, the two Cloud Run HTTPS origins above, and
`http://localhost:8098` while that local preview is in use. Verify a real user
sign-in and profile load after saving; the health endpoint cannot test OAuth.

The similarly named service in `us-west1` failed regional quota initialization.
It is separate from this deployment target and is not removed by this change.

Keep the previous revision available for a traffic rollback. Inspect Cloud Build
errors before changing infrastructure or credentials; a successful placeholder
startup is not proof that the application was deployed.
