# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each build is for one game and
loader, named in the build metadata after the version: `0.1.0+mc1.21.1-fabric` is version 0.1.0 for Minecraft 1.21.1
on Fabric.

## [Unreleased]

### Added
- A row with an action (`Tab.action`): its name on the left, a short muted link with an arrow on the right (gold under
  the pointer) that hands the settings screen to the mod, say to open a window of its own. API version 3.

### Changed
- The card no longer writes "name: value" under a picture unless the mod asks for it (`Preview.caption` is now false
  by default).

## [1.0.0] - 2026-11-03

The first stable version, for Minecraft 1.21.1, 26.1.2, 26.2 and 26.3, on Fabric and NeoForge.

### Changed
- The API in `net.tapacywka.aacs.api` is stable from now on: it only grows, older methods keep working.

## [0.1.0-beta.3] - 2026-11-03

A beta for every game: Minecraft 1.21.1, 26.1.2, 26.2 and 26.3, on Fabric and NeoForge.

### Changed
- A smaller file: unused drawing code and cursors removed, no local variable names in the classes.

### Fixed
- The rounded style: a lighter line no longer runs down the middle of the scroll bars.

## [0.1.0-beta.2] - 2026-11-02

### Added
- Minecraft 26.1.2, 26.2 and 26.3, on Fabric and NeoForge. For mod authors the API is the same, with the names 26.x
  uses: pictures and backgrounds draw on `GuiGraphicsExtractor`, icons are an `Identifier`.

## [0.1.0-beta.1] - 2026-11-02

### Added
- The settings file: values by sections in a JSON file of the mod (switches, whole and decimal numbers, choices,
  texts, lists). A changed value counts at once and the file is written when the screen closes. Bad values are
  forgiven: a number out of range is brought into it, an unknown choice or a wrong kind gets the default, keys the
  mod does not know stay in the file. A file that cannot be read is kept beside it as `.broken`.
- The settings screen, laid out over those values: tabs as wide as their titles, sections, sub-sections that fold,
  smooth scrolling with fading edges and a scroll bar that can be dragged, and the GUI scale held at most at the
  window's height over 360 while it is open.
- Rows: a switch, a slider of a number (a unit, a logarithmic scale, fixed values or words of the mod), a choice in
  steps on a slider over values of the mod's own, a choice with arrows from an enum or from a list the mod gives
  while the game runs, and a switch with a list of switches under it (with a search field).
- A small reset icon beside a row whose value differs from its default.
- The card beside the list: the name and description of the setting under the pointer, and the mod's picture of it
  for the whole tab or one row, with "name: value" under it. Without room for the card, the description shows as a
  tooltip under or over the row.
- The mod's own colours (every colour of the screen), shapes (rectangular or rounded), background, title icon and
  buttons left of Reset and Done.
- The settings button of the mod in Mod Menu and in the mod list of NeoForge, for every screen built.
- Sub-sections from other mods: a mod opens a tab to them, and another mod adds its own sub-section there with its
  own settings, shown by name after the tab's own rows (optionally only while a condition holds).

[Unreleased]: https://github.com/Tapacywka/actually-amazing-config-screen/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Tapacywka/actually-amazing-config-screen/compare/v0.1.0-beta.3...v1.0.0
[0.1.0-beta.3]: https://github.com/Tapacywka/actually-amazing-config-screen/compare/v0.1.0-beta.2...v0.1.0-beta.3
[0.1.0-beta.2]: https://github.com/Tapacywka/actually-amazing-config-screen/compare/v0.1.0-beta.1...v0.1.0-beta.2
[0.1.0-beta.1]: https://github.com/Tapacywka/actually-amazing-config-screen/releases/tag/v0.1.0-beta.1
