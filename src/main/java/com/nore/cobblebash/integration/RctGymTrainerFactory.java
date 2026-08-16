package com.nore.cobblebash.integration;

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel.StatsModel;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.util.JTO;
import com.mojang.logging.LogUtils;
import com.nore.cobblebash.util.CobbleBashText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RctGymTrainerFactory {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<TrainerModel> createTrainer(MinecraftServer server, String gymType, String trainerIdPart, int level) {
        return createTrainer(server, gymType, trainerIdPart, level, null);
    }

    public static Optional<TrainerModel> createTrainer(MinecraftServer server, String gymType, String trainerIdPart, int level, String displayNameOverride) {
        return RctTrainerDataLoader.load(server, gymType, trainerIdPart)
                .map(data -> {
                    RctTrainerDataLoader.BuildData build = selectBuild(data.builds());
                    List<RctTrainerDataLoader.PokemonData> shuffledTeam = new ArrayList<>(build.pokemon());
                    Collections.shuffle(shuffledTeam);
                    String displayName = displayNameOverride == null || displayNameOverride.isBlank()
                            ? data.displayName()
                            : displayNameOverride;

                    return new TrainerModel(
                            CobbleBashText.rctText(displayName),
                            JTO.<BattleAI>of(RCTBattleAI::new),
                            List.of(),
                            shuffledTeam.stream()
                            .map(pokemon -> createPokemon(pokemon, level))
                            .toList()
                    );
                });
    }

    public static Optional<String> getTrainerDisplayName(MinecraftServer server, String gymType, String trainerIdPart) {
        return RctTrainerDataLoader.load(server, gymType, trainerIdPart)
                .map(RctTrainerDataLoader.TrainerData::displayName);
    }

    private static PokemonModel createPokemon(RctTrainerDataLoader.PokemonData data, int level) {
        StatsModel ivs = new StatsModel(31, 31, 31, 31, 31, 31);
        StatsModel evs = new StatsModel(0, 0, 0, 0, 0, 0);

        return new PokemonModel(
                resolveSpeciesForRct(data.species()),
                "MALE",
                data.levelOr(level),
                "hardy",
                data.ability(),
                new LinkedHashSet<>(data.moves()),
                ivs,
                evs,
                false,
                data.heldItem(),
                new LinkedHashSet<>(data.aspects())
        );
    }

    private static String resolveSpeciesForRct(String requestedSpecies) {
        String requested = requestedSpecies == null ? "" : requestedSpecies.trim().toLowerCase(Locale.ROOT);
        if (requested.isBlank()) {
            return "";
        }

        for (ResourceLocation candidate : speciesIdCandidates(requested)) {
            Species species = PokemonSpecies.getByIdentifier(candidate);
            if (species != null) {
                return species.getResourceIdentifier().toString();
            }
        }

        String rawNameCandidate = requested.contains(":")
                ? requested.substring(requested.indexOf(':') + 1)
                : requested;
        int lastSlash = rawNameCandidate.lastIndexOf('/');
        String nameCandidate = rawNameCandidate;
        if (lastSlash >= 0) {
            nameCandidate = rawNameCandidate.substring(lastSlash + 1);
        }
        final String finalNameCandidate = nameCandidate;

        Species byName = PokemonSpecies.getByName(finalNameCandidate);
        if (byName != null) {
            return byName.getResourceIdentifier().toString();
        }

        Species matchedSpecies = PokemonSpecies.getSpecies().stream()
                .filter(species -> speciesMatches(species, requested, finalNameCandidate))
                .findFirst()
                .orElse(null);
        if (matchedSpecies != null) {
            return matchedSpecies.getResourceIdentifier().toString();
        }

        LOGGER.warn(
                "Could not resolve CobbleBash trainer species '{}'. Close loaded Cobblemon species: {}",
                requestedSpecies,
                describeCloseSpeciesMatches(finalNameCandidate)
        );
        return requested;
    }

    private static List<ResourceLocation> speciesIdCandidates(String requested) {
        List<ResourceLocation> candidates = new ArrayList<>();

        if (requested.contains(":")) {
            ResourceLocation exact = ResourceLocation.tryParse(requested);
            if (exact != null) {
                candidates.add(exact);
            }
            return candidates;
        }

        candidates.add(ResourceLocation.fromNamespaceAndPath("cobblemon", requested));
        candidates.add(ResourceLocation.fromNamespaceAndPath("cobblemon", "custom/" + requested));
        candidates.add(ResourceLocation.fromNamespaceAndPath("cobblemon_alatia", requested));
        return candidates;
    }

    private static boolean speciesMatches(Species species, String requested, String nameCandidate) {
        ResourceLocation id = species.getResourceIdentifier();
        String path = id.getPath();
        return species.getName().equalsIgnoreCase(nameCandidate)
                || id.toString().equalsIgnoreCase(requested)
                || path.equalsIgnoreCase(nameCandidate)
                || path.endsWith("/" + nameCandidate);
    }

    private static String describeCloseSpeciesMatches(String nameCandidate) {
        List<String> matches = PokemonSpecies.getSpecies().stream()
                .filter(species -> {
                    String id = species.getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
                    String name = species.getName().toLowerCase(Locale.ROOT);
                    return id.contains(nameCandidate) || name.contains(nameCandidate);
                })
                .limit(8)
                .map(species -> species.getResourceIdentifier() + " (" + species.getName() + ")")
                .toList();

        if (matches.isEmpty()) {
            return "none";
        }

        return String.join(", ", matches);
    }

    private static RctTrainerDataLoader.BuildData selectBuild(List<RctTrainerDataLoader.BuildData> builds) {
        return builds.get(ThreadLocalRandom.current().nextInt(builds.size()));
    }
}
