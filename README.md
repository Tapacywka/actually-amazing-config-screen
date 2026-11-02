# Actually Amazing Config Screen

A settings screen library for Minecraft mods on Fabric and NeoForge (1.21.1): tabs, sections that fold, smooth
scrolling, and a card beside the list with the description and a picture of the setting under the pointer. It grew
out of the settings screen of [Teyvat Cartographia](https://github.com/Tapacywka/teyvat-cartographia), which uses it.

Early development: the API (`net.tapacywka.aacs.api`) may still change before 1.0.0, and there are no releases yet.
Mods embed it in their JAR (Fabric `include`, NeoForge `jarJar`); players have nothing to install.

Building: `./gradlew build publishToMavenLocal` puts `net.tapacywka.aacs:aacs-common`, `aacs-fabric` and
`aacs-neoforge` in the local Maven repository.

Licence: MIT.
