# Reverb: Unified UX & Flow Strategy

## 1. The Core Experience (The "Shimmer")
Regardless of the device, the soul of Reverb is the **"Shimmer."** This is the visual feedback that tells the user, "Your mess has been polished."

*   **Visual Design:** A horizontal, semi-transparent white-to-transparent light sweep that moves across the text box from left to right as the text is replaced.
*   **Purpose:** It masks the split-second "jump" between the old messy text and the new polished text, making it feel magical rather than technical.

---

## 2. The "Pulse" (Anticipatory UX)
Reverb should feel like it's "breathing" with your typing. 

### Implementation:
*   **The Timer:** A 2-second "idle" timer starts every time a key is pressed.
*   **The Appearance:** If the user stops typing for 2 seconds, a tiny, soft glowing dot (The Pulse) appears near the cursor. 
*   **The Function:** This is a subtle hint: "I'm ready to polish this if you are."

---

## 3. Unified Interaction Design

### Tones as Emotional States
Reverb uses four core tones, each with a distinct visual and linguistic personality, elegantly arranged in a compact 2x2 grid overlay layout along with a space-saving icon-only Undo button:

| Tone | Icon | Visual Accent | Linguistic Personality |
| :--- | :--- | :--- | :--- |
| **Professional** | 👔 | Slate Blue / Sharp Edges | Concise, authoritative, formal. No emojis. |
| **Friendly** | 👋 | Soft Peach / Rounded | Warm, empathetic, uses exclamation marks and standard emojis. |
| **Edgy** | 🔥 | Neon Purple / Glitchy | Creative, bold, uses modern slang and expressive emojis. |
| **Fact Check** | ✅ | Clean White / Minimal | Focuses purely on grammatical and factual accuracy without altering voice. |

---

## 4. The "Clipboard Smartness" (Cross-App Intelligence)
Reverb doesn't just work on what you type; it works on what you *read*.

*   **Context Capture:** When the user copies more than 10 words, Reverb internally flags this as "External Context."
*   **The Quick Action:** A small overlay appears (The Reply Bar) with three context-aware buttons:
    *   **"Agree"** (Generates a positive, confirming reply).
    *   **"Decline"** (Generates a polite, firm refusal).
    *   **"Clarify"** (Generates a request for more information).

---

## 5. Privacy & Ethics (The "Trust Buffer")
Since Reverb has high-level permissions (Accessibility on Android, Global Hooks on Windows), the UX must prioritize **Trust**:

1.  **"Incognito" Mode:** A clear, visible toggle to tell Reverb to "Go to Sleep" for the next 15 minutes.
2.  **No Cloud History:** A dashboard that explicitly shows: "Your text was sent directly to OpenAI. No logs were saved on Reverb servers."
3.  **Local API Key:** The first-time user experience (FTUE) will guide the user on how to get their own OpenAI key, explaining *why* it's better for their privacy and wallet.

---

## 6. The Future: "Reverb Sync"
In the future, the **Tone Preferences** and **Common Phrases** (your "Voice") should sync between your Android phone and Windows laptop.
*   *Example:* If you teach Reverb that you prefer "Cheers" as a sign-off on your laptop, it should automatically use "Cheers" when you polish a text on your phone.
