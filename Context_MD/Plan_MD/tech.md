# Reverb: Technical Stack & Architecture

## 1. Shared Strategy: "Native Core, Shared Logic"
To ensure Reverb is lightning-fast and respects system resources, we will build with a **Native-First** approach.

---

## 2. Desktop Stack (Windows)
*   **Primary Engine:** **Rust with Tauri v2**.
    *   *Why:* Minimal memory footprint ( < 50MB RAM), fast startup, and native system tray / shortcut support.
*   **Windows APIs (Rust `windows` crate):**
    *   `RegisterHotKey`: Global `Alt + Space` listener.
    *   `SendInput`: Simulating `Ctrl + A`, `Ctrl + C`, `Ctrl+V`.
    *   `WS_EX_LAYERED` / `WS_EX_NOACTIVATE`: For the click-through, non-focus-stealing UI.
*   **UI Layer:** React or Svelte with **Glassmorphism/Acrylic** CSS effects for the "Pulse Bar."
*   **Key Storage:** **Windows Credential Manager** (via `keyring-rs`).

---

## 3. Mobile Stack (Android)
*   **Primary Language:** **Kotlin** (Native).
*   **Core APIs:**
    *   `AccessibilityService`: Primary engine for in-app text interaction (`onAccessibilityEvent`).
    *   `WindowManager`: Drawing the **Floating Bubble** using `TYPE_APPLICATION_OVERLAY`.
    *   `ClipboardManager`: Monitoring clipboard changes for **Smart Reply**.
*   **UI Layer:** **Jetpack Compose** for high-performance, smooth animations for the bubble and radial menu.
*   **Key Storage:** **Android Keystore System**.

---

## 4. The Intelligence Layer (AI Logic)
Reverb is a **"Direct-to-API"** tool. 
*   **Model Recommendation:** 
    *   *Speed:* **GPT-4o-mini** (for 90% of tasks).
    *   *Quality:* **GPT-4o** (for complex tone shifts).
*   **Prompt Architecture:**
    *   System prompts are stored locally and injected with context at runtime.
    *   Example Template: `SYSTEM: You are Reverb. Rewrite the following text to be [TONE], correcting all errors. USER: [MESSY_TEXT]`.
*   **Networking:** OkHttp (Android) / Reqwest (Rust) with direct SSL pinning to `api.openai.com`.

---

## 5. Visual Rendering: "The Shimmer"
*   **Windows:** CSS Keyframe animations for a 500ms white-to-transparent linear gradient sweep.
*   **Android:** Compose `InfiniteTransition` with a `Brush.linearGradient` to create the shimmer effect over the `EditText` region.

---

## 6. Implementation Phases (The Roadmap)

### Phase 1: Windows Proof-of-Concept (POC)
*   Implement `Alt + Space` + `Ctrl+A/C/V` logic in a simple Rust tray app.
*   Deliver the first working "Text Polish" in Slack/Discord.

### Phase 2: Android Proof-of-Concept (POC)
*   Build the base Accessibility Service that can read/replace text in a dummy app.

### Phase 3: The "Ghost" UI & Shimmer
*   Refine the overlay design (Desktop) and floating bubble (Mobile) with high-quality animations.

### Phase 4: Tone Customization & Sync
*   Allow users to edit their own System Prompts for each tone and sync preferences via a lightweight, encrypted sync (optional).
