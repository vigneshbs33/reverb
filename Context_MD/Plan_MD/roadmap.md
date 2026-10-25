# Reverb: The Development Roadmap

## Phase 1: The "Hacker" POC (Windows)
**Goal:** Prove the "Selection Hack" (Capture & Replace) works in real apps.
- [ ] Create a simple Rust/Tauri app that lives in the system tray.
- [ ] Implement `RegisterHotKey` for `Alt + Space`.
- [ ] Script the `Ctrl+A` -> `Ctrl+C` -> `Ctrl+V` sequence.
- [ ] Add a simple "OpenAI Key" input field.
- [ ] Successfully "Polish" a messy sentence in Slack/Discord.

---

## Phase 2: The "Ghost" Overlay & UI
**Goal:** Make it feel magical, not like a script.
- [ ] Create the **Pulse Bar** (Transparent, non-activating window).
- [ ] Implement the **"Shimmer"** animation (CSS gradient sweep).
- [ ] Add the 3 Tone icons (👔, 👋, 🔥) with arrow-key selection.
- [ ] Implement the "Pulse" dot that appears near the cursor after 2 seconds of idleness.

---

## Phase 3: Android System Service
**Goal:** Bring the "Invisible Assistant" to mobile.
- [ ] Build the **Accessibility Service** in Kotlin.
- [ ] Implement the **Floating Bubble** UI with `WindowManager`.
- [ ] Successfully read and replace text in a 3rd party app (e.g., Instagram).
- [ ] Add the radial menu for Tone selection.

---

## Phase 4: Intelligence & Context
**Goal:** Make the AI smarter and more proactive.
- [ ] Implement **"Smart Context"** (Detecting clipboard changes for incoming messages).
- [ ] Create the "Reply Generation" prompt and UI toast.
- [ ] Add "Tone Customization" so users can tweak their own system prompts.

---

## Phase 5: Polishing & Distribution
**Goal:** Launch-ready product.
- [ ] **Privacy Audit:** Ensure zero logs are kept and keys are encrypted in the OS Keychain/Keystore.
- [ ] **Onboarding Flow:** Implement the "Aha!" sandbox and OpenAI setup guide.
- [ ] **Performance Pass:** Ensure < 50MB RAM usage on Windows and < 20MB on Android.
- [ ] **Packaging:** Create installers (.msi/.exe) and the Android APK/Bundle.

---

## Phase 6: v3.0.x Polish & Precision (Completed)
**Goal:** Enhance UI real-estate and text accuracy.
- [x] Implement **"Instant Undo"** mechanism caching original text.
- [x] Add **"Fact Check"** tone for factual/grammatical correction without style alteration.
- [x] Redesign floating overlay into a compact 2x2 grid.
- [x] Professionalize the main app OnboardingScreen.
