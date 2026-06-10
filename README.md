# Cobble Bash

Cobble Bash is a Minecraft 1.21.1 NeoForge addon for Cobblemon that adds private instanced type gyms, trainer battles, and Cobble Badges progression.

Players enter gyms through the Training Simulator, battle two trainers and a gym boss, and unlock first-tier Cobble Badges through gym completion. Each gym is generated in a private slot inside a custom void dimension and cleaned up after completion, defeat, logout, or leaving.

## Features

- 18 type-themed gyms
- Private per-player gym instances
- Trainer 1, Trainer 2, and Boss progression
- Dynamic trainer level scaling based on player gym progress
- Cobble Badges integration for first-tier badge unlocks
- JSON-driven trainer teams, moves, held items, abilities, and dialogue
- Random trainer build selection and shuffled Pokemon order
- Training Simulator block and type Training Disks
- Advancement and stat support for modpack quest integration

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Cobblemon 1.7.3 or newer
- RadicalCobblemonTrainerAPI 0.15.2-beta or newer
- Cobblemon Badges 4.0.0 or newer

## Development Notes

This repository contains the public source code for Cobble Bash. The local development build currently expects dependency jars in a `libs/` folder, but those third-party jars are not included in this repository.

To build locally, supply the required dependency jars or adjust `build.gradle` to resolve those dependencies from your own preferred repositories.

## License

Cobble Bash is licensed under the GNU Lesser General Public License v3.0 only (`LGPL-3.0-only`).

Official license text: https://www.gnu.org/licenses/lgpl-3.0.html
