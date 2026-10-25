# Contributing to Reverb

Thanks for taking a look. Bug reports, ideas and pull requests are all welcome.

## Reporting a bug

Open an issue with the bug template and include:

- Phone model and Android version
- The app you were typing in (for example WhatsApp or Gmail)
- What you tapped, what you expected and what happened
- The message shown on the bubble, if any

Please never paste your OpenAI API key or private messages into an issue.

## Making a change

1. Fork the repo and create a branch from `main`.
2. Build with `./gradlew :apps:android:assembleDebug` (see the README for setup).
3. Run `./gradlew test` and `ktlint --relative "**/*.kt" "**/*.kts"`. CI runs both on every pull request.
4. Add or update unit tests. Editing logic belongs in `core/domain`, where it can be tested with the fakes in `core/domain/src/test`.
5. Test on a real device: enable the Accessibility Service, then try Polish, Smart Reply and Undo in at least one chat app and one email app.
6. Keep changes focused. One feature or fix per pull request.
7. Open a pull request that explains what changed and how you tested it.

## Code style

- Kotlin official style, checked by ktlint 1.3.1 with the rules in `.editorconfig`.
- Keep Android code out of `core/domain`, `core/model`, `core/prompt` and `core/network`. New engines, fields and stores implement the interfaces in `core/domain`.
- UI in Jetpack Compose.
- Keep the privacy rules intact: read text only on a user tap, never read password fields, never log user text, never send text anywhere except the model API the user configured.
- Never commit API keys, keystores or `local.properties`.
