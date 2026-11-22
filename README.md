# Combo HUD

Combo HUD is a client-side Fabric mod for Minecraft 26.3 that displays a sword-hit combo counter. Customize the label and counter colors, move or resize each element, choose which targets count, and adjust the combo timeout.

## Install

Use Minecraft 26.3 with Fabric Loader and Fabric API for 26.3. Place the mod JAR in your instance’s mods folder. Install Mod Menu to open the Combo HUD settings from the mods screen.

## Build from source

Install JDK 25 and open a terminal in the folder containing build.gradle. The first build requires internet access to download Gradle and dependencies.

On Windows, run:

```powershell
.\gradlew.bat build
```

On macOS or Linux, run:

```sh
sh gradlew build
```

The playable mod is build/libs/Combo-HUD-1.2.0.jar. The separate sources JAR contains the Java code.
