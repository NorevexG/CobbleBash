# CobbleBash

CobbleBash is a Minecraft 1.21.1 NeoForge addon for Cobblemon that adds a private instanced gym progression system, Elite Four challenge, trainer battles, and Cobble Badges progression.

Players enter private gym instances through the Training Simulator, battle two trainers and a gym boss, and earn first-tier Cobble Badges through Cobble Badges integration. Each gym is generated in a custom void dimension, reused where possible, and reset between runs.

## Features

- 18 type-themed gyms
- Elite Four and Champion challenge
- Private per-player gym instances
- Trainer 1, Trainer 2, and boss progression
- Dynamic trainer level scaling based on player gym progress
- Cobble Badges integration for first-tier badge unlocks
- Data pack driven trainer teams, moves, held items, abilities, rewards, and dialogue
- Random trainer and gym leader visual variants
- Training Simulator block and type Training Disks
- Trainer Ribbon, Champion Ribbon, Champion Beacon, and Champion Upgrade Smithing Template
- Advancement and stat support for modpack and quest integration
- Optional Cobble Dollars repeat-clear compatibility

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Cobblemon 1.7.3 or newer
- RadicalCobblemonTrainerAPI 0.15.2-beta or newer
- Cobblemon Badges 4.0.0 or newer
- Curios 9.5.1+1.21.1 or newer

## Optional Compatibility

- Cobble Dollars 2.0.0 or newer

Cobble Dollars is optional. CobbleBash should load normally without it installed.

## Development Notes

This repository contains the public source code for CobbleBash. The local development build expects dependency jars in a `libs/` folder, but those third-party jars are not included in this repository.

To build locally, supply the required dependency jars or adjust `build.gradle` to resolve those dependencies from your preferred repositories.

Required local filenames are listed in the `dependencies` block in `build.gradle`. The project uses Java 21; after providing those jars, run `./gradlew build`. On Unix-like systems the wrapper is checked in as executable.

## License

CobbleBash is licensed under the GNU Lesser General Public License v3.0 only (`LGPL-3.0-only`).

Official license text: https://www.gnu.org/licenses/lgpl-3.0.html
