# Taxoryn Desktop — Phase 1 patch

This patch creates a Windows desktop shell for the existing live Taxoryn application.
It intentionally does not add offline storage or change the backend.

## Why this Phase 1 approach

The existing frontend uses React Router `BrowserRouter`, relative `/api` requests, and an in-memory access token with an HttpOnly refresh cookie. Loading the official HTTPS app inside Electron preserves the existing origin, routing, API proxy, and cookie behavior with the least production risk. The desktop app opens directly to `https://app.taxoryn.com/login`; users do not need to type the URL.

This is online-only. Offline mode is explicitly deferred to Phase 2.

## Apply

1. Back up the current repository and copy `electron/main.cjs` into `frontend/electron/main.cjs`.
2. Merge the supplied `package.json` changes into `frontend/package.json` (or replace it if it is unchanged from the uploaded repository).
3. From the `frontend` directory, run:
   ```powershell
   npm install
   ```
   This updates `package-lock.json` with Electron and electron-builder.
4. Run the desktop shell:
   ```powershell
   npm run desktop:dev
   ```
5. Build the Windows installer on Windows:
   ```powershell
   npm run desktop:dist
   ```
   Output will be under `frontend/release/`.

## Security notes

- Electron Node integration is disabled; context isolation and sandboxing are enabled.
- Only `https://app.taxoryn.com` is allowed inside the app. Other HTTPS links open in the default browser.
- Do not add secrets or backend credentials to the desktop package.
- Test sign-in, refresh-cookie behavior, logout, file uploads/downloads, pop-up links, and external identity providers before distribution.
- For production release, configure code signing and a tested update mechanism. This starter patch does not configure automatic updates.
- The EXE target is NSIS. MSI packaging can be added after the first installer is validated, using a Windows MSI/WiX packaging pipeline.
