# Ars Sophisticated

Ars Sophisticated adds five Repository tiers to Ars Nouveau with support for Sophisticated Storage upgrades. Link an Ars Nouveau Storage Lectern to a Repository Catalog to access a chain of regular and upgraded Repositories.

| Repository | Storage slots | Upgrade slots |
| --- | ---: | ---: |
| Copper | 72 | 1 |
| Iron | 81 | 1 |
| Gold | 108 | 2 |
| Diamond | 144 | 3 |
| Netherite | 168 | 4 |

Stack upgrades increase capacity in the Repository where they are installed. Use a compatible Sophisticated Storage Tier Upgrade on a placed Repository to upgrade it in place and keep its contents, name, and installed upgrades.

## Download and requirements

Download the mod from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ars-sophisticated). This project targets **Minecraft 1.21.1** with **NeoForge** and **Java 21**.

Required mods and tested versions:

| Mod | Tested version |
| --- | --- |
| Ars Nouveau | 5.13.4 |
| Sophisticated Core | 1.5.31.2460 |
| Sophisticated Storage | 1.6.16.2222 |

## Build and development

Install a Java 21 JDK, then run these commands from the project root:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runGameTestServer
```

On macOS or Linux, use `./gradlew` in place of `.\gradlew.bat`. The build writes the mod JAR to `build/libs/`. The development client uses `run/` for its saves and settings.

## License

Ars Sophisticated is All Rights Reserved.
