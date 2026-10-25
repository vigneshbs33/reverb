# Reverb: Windows Implementation Detail

## 1. The "Ghost Overlay" Strategy (Windows)
Reverb operates as a **Global Hook Layer** on Windows. It monitors keyboard input and text buffers without taking focus from the user's active application.

---

## 2. Technical Pillars

### A. The Global Hook (Key Listener)
Using the `RegisterHotKey` Windows API, Reverb listens for the global shortcut: **`Alt + Space`**. 
*   This trigger works regardless of which app is in focus (Slack, Discord, Outlook, Word).

### B. The "Selection Hack" (Input Simulation)
To capture and replace text in external apps, Reverb uses the **Selection Hack** protocol:
1.  **Capture:** Reverb sends a simulated `Ctrl + A` (Select All) followed by a `Ctrl + C` (Copy) to the system clipboard.
2.  **Buffer:** It stores the captured text and the *previous* clipboard content in memory.
3.  **Replace:** After polishing, it injects the new text by sending a simulated `Ctrl + V` (Paste).
4.  **Restore:** It restores the original clipboard content, making the entire process invisible.

### C. The "Pulse Bar" (Ghost UI)
The UI is a **Non-Activating Window** (`WS_EX_NOACTIVATE`) that stays on top but never steals keyboard focus.
*   **The UI:** A sleek, glassmorphic horizontal bar appears near the bottom-center of the screen.
*   **Controls:** Users can toggle tones (👔, 👋, 🔥) using arrow keys or the mouse.
*   **Visuals:** Uses the Windows **Acrylic/Mica** effect (via `win32` APIs in Rust/Tauri).

### D. Smart Context (Clipboard Hook)
Using `AddClipboardFormatListener`, Reverb detects when the user copies text from another source (e.g., an incoming email). 
*   **Trigger:** If >10 words are copied, a small "Reverb Reply" dot appears near the cursor.
*   **Action:** Clicking it generates 3 contextual reply options ready to be pasted.

---

## 3. Implementation Stack
*   **Core:** Rust (Tauri v2).
*   **APIs:** `windows` crate for direct Win32 API access (`SendInput`, `GetForegroundWindow`).
*   **Security:** `keyring-rs` for storing OpenAI API keys in the Windows Credential Manager.

---

## 4. Performance & Compatibility
*   **Low Footprint:** Target memory usage < 50MB.
*   **Fallback Mode:** If an app blocks the "Selection Hack" (like some Terminals), Reverb switches to **Accessibility UI Automation** (UIA) to read and set text directly.
*   **Tray Integration:** A minimalist system tray icon for settings and "Pause" mode.
