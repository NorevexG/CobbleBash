package com.nore.cobblebash.item;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import top.theillusivec4.curios.api.SlotContext;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RibbonAttributeManager {
    private static final Map<UUID, EnumMap<RibbonKind, RibbonGroup>> SESSIONS = new ConcurrentHashMap<>();

    private RibbonAttributeManager() {
    }

    public static void equip(SlotContext context, RibbonKind kind) {
        if (!(context.entity() instanceof ServerPlayer player)) {
            return;
        }

        EnumMap<RibbonKind, RibbonGroup> playerSessions = SESSIONS.computeIfAbsent(
                player.getUUID(),
                id -> new EnumMap<>(RibbonKind.class)
        );
        RibbonGroup group = playerSessions.computeIfAbsent(kind, ignored -> new RibbonGroup(kind, subscribe(player, kind)));
        group.sessions.put(SlotKey.from(context), new RibbonSession(SlotKey.from(context)));
        apply(player, group, true);
    }

    public static void unequip(SlotContext context, RibbonKind kind) {
        if (!(context.entity() instanceof ServerPlayer player)) {
            return;
        }

        EnumMap<RibbonKind, RibbonGroup> playerSessions = SESSIONS.get(player.getUUID());
        if (playerSessions == null) {
            removeModifiers(player, kind);
            return;
        }

        RibbonGroup group = playerSessions.get(kind);
        if (group == null) {
            removeModifiers(player, kind);
            return;
        }

        group.sessions.remove(SlotKey.from(context));
        if (group.sessions.isEmpty()) {
            playerSessions.remove(kind);
            group.close(player);
        } else {
            apply(player, group, true);
        }

        if (playerSessions.isEmpty()) {
            SESSIONS.remove(player.getUUID());
        }
    }

    public static void handlePlayerLogout(ServerPlayer player) {
        EnumMap<RibbonKind, RibbonGroup> playerSessions = SESSIONS.remove(player.getUUID());
        if (playerSessions == null) {
            return;
        }

        for (RibbonGroup group : playerSessions.values()) {
            group.close(player);
        }
    }

    public static List<TooltipTypeBonus> calculateTooltipBonuses(Player player, RibbonKind kind, double multiplier) {
        if (player == null || player.level() == null) {
            return List.of();
        }

        Map<String, TooltipTypeBonus> bonuses = new LinkedHashMap<>();
        try {
            PlayerPartyStore party = getParty(player.getUUID(), player.level().registryAccess());
            if (kind == RibbonKind.TRAINER) {
                addPokemonTooltip(bonuses, party.get(0), multiplier);
            } else {
                for (int slot = 0; slot < party.size(); slot++) {
                    addPokemonTooltip(bonuses, party.get(slot), multiplier);
                }
            }
        } catch (RuntimeException exception) {
            CobbleBash.LOGGER.warn("Failed to calculate Champion/Trainer Ribbon tooltip bonuses.", exception);
        }

        return new ArrayList<>(bonuses.values());
    }

    private static Object subscribe(ServerPlayer player, RibbonKind kind) {
        try {
            PlayerPartyStore party = getParty(player);
            Object observable = party.getClass().getMethod("getAnyChangeObservable").invoke(party);
            return observable.getClass().getMethod("subscribe", Consumer.class).invoke(observable, (Consumer<Object>) ignored -> {
                RibbonGroup group = getGroup(player, kind);
                if (group != null) {
                    apply(player, group, false);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException exception) {
            CobbleBash.LOGGER.warn("Failed to subscribe Champion/Trainer Ribbon to Cobblemon party changes.", exception);
            return null;
        }
    }

    private static RibbonGroup getGroup(ServerPlayer player, RibbonKind kind) {
        EnumMap<RibbonKind, RibbonGroup> playerSessions = SESSIONS.get(player.getUUID());
        return playerSessions == null ? null : playerSessions.get(kind);
    }

    private static void apply(ServerPlayer player, RibbonGroup group, boolean force) {
        RibbonModifiers modifiers = calculateAggregateModifiers(player, group);
        if (!force && modifiers.signature().equals(group.lastSignature)) {
            return;
        }

        removeModifiers(player, group.kind);
        modifiers.values().forEach((key, amount) -> addModifier(player, group.kind, key, amount));
        group.lastSignature = modifiers.signature();

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static RibbonModifiers calculateAggregateModifiers(ServerPlayer player, RibbonGroup group) {
        EnumMap<AttributeKey, Double> values = new EnumMap<>(AttributeKey.class);
        StringBuilder signature = new StringBuilder(group.kind.name());

        try {
            PlayerPartyStore party = getParty(player);
            List<RibbonSession> sessions = new ArrayList<>(group.sessions.values());
            sessions.sort(Comparator.comparing(session -> session.slotKey));

            for (int copy = 0; copy < sessions.size(); copy++) {
                double multiplier = Math.pow(0.5D, copy);
                signature.append("|copy").append(copy).append('@').append(sessions.get(copy).slotKey).append('=').append(multiplier);

                if (group.kind == RibbonKind.TRAINER) {
                    addPokemon(values, signature, party.get(0), multiplier);
                } else {
                    for (int slot = 0; slot < party.size(); slot++) {
                        addPokemon(values, signature, party.get(slot), multiplier);
                    }
                }
            }
        } catch (RuntimeException exception) {
            CobbleBash.LOGGER.warn("Failed to calculate Champion/Trainer Ribbon modifiers.", exception);
        }

        return new RibbonModifiers(values, signature.toString());
    }

    private static PlayerPartyStore getParty(ServerPlayer player) {
        return Cobblemon.INSTANCE.getStorage().getParty(player);
    }

    private static PlayerPartyStore getParty(UUID playerId, RegistryAccess registryAccess) {
        return Cobblemon.INSTANCE.getStorage().getParty(playerId, registryAccess);
    }

    private static void addPokemon(EnumMap<AttributeKey, Double> values, StringBuilder signature, Pokemon pokemon, double multiplier) {
        if (pokemon == null) {
            signature.append("|empty");
            return;
        }

        Map<String, ElementalType> types = new LinkedHashMap<>();
        for (ElementalType type : pokemon.getTypes()) {
            types.put(type.getName().toLowerCase(Locale.ROOT), type);
        }

        if (types.isEmpty()) {
            signature.append("|typeless");
            return;
        }

        double levelShare = 1.0D / types.size();
        for (String typeName : types.keySet()) {
            double levels = levelShare * multiplier;
            signature.append('|').append(typeName).append(':').append(levels);
            addType(values, typeName, levels);
        }
    }

    private static void addPokemonTooltip(Map<String, TooltipTypeBonus> bonuses, Pokemon pokemon, double multiplier) {
        if (pokemon == null) {
            return;
        }

        Map<String, ElementalType> types = new LinkedHashMap<>();
        for (ElementalType type : pokemon.getTypes()) {
            types.put(type.getName().toLowerCase(Locale.ROOT), type);
        }

        if (types.isEmpty()) {
            return;
        }

        double levelShare = 1.0D / types.size();
        for (Map.Entry<String, ElementalType> entry : types.entrySet()) {
            EnumMap<AttributeKey, Double> values = new EnumMap<>(AttributeKey.class);
            addType(values, entry.getKey(), levelShare * multiplier);
            TooltipTypeBonus bonus = bonuses.computeIfAbsent(
                    entry.getKey(),
                    ignored -> new TooltipTypeBonus(entry.getKey(), entry.getValue().getTextureXMultiplier(), new EnumMap<>(AttributeKey.class))
            );
            values.forEach((key, amount) -> bonus.attributes.merge(key, amount, Double::sum));
        }
    }

    private static void addType(EnumMap<AttributeKey, Double> values, String typeName, double levels) {
        switch (typeName) {
            case "fighting" -> add(values, AttributeKey.ATTACK_DAMAGE, 0.5D * levels);
            case "steel" -> add(values, AttributeKey.ARMOR, 1.0D * levels);
            case "rock" -> add(values, AttributeKey.MINING_EFFICIENCY, 1.0D * levels);
            case "ground" -> add(values, AttributeKey.MAX_HEALTH, 1.5D * levels);
            case "fairy" -> add(values, AttributeKey.LUCK, 0.5D * levels);
            case "electric" -> {
                add(values, AttributeKey.MOVEMENT_SPEED, 0.005D * levels);
                add(values, AttributeKey.ATTACK_SPEED, 0.05D * levels);
            }
            case "flying" -> {
                add(values, AttributeKey.SAFE_FALL_DISTANCE, 1.0D * levels);
                add(values, AttributeKey.FALL_DAMAGE_MULTIPLIER, -0.1D * levels);
            }
            case "psychic" -> {
                add(values, AttributeKey.BLOCK_INTERACTION_RANGE, 0.5D * levels);
                add(values, AttributeKey.ENTITY_INTERACTION_RANGE, 0.25D * levels);
            }
            case "water" -> {
                add(values, AttributeKey.WATER_MOVEMENT_EFFICIENCY, 0.15D * levels);
                add(values, AttributeKey.OXYGEN_BONUS, 1.0D * levels);
            }
            case "fire" -> {
                add(values, AttributeKey.BURNING_TIME, -0.1D * levels);
                add(values, AttributeKey.ATTACK_DAMAGE, 0.25D * levels);
            }
            case "dragon" -> {
                add(values, AttributeKey.EXPLOSION_KNOCKBACK_RESISTANCE, 0.1D * levels);
                add(values, AttributeKey.ATTACK_DAMAGE, 0.25D * levels);
            }
            case "normal" -> {
                add(values, AttributeKey.MAX_HEALTH, 0.5D * levels);
                add(values, AttributeKey.MOVEMENT_SPEED, 0.002D * levels);
                add(values, AttributeKey.ATTACK_DAMAGE, 0.15D * levels);
            }
            case "grass" -> {
                add(values, AttributeKey.MOVEMENT_EFFICIENCY, 0.1D * levels);
                add(values, AttributeKey.SWEEPING_DAMAGE_RATIO, 0.1D * levels);
                add(values, AttributeKey.MOVEMENT_SPEED, 0.003D * levels);
            }
            case "ghost" -> {
                add(values, AttributeKey.FOLLOW_RANGE, -2.0D * levels);
                add(values, AttributeKey.MAX_HEALTH, 1.0D * levels);
            }
            case "dark" -> {
                add(values, AttributeKey.SNEAKING_SPEED, 0.05D * levels);
                add(values, AttributeKey.FOLLOW_RANGE, -2.0D * levels);
            }
            case "bug" -> add(values, AttributeKey.ATTACK_SPEED, 0.2D * levels);
            case "ice" -> {
                add(values, AttributeKey.ARMOR_TOUGHNESS, 0.5D * levels);
                add(values, AttributeKey.KNOCKBACK_RESISTANCE, 0.05D * levels);
            }
            case "poison" -> {
                add(values, AttributeKey.ATTACK_DAMAGE, 0.25D * levels);
                add(values, AttributeKey.ATTACK_KNOCKBACK, 0.25D * levels);
            }
            default -> {
            }
        }
    }

    private static void add(EnumMap<AttributeKey, Double> values, AttributeKey key, double amount) {
        values.merge(key, amount, Double::sum);
    }

    private static void addModifier(ServerPlayer player, RibbonKind kind, AttributeKey key, double amount) {
        if (Math.abs(amount) < 0.000001D) {
            return;
        }

        AttributeInstance instance = player.getAttribute(key.attribute());
        if (instance == null) {
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId(kind, key),
                amount,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeModifiers(ServerPlayer player, RibbonKind kind) {
        for (AttributeKey key : AttributeKey.values()) {
            AttributeInstance instance = player.getAttribute(key.attribute());
            if (instance != null) {
                instance.removeModifier(modifierId(kind, key));
            }
        }
    }

    private static ResourceLocation modifierId(RibbonKind kind, AttributeKey key) {
        return ResourceLocation.fromNamespaceAndPath(
                CobbleBash.MODID,
                "ribbon/" + kind.name().toLowerCase(Locale.ROOT) + "/" + key.path
        );
    }

    public enum RibbonKind {
        TRAINER,
        CHAMPION
    }

    public enum AttributeKey {
        ARMOR("armor", "armor", Attributes.ARMOR),
        ARMOR_TOUGHNESS("armor_toughness", "toughness", Attributes.ARMOR_TOUGHNESS),
        ATTACK_DAMAGE("attack_damage", "damage", Attributes.ATTACK_DAMAGE),
        ATTACK_KNOCKBACK("attack_knockback", "knockback", Attributes.ATTACK_KNOCKBACK),
        ATTACK_SPEED("attack_speed", "attack speed", Attributes.ATTACK_SPEED),
        BLOCK_INTERACTION_RANGE("block_interaction_range", "block reach", Attributes.BLOCK_INTERACTION_RANGE),
        BURNING_TIME("burning_time", "burn time", Attributes.BURNING_TIME),
        EXPLOSION_KNOCKBACK_RESISTANCE("explosion_knockback_resistance", "explosion resist", Attributes.EXPLOSION_KNOCKBACK_RESISTANCE),
        ENTITY_INTERACTION_RANGE("entity_interaction_range", "entity reach", Attributes.ENTITY_INTERACTION_RANGE),
        FALL_DAMAGE_MULTIPLIER("fall_damage_multiplier", "fall damage", Attributes.FALL_DAMAGE_MULTIPLIER),
        FOLLOW_RANGE("follow_range", "mob vision", Attributes.FOLLOW_RANGE),
        KNOCKBACK_RESISTANCE("knockback_resistance", "kb resist", Attributes.KNOCKBACK_RESISTANCE),
        LUCK("luck", "luck", Attributes.LUCK),
        MAX_HEALTH("max_health", "health", Attributes.MAX_HEALTH),
        MINING_EFFICIENCY("mining_efficiency", "mining", Attributes.MINING_EFFICIENCY),
        MOVEMENT_EFFICIENCY("movement_efficiency", "move eff", Attributes.MOVEMENT_EFFICIENCY),
        MOVEMENT_SPEED("movement_speed", "speed", Attributes.MOVEMENT_SPEED),
        OXYGEN_BONUS("oxygen_bonus", "oxygen", Attributes.OXYGEN_BONUS),
        SAFE_FALL_DISTANCE("safe_fall_distance", "safe fall", Attributes.SAFE_FALL_DISTANCE),
        SNEAKING_SPEED("sneaking_speed", "sneak speed", Attributes.SNEAKING_SPEED),
        SWEEPING_DAMAGE_RATIO("sweeping_damage_ratio", "sweep", Attributes.SWEEPING_DAMAGE_RATIO),
        WATER_MOVEMENT_EFFICIENCY("water_movement_efficiency", "water move", Attributes.WATER_MOVEMENT_EFFICIENCY);

        private final String path;
        private final String displayName;
        private final Holder<Attribute> attribute;

        AttributeKey(String path, String displayName, Holder<Attribute> attribute) {
            this.path = path;
            this.displayName = displayName;
            this.attribute = attribute;
        }

        public String displayName() {
            return displayName;
        }

        private Holder<Attribute> attribute() {
            return attribute;
        }
    }

    private static final class RibbonGroup {
        private final RibbonKind kind;
        private final Object subscription;
        private final Map<SlotKey, RibbonSession> sessions = new LinkedHashMap<>();
        private String lastSignature = "";

        private RibbonGroup(RibbonKind kind, Object subscription) {
            this.kind = kind;
            this.subscription = subscription;
        }

        private void close(ServerPlayer player) {
            if (subscription != null) {
                try {
                    subscription.getClass().getMethod("unsubscribe").invoke(subscription);
                } catch (ReflectiveOperationException exception) {
                    CobbleBash.LOGGER.warn("Failed to unsubscribe Champion/Trainer Ribbon from Cobblemon party changes.", exception);
                }
            }
            removeModifiers(player, kind);
        }
    }

    private record SlotKey(String identifier, int index, boolean cosmetic) implements Comparable<SlotKey> {
        private static SlotKey from(SlotContext context) {
            return new SlotKey(context.identifier(), context.index(), context.cosmetic());
        }

        @Override
        public int compareTo(SlotKey other) {
            int identifierCompare = identifier.compareTo(other.identifier);
            if (identifierCompare != 0) {
                return identifierCompare;
            }

            int indexCompare = Integer.compare(index, other.index);
            return indexCompare != 0 ? indexCompare : Boolean.compare(cosmetic, other.cosmetic);
        }
    }

    private record RibbonSession(SlotKey slotKey) {
    }

    private record RibbonModifiers(EnumMap<AttributeKey, Double> values, String signature) {
    }

    public record TooltipTypeBonus(String typeName, int textureIndex, EnumMap<AttributeKey, Double> attributes) {
        public TooltipTypeBonus {
            Objects.requireNonNull(typeName);
            Objects.requireNonNull(attributes);
        }
    }
}
