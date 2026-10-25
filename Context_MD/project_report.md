# Reverb Project Report

## Project Overview
Reverb is a premium Android accessibility service and floating overlay app that acts as an intelligent writing companion. It utilizes `gpt-4o-mini` to polish, adjust tone, and rewrite text on-device across any app.

## Version History

### [v3.0.3]
- **Focus:** Promotion Tone & Landscape UI Fixes
- **Key Changes:**
  - Added "Promotion" rewriting tone for clear, engaging marketing messages.
  - Fixed an unclosable out-of-screen bug for the floating bubble in landscape mode using vertical scroll physics.
  - Enabled proper ProGuard keep rules to resolve Tink R8 minification errors.
  - Output product-level release build.

### [v3.0.2] - Planning Phase
- **Focus:** Major UI Enhancements
- **Key Changes:**
  - Front overlay streamlined and kept compact.
  - "Undo" button miniaturized to an icon-only representation.
  - Main application UI redesigned for a "simple, professional, and clean" aesthetic.
  - Added ongoing project report documentation.

### [v3.0.1]
- **Focus:** Stability & Play Store Readiness
- **Key Changes:**
  - Implemented Instant Undo mechanism via foreground service cache.
  - Added "Intelligent Fact Check" tone with explanatory reasons.
  - Fixed detached `GhostClipboardActivity` task affinity bugs.
  - Enabled R8/ProGuard minification.

### [v3.0.0] and Earlier
- Initial modular architecture setup, floating bubble UI, and tone personalization through scenario-based queries.
