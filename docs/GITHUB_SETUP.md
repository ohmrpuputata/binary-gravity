# GitHub Setup Notes

Some GitHub polish cannot be fully represented as files. Use this checklist in the repository settings after pushing these changes.

## About Section

Suggested description:

```text
Minecraft Fabric 1.21.1 survival mod about an escalating alien apocalypse: contamination, radiation, tactical swarms, structures, gear, and a day-8 boss.
```

Suggested topics:

```text
minecraft
fabric
minecraft-mod
fabric-mod
java
java-21
minecraft-1211
survival
alien-invasion
gameplay-mod
modding
```

Suggested website:

```text
https://github.com/physicaldazezzz/binary-gravity
```

## Social Preview

Use `docs/assets/repo-banner.svg` as the source artwork for a social preview. GitHub's social preview uploader may require a PNG, so export the SVG to PNG if the UI rejects it.

## Recommended Repository Settings

- Enable Issues.
- Enable Discussions if you want design/balance conversations outside bug reports.
- Protect the default branch once CI is green.
- Require the `Build` workflow for pull requests when the project is ready for outside contributors.
- Add a first GitHub Release only after confirming the license and testing the produced jar in a clean Fabric instance.

## Suggested Labels

```text
bug
enhancement
balance
content
crash
compatibility
documentation
good first issue
help wanted
needs reproduction
worldgen
rendering
ai
```

## Release Checklist

Before creating a public release:

- Run `.\gradlew.bat build`.
- Test the jar in a clean Minecraft `1.21.1` Fabric profile.
- Confirm Fabric API version compatibility.
- Confirm the license in `LICENSE` and `fabric.mod.json`.
- Add screenshots or a short gameplay clip to the release notes.
- List known issues honestly.
