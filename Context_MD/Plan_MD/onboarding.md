# Reverb: Onboarding & First-Time Experience (FTUE)

## 1. The "Aha!" Moment
Reverb is a **"Bring Your Own Brain"** app. The onboarding must be fast, educational, and friction-free to bridge the gap between a first-time user and a "Super-Powered" writer.

---

## 2. The Onboarding Flow (The First 3 Minutes)

### Step 1: The "Invisible Magic" Hook
*   **Action:** When the user first opens Reverb, they see a minimalist, fullscreen overlay. 
*   **Visual:** "You're messy. We're okay with that. Try typing something messy below."
*   **Interaction:** A built-in sandbox text box where the user can type "hey sry late for mtg." 
*   **Trigger:** They hit `Alt + Space` (as instructed). 
*   **The Magic:** It polishes the text *using a demo API key* provided by the app for the first 5 "Reverbes."

### Step 2: The "Brain Setup" (OpenAI Key)
*   **Action:** After the demo, Reverb explains the "Own Your AI" philosophy.
*   **Education:** "Reverb is private. We don't see your text. To keep it that way, you'll use your own OpenAI key."
*   **The Bridge:** Reverb provides a direct, 1-click button to the OpenAI API Key dashboard and a 30-second "How-To" video/GIF.
*   **Validation:** Once pasted, Reverb instantly validates the key with a "Connection Successful!" checkmark.

### Step 3: Permissions (The "Trust Handshake")
*   **Action:** Reverb requests system-level permissions.
*   **Transparency:** 
    *   **Windows:** Explains why it needs "Global Shortcut" access.
    *   **Android:** Explains why it needs "Accessibility" (to read and write text *inside* your apps).
*   **The "Safety Lock":** Reverb highlights its "Exclusion List" (Banking apps, Passwords) to prove it values privacy.

---

## 3. Persistent Settings & Customization

The **Settings Panel** (accessed via the System Tray or App Icon) will contain:

*   **Model Selection:** Toggle between `GPT-4o-mini` (Fast/Cheap) and `GPT-4o` (High-Quality).
*   **Tone Customization:** Ability to "Tweak" the system prompts for the 3 core tones.
*   **Exclusion List:** A simple checklist of apps where Reverb should *never* activate.
*   **Shortcut Settings:** Change `Alt + Space` to any other global key combination.
*   **Privacy Toggle:** A "Global Killswitch" to stop the Accessibility/Listener service completely.

---

## 4. The "Voice" Learning (Advanced)
After the user has "Reverbed" 100 times, the app will show a small notification:
*   "I've noticed you use the word 'Awesome' a lot. Should I keep that in your Professional tone too?"
*   This introduces the **"Voice"** feature, where Reverb learns to match the user's specific vocabulary while still fixing their grammar.
