# Tashan: Real Browser OAuth, Live Pixabay Media & Effects, and Advanced System Settings

This plan replaces all in-app social login form dialogs with genuine browser-based Firebase OAuth redirects for Apple, Facebook, and X (Twitter), integrates live Pixabay video effects/overlays and music/sound libraries into the camera and editor, and upgrades the Settings screen into a comprehensive system control center.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed User Preferences from Phase 1:**
> - **Confirmed Decision 1 (Real Browser OAuth & Provider Diagnostics):** Completely remove the custom in-app name/email dialog (`SocialLoginDialog`). Apple (`apple.com`), Facebook (`facebook.com`), and X/Twitter (`twitter.com`) buttons will launch the real system browser / Custom Tab via `FirebaseAuth.startActivityForSignInWithProvider()`. Users will only be logged into Tashan after completing authorization in the external browser and returning with a valid Firebase OAuth token. If a provider is not yet configured in the Firebase Console, a diagnostic sheet will explain the exact callback URL and console setup steps without ever allowing a fake or unverified login bypass.
> - **Confirmed Decision 2 (Live Pixabay Effects, Overlays & Audio):** Fetch live visual video effects, motion overlays, trending music beats, and sound/voice effects through Pixabay (`PixabayClient`) with real-time search, category filtering, and audio preview inside the Camera and Video Editor.
> - **Confirmed Decision 3 (Advanced System Settings):** Expand the Settings & Privacy screen with three advanced control modules:
>   1. **Security & OAuth Session Management** (active OAuth provider bindings, session revocation, 2FA/OTP verification status, biometric/PIN lock).
>   2. **Video Quality, Bitrate, CDN & Cache Engine** (adaptive bitrate ceiling, 60fps hardware acceleration toggle, multi-region edge CDN routing, granular media/effect cache purge).
>   3. **Privacy, Creator Studio & Notification Controls** (granular audience permissions, live stream verification gates, push/in-app notification matrix).

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **True Browser OAuth Authentication**: Tapping **Continue with Apple**, **Continue with Facebook**, or **Continue with X** immediately launches the official provider login page in the system browser via Firebase Authentication (`OAuthProvider`). No local name or email input dialog ever appears. Access to the app is granted strictly when Firebase returns a verified `AuthResult` from the browser redirect.
  2. **Pixabay-Powered Effects & Sound Library**: The camera and video editor pull live visual effects (neon particles, light leaks, bokeh, glitch motion overlays) and categorized audio tracks/sound effects from Pixabay, complete with instant search and real audio playback.
  3. **Advanced System Settings**: Provides power-user and creator-grade controls over account security, OAuth sessions, streaming/playback pipelines, and privacy rules.
- **Target Audience / Persona**: Short-form video creators and viewers who expect authentic, non-bypassable social authentication, rich real-time creative assets, and deep control over app performance and security.
- **Key Value**: Eliminates mock login shortcuts in favor of real OAuth security while giving creators a dynamic cloud library of visual effects and music tracks.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Browser OAuth Flow (Apple, Facebook, X)**:
     - User taps **Apple**, **Facebook**, or **X (Twitter)** on the login sheet.
     - The app checks for any pending OAuth result (`firebaseAuth.pendingAuthResult`) or launches `firebaseAuth.startActivityForSignInWithProvider(activity, provider)`.
     - The Android Custom Tab / system browser opens the official Apple ID, Facebook, or X authorization page.
     - Upon user approval in the browser, control redirects back to the app; `finalizeSignIn()` syncs the authenticated `FirebaseUser` profile to Firestore and logs the user in.
     - If the user cancels in the browser or if the provider is not yet enabled in Firebase Console, the user remains logged out and sees a clear error banner (with an optional "View Firebase Provider Setup Guide" action if Firebase reports an unconfigured provider).
  2. **Pixabay Visual Effects & Music Picker Flow**:
     - In the Camera or Editor, tapping **Effects / Pixabay FX** opens the live Pixabay Visual Effects & Motion Overlays sheet with search (`neon`, `glitch`, `fire`, `bokeh`, `particles`, `cinematic`) and real video/image overlay previews.
     - Tapping **Add Sound / Pixabay Music** opens the upgraded Sound & Audio FX selector featuring live Pixabay search, category chips (*Trending Beats*, *Desi Vibes*, *Cinematic*, *Sound FX*, *Voice Clips*), waveform preview, and one-tap track attachment.
  3. **Advanced System Settings Flow**:
     - Navigating to **Settings & Privacy** reveals an organized, telemetry-rich control center with interactive toggles, sliders, live diagnostics, and deep sub-panels for Security/OAuth Sessions, Video/CDN Engine, and Creator Privacy.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-contrast **Luxury Cyber-Dark** studio aesthetic tailored for short-form video creators.
  - *Color Palette & Mood*:
    - `--background`: Deep Obsidian (`#0A0A0F`) and Elevated Slate (`#14141F`)
    - `--primary`: Tashan Electric Teal (`#00F2FE`)
    - `--accent`: Vibrant Coral Pink (`#FE2C55`) and Gold Verification (`#FFD700`)
    - `--text-primary`: Crisp White (`#FFFFFF`), `--text-secondary`: Muted Silver (`#9E9EAE`)
  - *Typography & Hierarchy*: Bold geometric display headers (`FontWeight.Black`, tight letter spacing) paired with clean, scannable body labels and monospace telemetry badges for bitrates, latency, and session IDs.
  - *Component Styling & Layout*: Material 3 dark surfaces with subtle 1.dp gradient borders, pill-shaped category filters, and full edge-to-edge WindowInsets compliance.

