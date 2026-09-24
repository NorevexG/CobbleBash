# CobbleBash

CobbleBash is a Minecraft 1.21.1 NeoForge addon for Cobblemon that adds a private instanced gym progression system, Elite Four challenge, trainer battles, and Cobble Badges progression.

Players enter private gym instances through the Training Simulator, battle two trainers and a gym boss, and earn first-tier Cobble Badges through Cobble Badges integration. Each gym is generated in a custom void dimension, reused where possible, and reset between runs.

## Features

- 18 type-themed gyms
- Elite Four and Champion challenge
- Private per-player gym instances
- Trainer 1, Trainer 2, and boss progression
- Dynamic trainer level scaling based on player gym progress
- Configurable 18-gym level progression and trainer/leader level offsets
- Level-specific trainer loadouts with safe nearest-range fallback
- Configurable per-battle bag item limit for regular gyms and a shared Elite Four item limit
- Data pack selectable single, double, or random trainer battles
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

## Server Configuration

CobbleBash creates `config/cobblebash-common.toml`. The `gymBaseLevels` list contains the base Pokemon level for gym attempts 1 through 18, ordered by how many gyms the player has completed rather than by gym type. `trainerOneLevelOffset`, `trainerTwoLevelOffset`, and `gymLeaderLevelOffset` are added to that base level and the result is clamped to 1 through 100.

`maxBattleItemUses` limits each player to that many bag-item uses during each individual regular CobbleBash gym trainer battle. It defaults to `5`, resets at the start of every battle, and does not restrict items outside battle. Set it to `-1` for unlimited battle items or `0` to prohibit them during battle.

`eliteFourMaxItemUses` is a single shared allowance for battle items used across the full Elite Four and Champion attempt, including healing and revival items used between battles. It defaults to `25` and resets when the attempt ends. Set it to `-1` for unlimited use or `0` to prohibit battle items during the challenge. The counted item set is data-pack extensible through the `cobblebash:elite_four_battle_items` item tag.

The default curve begins at levels 14/16/18 for Trainer 1, Trainer 2, and the Gym Leader, and reaches 91/93/95 at the eighteenth gym. The leader's fifth and sixth Pokemon rise above the leader level, capped at levels 97 and 100 respectively.

## Trainer Data Pack Options

Trainer files under `data/cobblebash/gym_trainers/<gym>/<trainer>.json` support these optional top-level properties:

```json
{
  "battle_format": "singles",
  "shuffle_team": true
}
```

`battle_format` accepts `singles`, `doubles`, or `random` and defaults to `singles`. A double battle uses the same NPC and team but allows two active Pokemon per side. Forced doubles remain valid when the player has only one usable Pokemon. `random` chooses between singles and doubles, but chooses singles when the player has fewer than two usable Pokemon.

`shuffle_team` controls whether the selected build's Pokemon order is shuffled when the trainer is registered. It defaults to `true`, preserving the original behavior. Set it to `false` when a curated lead order matters, particularly for double-battle teams.

Each entry in `builds` may also define an inclusive level range:

```json
{
  "builds": [
    {
      "id": "early_game",
      "name": "Early Game",
      "min_level": 10,
      "max_level": 30,
      "pokemon": [
        { "species": "Butterfree", "moves": ["Bug Buzz"] }
      ]
    },
    {
      "id": "late_game",
      "name": "Late Game",
      "min_level": 31,
      "max_level": 100,
      "pokemon": [
        { "species": "Volcarona", "moves": ["Bug Buzz"] }
      ]
    }
  ]
}
```

The active trainer level is compared with `min_level` and `max_level`, and one matching build is selected randomly. Overlapping ranges are allowed and make all matching builds eligible. Both fields are optional and default to `0` and `100`, so all bundled trainer data retains its existing behavior. If a data pack leaves a gap, CobbleBash randomly selects among the nearest build ranges, logs a server warning, and warns the player in chat when the battle begins.

## Development Notes

This repository contains the public source code for CobbleBash. The local development build expects dependency jars in a `libs/` folder, but those third-party jars are not included in this repository.

To build locally, supply the required dependency jars or adjust `build.gradle` to resolve those dependencies from your preferred repositories.

## License

CobbleBash is licensed under the GNU Lesser General Public License v3.0 only (`LGPL-3.0-only`).

Official license text: https://www.gnu.org/licenses/lgpl-3.0.html
