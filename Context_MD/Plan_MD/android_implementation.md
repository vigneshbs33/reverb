# Reverb: Android Implementation Detail

## 1. The "System Assistant" Strategy (Android)
On Android, Reverb lives as a **Foreground System Service**. It monitors typing and text fields across the entire operating system, enabling a seamless "Polish" in any app.

---

## 2. Technical Pillars

### A. The "Reader & Writer" (Accessibility Service)
The **Accessibility API** is the primary engine for Reverb on Android. 
*   **Reading Text:** Reverb listens for `TYPE_VIEW_FOCUSED` and `TYPE_WINDOW_CONTENT_CHANGED` events. 
*   **Capturing Input:** It identifies the active `EditText` node and reads its content using `AccessibilityNodeInfo.getText()`.
*   **Injecting Result:** After AI processing, Reverb uses the `ACTION_SET_TEXT` command to instantly replace the text in the active text field. 
*   **Configuration:** The service is configured with `canRetrieveWindowContent="true"` and `accessibilityFlags="flagDefault"`.

### B. The "Reverb Bubble" (UI Overlay)
To minimize screen real-estate, Reverb uses a **Floating Bubble Overlay**.
*   **The UI:** A tiny, semi-transparent circle (Messenger Chat Head style) that snaps to the screen edges.
*   **The Menu:** Tapping the bubble expands a compact 2x2 grid menu containing tone icons (👔, 👋, 🔥, ✅).
*   **Compact Undo:** A space-saving, icon-only Undo button sits in the menu to quickly revert text replacements.
*   **Technology:** Implemented using the `WindowManager` API with Jetpack Compose inside `TYPE_APPLICATION_OVERLAY`.

### C. The "Smart Context" (Clipboard Monitor)
*   Reverb registers a clipboard listener through a detached `GhostClipboardActivity`. This prevents the main app from accidentally popping to the foreground.
*   When text is copied, Reverb shows a small "Reply" toast.
*   Tapping the toast opens an overlay with 3 context-aware reply suggestions.

---

## 3. Implementation Stack
*   **Language:** Kotlin (Native).
*   **UI Framework:** Jetpack Compose (for the bubble and radial menu animations).
*   **Concurrency:** Kotlin Coroutines for non-blocking API calls.
*   **Networking:** OkHttp with Retrofit for OpenAI API calls.
*   **Security:** Android Keystore to encrypt the user's API key.

---

## 4. Platform Challenges & Solutions
*   **Battery Management:** To prevent Android from killing the background service, Reverb uses a **Foreground Service** with a silent, persistent notification.
*   **Privacy Mode:** Users can toggle "Incognito Mode" from the notification shade to pause the Accessibility listener.
*   **Exclusion List:** Reverb automatically pauses in apps that use `FLAG_SECURE` (like banking apps and password managers) to protect user data.
*   **One-Handed Operation:** The bubble and radial menu are designed to be reachable with one thumb on any screen size.
