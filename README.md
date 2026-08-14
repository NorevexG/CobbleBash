# CobbleBash Screen

CobbleBash Screen is a Minecraft 1.21.1 NeoForge add-on for CobbleBash 0.1.3. It replaces the Training Simulator interaction with a compact, technology-themed screen without replacing CobbleBash classes, registrations, or resources.

## Features

- Separate tabs for all 18 gyms and the Elite Four
- Nine gym cards visible at once with smooth continuous scrolling
- Permanent per-player challenge unlocks using the original CobbleBash Training Discs
- Separate unlock and challenge confirmation interactions
- Elite Four access remains locked behind completion of all 18 gyms
- Per-gym successful clear counts and total challenges started
- Cobblemon GUI click and PC unlock sounds
- English and Simplified Chinese interface translations

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.234 or newer
- CobbleBash 0.1.3
- CobbleBash's own required dependencies

The development build expects `libs/cobblebash-0.1.3.jar` and the other local dependency jars declared in `build.gradle`.

## Integration API

Other server-side mods can unlock challenges without consuming a disc through `TrainingSimulatorApi`:

```java
TrainingSimulatorApi.unlockGym(player, GymType.FIRE);
TrainingSimulatorApi.unlockAllGyms(player);

if (TrainingSimulatorApi.isEliteFourAvailable(player)) {
    TrainingSimulatorApi.unlockEliteFour(player);
}
```

`unlockEliteFour` always preserves CobbleBash's progression order and returns `false` until the player has completed all 18 gyms.

## Compatibility Design

The add-on opens its menu through NeoForge's block interaction event and calls CobbleBash's public `GymCommand` entry methods. One narrowly scoped Mixin observes successful returns from CobbleBash's private gym completion method so the UI can record repeat clear counts; it does not cancel or alter CobbleBash behavior.

Player UI progress is stored in the overworld data file `cobblebash_screen_training_simulator.dat`. The add-on owns only the `cobblebash_screen` registry and resource namespace.

## Build

Use Java 21 and run:

```shell
./gradlew build
```

The output is `build/libs/cobblebash_screen-0.1.0.jar`.

## Author

P1nero

## License

LGPL-3.0-only
