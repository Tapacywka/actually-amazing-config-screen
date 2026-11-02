# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each build is for one game and
loader, named in the build metadata after the version: `0.1.0+mc1.21.1-fabric` is version 0.1.0 for Minecraft 1.21.1
on Fabric.

## [Unreleased]

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