- **Interactive Feedback & Motion**:
  - Browser handoff state indicator while awaiting the OAuth redirect callback.
  - Live loading skeletons and animated audio equalizer bars when previewing tracks and Pixabay effects.
  - Instant haptic/toast confirmation when purging caches, revoking sessions, or switching CDN edge nodes.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real Firebase `OAuthProvider` Browser Redirects vs. In-App Dialogs**
  - *Chosen Approach*: Use `OAuthProvider.newBuilder(providerId)` (`apple.com`, `facebook.com`, `twitter.com`) with `FirebaseAuth.startActivityForSignInWithProvider(activity, provider)` and `pendingAuthResult`. Remove `SocialLoginDialog` completely.
  - *Why*: The user explicitly requested that clicking Apple, Facebook, or Twitter must open the real browser login flow without any internal name/ID input screen, and must only grant access if the browser authentication completes.
  - *Alternatives Considered*: Native third-party Facebook/Twitter SDKs require hardcoded vendor app IDs in `AndroidManifest.xml`, whereas Firebase's unified `OAuthProvider` handles Apple, Facebook, and X cleanly through the system browser using the project's Firebase configuration.

- **Decision 2: Live Pixabay API Integration for Both Visual Effects and Audio Library**
  - *Chosen Approach*: Extend `PixabayClient` and `DynamicFilterManager` to fetch live video effects/overlays (`film` & `animation` types with queries like `particles`, `light leaks`, `neon`, `glitch`) from the Pixabay API, and integrate a Pixabay-tagged Music & Sound FX repository with search and ExoPlayer/MediaPlayer streaming.
  - *Why*: Ensures effects and music tracks are dynamically searchable and backed by real network requests and streamable media URLs.

- **Decision 3: Persistent Advanced System Settings**
  - *Chosen Approach*: Persist all new advanced settings (bitrate ceiling, hardware decoder mode, CDN node, OAuth session security, privacy matrix) in `SharedPreferences` and synchronize with `VideoRepository` state flows.
  - *Why*: Guarantees that changes made in the Advanced Settings screen immediately impact video playback, camera defaults, and security enforcement across the app.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER (Compose)                            │
├────────────────────────┬───────────────────────────┬───────────────────────┤
│      AuthScreen        │    CameraUploadScreen     │ SettingsAndPrivacy    │
│  ┌──────────────────┐  │  ┌─────────────────────┐  │ ┌───────────────────┐ │
│  │ Google (CredMgr) │  │  │ Pixabay FX Overlay  │  │ │ Security & OAuth  │ │
│  │ Apple (OAuthWeb) │  │  │ & Filter Selector   │  │ │ Video/CDN/Bitrate │ │
│  │ FB (OAuthWeb)    │  │  ├─────────────────────┤  │ │ Privacy & Creator │ │
│  │ X (OAuthWeb)     │  │  │ Pixabay Music &     │  │ │ Diagnostics Hub   │ │
│  │ Phone SMS (OTP)  │  │  │ Sound FX Picker     │  │ └─────────┬─────────┘ │
│  └────────┬─────────┘  │  └──────────┬──────────┘  │           │           │
└───────────┼────────────┴─────────────┼─────────────┴───────────┼───────────┘
            │                          │                         │
            ▼                          ▼                         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        DOMAIN & REPOSITORY LAYER                           │
