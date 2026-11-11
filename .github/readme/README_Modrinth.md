<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/actually-amazing-config-screen/main/.github/images/icon.png" alt="Actually Amazing Config Screen" width="112">
</p>

<p align="center">
  <a href="https://modrinth.com/mod/actually-amazing-config-screen"><img src="https://img.shields.io/modrinth/dt/actually-amazing-config-screen?style=flat&logo=modrinth&label=modrinth&color=00AF5C" alt="Modrinth downloads"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/actually-amazing-config-screen"><img src="https://img.shields.io/curseforge/dt/1779712?style=flat&logo=curseforge&label=curseforge&color=F16436" alt="CurseForge downloads"></a>
  <a href="https://github.com/Tapacywka/actually-amazing-config-screen/releases"><img src="https://img.shields.io/github/v/release/Tapacywka/actually-amazing-config-screen?style=flat&label=release&color=E6B85C" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/side-client-5B8DEF?style=flat" alt="Client side">
  <a href="https://github.com/Tapacywka/actually-amazing-config-screen/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-8A8F98?style=flat" alt="MIT license"></a>
</p>

<p align="center">
  <b>A settings screen for your Fabric or NeoForge mod that players actually enjoy using.</b><br>
  Describe your settings once and get tabs, smooth scrolling and a card that shows what each setting does.
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/actually-amazing-config-screen/main/.github/images/card.jpg" alt="A settings screen with tabs and the card explaining the setting under the pointer" width="100%">
</p>

<h2 id="players">🎮 For players</h2>

You don't need to install this. It comes inside the mods that use it, such as
[Teyvat Cartographia](https://github.com/Tapacywka/teyvat-cartographia), and opens from their settings button in Mod Menu or NeoForge's mod list.

<h2 id="features">✨ What you get</h2>

- **Tabs and sections**: tabs as wide as their titles (they scroll when there are many), sub-sections that fold.
- **A preview card**: point at a setting and see its name, description and a picture of what it changes. In a narrow window the description becomes a tooltip.
- **Every kind of row**: switches, sliders with units or steps, choices with arrows, a switch with a searchable list, a button that opens your own window.
- **Smooth everything**: scrolling without steps, rows that fade at the edges, a reset icon beside every changed setting.
- **Your look**: rectangular or rounded, your colours and background, kept at a readable size on any screen.
- **Room for others**: a tab where other mods can add their own settings.
- **Settings button** in Mod Menu and in NeoForge's mod list, set up for you.

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/actually-amazing-config-screen/main/.github/images/rounded.jpg" alt="The rounded style" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/actually-amazing-config-screen/main/.github/images/list.jpg" alt="A switch with a searchable list" width="49%">
</p>

<h2 id="authors">🛠️ For mod authors</h2>

Embed it in your JAR, so your players have nothing to install. It is on [Modrinth Maven](https://support.modrinth.com/en/articles/8801191-modrinth-maven):

```groovy
repositories { maven { url = 'https://api.modrinth.com/maven' } }

dependencies {
    // Fabric (on 26.x: implementation instead of modImplementation)
    include(modImplementation('maven.modrinth:actually-amazing-config-screen:1.3.0+mc1.21.1-fabric'))
    // NeoForge
    jarJar(implementation('maven.modrinth:actually-amazing-config-screen:1.3.0+mc1.21.1-neoforge'))
}
```

Describe the settings, then the screen:

```java
ConfigFile.Builder b = ConfigFile.builder();
BoolValue grid = b.bool("grid", true);
IntValue opacity = b.integer("opacity", 80, 0, 100);
ConfigFile file = b.build();
file.load(configDir.resolve("mymod-client.json"));

AacsScreen screen = AacsScreen.builder("mymod", file)
        .tab("mymod.config.tab.display", t -> t
                .toggle(grid)
                .slider(opacity, s -> s.unit("%")))
        .build();

Minecraft.getInstance().setScreen(screen.open(parent));
```

Names and descriptions come from your lang files (`mymod.config.<key>` and `mymod.config.<key>.tooltip`). The API in `net.tapacywka.aacs.api` is stable since 1.0.0: it only grows, nothing is removed.

<h2 id="versions">📦 Versions</h2>

Minecraft **1.21.1**, **26.1.2**, **26.2** and **26.3**, on **Fabric** (no Fabric API needed) and **NeoForge**. The Fabric file also runs on NeoForge through Sinytra Connector.

<h2 id="links">🔗 Links</h2>

- [Report a bug or suggest an idea](https://github.com/Tapacywka/actually-amazing-config-screen/issues/new/choose)
- [Changelog](https://github.com/Tapacywka/actually-amazing-config-screen/blob/main/CHANGELOG.md)
- [Source code](https://github.com/Tapacywka/actually-amazing-config-screen): `./gradlew build`, and `./gradlew :fabric:runClient` opens a test mod with every kind of row

<sub>MIT License. Icons from Nieobie's Game Icon Pack (CC0).</sub>
