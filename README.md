<p align="center">
  <img src="docs/assets/repo-banner.svg" alt="Alien Apocalypse banner" width="100%">
</p>

<p align="center">
  <a href="https://github.com/physicaldazezzz/binary-gravity/actions/workflows/build.yml"><img alt="Build" src="https://github.com/physicaldazezzz/binary-gravity/actions/workflows/build.yml/badge.svg"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62b47a?logo=curseforge&logoColor=white">
  <img alt="Fabric" src="https://img.shields.io/badge/Fabric-loader%200.17.2-2b2f77">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-f89820?logo=openjdk&logoColor=white">
  <img alt="Mod version" src="https://img.shields.io/badge/version-1.14.0-58d68d">
  <img alt="License" src="https://img.shields.io/badge/license-CC0--1.0-lightgrey">
</p>

# Alien Apocalypse

`alien-invasion` is a Minecraft Fabric 1.21.1 survival mod about an escalating alien apocalypse: contamination spreads, radiation becomes a logistics problem, swarms learn to pressure the player, and the world fills with crashed ships, hives, vaults, weapons, armor, machines, and a day-8 boss fight.

Repository name note: the GitHub repository is `binary-gravity`, the Gradle project is `alien-invasion`, and the in-game mod name is **Alien Apocalypse**.

## Highlights

- Staged invasion pressure with world events, swarm escalation, infection, radiation, acid rain, and survival pacing.
- Tactical alien AI: flanking, retreating, tunneling, bombing runs, bridge building, squad aggro, scavenging, teleport pressure, and hive behavior.
- Progression loop built around contamination counters: hazmat gear, purifiers, geiger tools, antidotes, cosmic gear, black-market salvage, and endgame crafting.
- World content: alien cities, hives, crashed UFOs, labs, bunkers, vaults, residue veins, and a homeworld dimension.
- Combat toys: blasters, gravity weapons, EMP grenades, turrets, plasma gear, Nibirium tools, palladium/platinum anvils, and boss rewards.
- Client polish: custom renderers, armor models, HUD overlay, particles, ambience, language files, textures, JEI recipe category integration, and custom models.

## Content Snapshot

| Area | Current footprint |
| --- | ---: |
| Recipes | 103 |
| Loot tables | 219 |
| Worldgen JSON files | 33 |
| Advancements | 15 |
| Texture files | 494 |
| Languages | `en_us`, `ru_ru` |
| Target Minecraft | `1.21.1` |
| Java target | `21` |

## Gallery

<table>
  <tr>
    <td width="50%">
      <img src="src/main/resources/assets/alien-invasion/icon.png" alt="Alien Apocalypse icon" width="160">
    </td>
    <td width="50%">
      <img src="art/mod_blocks_redesign_reference.png" alt="Block redesign reference">
    </td>
  </tr>
  <tr>
    <td><strong>Mod icon</strong><br>Small, readable identity for launchers and mod lists.</td>
    <td><strong>Block art direction</strong><br>Contaminated machinery, organic residue, warning lights, and sci-fi survival surfaces.</td>
  </tr>
</table>

## Gameplay Pillars

| Pillar | What it means in game |
| --- | --- |
| Survive the spread | Infection, contaminated ground, toxic water, radiation fields, and acid weather force preparation instead of simple rushing. |
| Earn stronger gear | Materials have upstream sources and downstream uses: ores, hives, boss drops, salvage, machines, and crafting chains. |
| Fight smarter swarms | Enemies are not just stat blocks. They build, flank, leap, flee, steal, tunnel, fly, cast, and coordinate pressure. |
| Push into danger | Better loot is tied to deeper structures, harder encounters, and late-stage invasion milestones. |
| End the invasion | The Swarm Beacon and day-8 boss turn progression into a finishable campaign arc. |

## Requirements

- Minecraft `1.21.1`
- Fabric Loader `0.17.2` or newer
- Fabric API `0.116.8+1.21.1`
- Java `21`
- JEI is optional for recipe browsing during development and compatible playthroughs

## Install

1. Install Minecraft `1.21.1` with Fabric Loader.
2. Install Fabric API for `1.21.1`.
3. Put the built `alien-invasion-*.jar` into your `mods` folder.
4. Launch the game and check the mod list for **Alien Apocalypse**.

This repository does not pretend a release exists before one is published. If there is no GitHub Release yet, build from source.

## Build From Source

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
chmod +x ./gradlew
./gradlew build
```

The remapped mod jar is written to `build/libs/`.

## Repository Map

| Path | Purpose |
| --- | --- |
| `src/main/java/com/example/alieninvasion` | Fabric mod source, AI, blocks, items, worldgen hooks, renderers, and systems. |
| `src/main/resources/assets/alien-invasion` | Textures, models, lang files, particles, and client assets. |
| `src/main/resources/data/alien-invasion` | Recipes, loot tables, advancements, dimensions, worldgen, and tags. |
| `docs/DESIGN.md` | Design rules for progression and content causality. |
| `docs/CONTENT_LEDGER.md` | Content ledger and backlog-style verdicts. |
| `docs/asset_audit.md` | Visual/content audit notes. |
| `tools/` | Maintenance scripts for localization and generated art assets. |

## Development Notes

- Keep new content connected to acquisition, usage, and purpose. A material with no downstream use is debt, not depth.
- Prefer meaningful counters over raw power creep: radiation needs protection, swarms need crowd control, machines need EMP, deep loot needs risk.
- Run `.\gradlew.bat build` before opening a pull request.
- Update `en_us.json` and `ru_ru.json` together when adding player-facing text.
- Keep generated or reference art in `art/` or `docs/`, not mixed into gameplay assets unless it is actually used by the mod.

## GitHub Project Polish

This repository includes:

- GitHub Actions build workflow
- Dependabot configuration
- Bug, feature, and balance issue forms
- Pull request checklist
- Contribution and security notes
- GitHub setup notes for topics, description, and social preview

See [`docs/GITHUB_SETUP.md`](docs/GITHUB_SETUP.md) for the repo settings that cannot be fully represented as files.

## License

The mod metadata declares `CC0-1.0`, and this repository includes the matching `LICENSE` file. If that is not the intended license for the code and art, change both `LICENSE` and `src/main/resources/fabric.mod.json` before publishing releases.