├────────────────────────┬───────────────────────────┬───────────────────────┤
│      AuthManager       │     PixabayClient &       │    VideoRepository    │
│  - startOAuthBrowser() │   DynamicFilterManager    │  - finalizeSignIn()   │
│  - checkPendingOAuth() │  - searchEffectOverlays() │  - advancedSettings   │
│  - sendPhoneOtp()      │  - searchMusicAndSfx()    │  - revokeSessions()   │
│  - verifyPhoneOtp()    │  - applyLiveOverlay()     │  - purgeMediaCache()  │
└───────────┬────────────┴─────────────┬─────────────┴───────────┬───────────┘
            │                          │                         │
            ▼                          ▼                         ▼
┌────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────┐
│   Firebase Auth SDK    │ │   Pixabay REST API      │ │ Firebase Firestore  │
│ (Browser Custom Tabs)  │ │ (Videos / FX / Audio)   │ │ & SharedPreferences │
└────────────────────────┘ └─────────────────────────┘ └─────────────────────┘
```

### Data Model & State
- **OAuth Provider Mapping**:
  - `Apple` -> `"apple.com"` (scopes: `"email"`, `"name"`)
  - `Facebook` -> `"facebook.com"` (scopes: `"email"`, `"public_profile"`)
  - `Twitter / X` -> `"twitter.com"`
- **Pixabay Effect & Audio Models**:
  - `PixabayEffectItem`: Maps `PixabayVideoHit` (`id`, `tags`, `streamUrl`, `thumbnailUrl`, `blendMode`, `category`) so users can layer live Pixabay video effects/animations over their camera preview and video posts.
  - `PixabayAudioTrack`: Represents searchable music tracks and sound effects (`id`, `title`, `artist`, `category`, `durationSec`, `streamUrl`, `tags`, `pixabayId`) integrated into `TikTokSoundSelectorDialog`.
- **Advanced System Settings Model (`SnackVideoSettings`)**:
  - Expanded with `maxBitrateMbps`, `hardwareAcceleration60Fps`, `preferredVideoCodec` (`H.264`, `HEVC/H.265`, `AV1`), `dnsOverHttpsEnabled`, `oauthStrictSessionBinding`, `biometricAppLock`, `creatorWatermarkStyle`, and `liveStreamLatencyMode`.

### Interactive Component & State Mapping
1. **Browser OAuth Launch & Callback (`AuthManager.signInWithOAuthProvider` & `AuthScreen`)**:
   - Clicking Apple, Facebook, or X invokes `authManager.signInWithOAuthProvider(activity, providerKey)`.
   - First inspects `firebaseAuth.pendingAuthResult`; if absent, invokes `firebaseAuth.startActivityForSignInWithProvider(activity, oAuthProvider)`.
   - While the browser Custom Tab is active, `isAuthenticatingOAuth` displays a non-bypassable browser handoff state.
   - On success, `repository.finalizeSignIn(authResult.user, providerKey)` writes the verified user to Firestore and closes the auth sheet.
   - On failure or cancellation, no login occurs; an actionable error message is shown.
2. **Pixabay Effects & Music Browser (`CameraUploadScreen`)**:
   - Typing a search query or tapping a category chip triggers `PixabayClient.getInstance().searchVideos(query, category)` in a coroutine, updating the live grid of visual effects and overlays.
   - Selecting a Pixabay visual effect applies its visual tint/overlay and attaches the effect metadata to the recorded video.
   - Selecting a track in the Pixabay Music & Sound FX dialog previews the audio via `AudioPlayerManager` and binds the track to the camera/editor session.
3. **Advanced Settings Controls (`SettingsAndPrivacyScreen`)**:
   - Interactive sliders, codec selectors, CDN ping testers, OAuth session revocation buttons, and cache purgers immediately mutate `repository.snackSettings` and persist to disk.
