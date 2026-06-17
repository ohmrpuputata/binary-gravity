# Contributing

Thanks for helping improve **Alien Apocalypse**. The project is content-heavy, so the most valuable contributions are the ones that keep progression readable and avoid adding disconnected items.

## Local Setup

Requirements:

- Java 21
- Minecraft/Fabric development through Gradle Loom
- Git

Build:

```powershell
.\gradlew.bat build
```

On Linux/macOS:

```bash
chmod +x ./gradlew
./gradlew build
```

## Content Rules

- Every item, material, mob, block, and structure needs an input, an output, and a purpose.
- Do not add a stronger tool or weapon unless there is a threat, cost, or gate that justifies it.
- Update recipes, loot, models, textures, lang files, and docs together when the feature touches them.
- Keep `en_us.json` and `ru_ru.json` aligned for player-facing text.
- If a feature changes progression, update `docs/DESIGN.md` or `docs/CONTENT_LEDGER.md`.

## Pull Requests

Good pull requests usually include:

- A short description of the player-facing change.
- Screenshots or clips for visual/client changes.
- Notes about balance impact.
- `.\gradlew.bat build` result.
- Any known unfinished work, clearly marked.

## Style

- Prefer small, reviewable changes over sweeping refactors.
- Follow the existing package layout.
- Keep generated/reference art in `art/` or `docs/` unless it is used by the mod.
- Avoid dead-end content: if a material or machine exists, it should matter somewhere.

## Bug Reports

Use the GitHub issue template and include:

- Minecraft version
- Fabric Loader version
- Fabric API version
- Java version
- Mod version or commit
- Reproduction steps
- Latest log or crash report
