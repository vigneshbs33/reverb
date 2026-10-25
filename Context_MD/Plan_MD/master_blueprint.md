# Reverb: The Master Blueprint (General Architecture)

## 1. The "Ghost Layer" Philosophy
Reverb is designed as a **Ghost Layer** that sits between the Operating System and your active Application. It doesn't live *inside* an app; it watches from above. This allows it to work in Slack, Discord, Chrome, Word, and even your terminal without needing individual plugins for each.

---

## 2. Core Operational Loop (The Engine)

Reverb operates on a 4-step cyclical engine triggered by the user.

### Step 1: The Capture (Input)
*   **Trigger:** The user hits a global system-wide shortcut (Desktop: `Alt + Space`, Mobile: Bubble Tap).
*   **Action:** Reverb instantly "grabs" the text from the current focus point. 
    *   *Mechanism (Desktop):* It uses a "Selection Hack." It sends a simulated `Ctrl + A` (Select All) followed by a `Ctrl + C` (Copy) to the system clipboard in milliseconds.
    *   *Mechanism (Mobile):* It uses the `Accessibility API` to read the current `EditText` node.
    *   *Optimization:* It temporarily stores your *original* clipboard content so it can restore it later.

### Step 2: The Contextual Brain (Processing)
*   **Action:** The captured text is sent to the local Reverb Engine.
*   **Selection:** A sleek, transparent overlay (The Pulse Bar) appears near the cursor, allowing the user to pick a **Tone** (👔, 👋, 🔥).
*   **AI Call:** Reverb sends the raw text + Tone instructions + User API Key directly to the OpenAI API.
    *   *Prompt Engine:* Reverb wraps your text in a "Persona Prompt" (e.g., "Rewrite this to be professional...").

### Step 3: The Magic Replacement (Output)
*   **Action:** Once the AI responds (usually < 500ms), Reverb performs the "Swap."
*   **Mechanism (Desktop):* It clears the current text in the app and sends a simulated `Ctrl + V` (Paste).
*   **Mechanism (Mobile):* It uses `ACTION_SET_TEXT` via Accessibility Services.
*   **The Shimmer:** A visual horizontal light sweep animation plays over the text area to mask the replacement.

### Step 4: The Restore & Undo
*   **Restore Clipboard:** Reverb restores the user's original clipboard content so they don't lose whatever they had copied before the "Reverb" event.
*   **Undo Mechanism:** The original pre-polished text is cached in the foreground service. The user can tap a compact `Undo` icon in the overlay menu to instantly revert the replacement.

---

## 3. Advanced Systems

### The Reverb Pulse (Presence)
*   Reverb maintains a tiny, low-resource background process.
*   **Desktop:** It knows where your cursor is. When you stop typing for > 2 seconds, the "Pulse" (a tiny glowing dot) appears near your cursor to signal readiness.
*   **Mobile:** The floating bubble subtly glows after a sentence is completed.

### Smart Context (Intelligence)
*   Reverb monitors the clipboard for "Incoming Context" (e.g., a message you just received).
*   If you copy text from another person, Reverb's "Reply" engine activates, offering 3 suggested replies (Accept, Decline, Clarify) in your chosen tone.

---

## 4. Key Systems & Modules

*   **Global Hook Manager:** High-priority keyboard listener (`Alt + Space`).
*   **Overlay UI (The "Ghost" UI):** A `WS_EX_NOACTIVATE` window that stays on top but doesn't steal focus from your typing.
*   **Secure Vault:** Local-only, OS-level encrypted storage (Keychain/Keystore) for the user's API keys.
*   **Prompt Factory:** A collection of optimized system prompts for each Tone Personality.

---

## 5. Security & Privacy Model
*   **Zero-Cloud Processing:** No Reverb servers. Your text goes directly from your app to OpenAI.
*   **Local Processing:** All capturing and UI rendering happen locally.
*   **API Sovereignty:** The user owns the API key and can monitor their own usage and costs.
