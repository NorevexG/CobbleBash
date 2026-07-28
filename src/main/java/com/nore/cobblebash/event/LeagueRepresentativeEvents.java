package com.nore.cobblebash.event;

import com.cobblemon.mod.common.CobblemonItems;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.gym.GymType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeagueRepresentativeEvents {
    private static final ResourceLocation END_CITY_TREASURE = ResourceLocation.withDefaultNamespace("chests/end_city_treasure");
    private static final Map<ResourceLocation, List<LootInjection>> LOOT_INJECTIONS = buildLootInjections();

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != CobbleBash.LEAGUE_REPRESENTATIVE.get()) {
            return;
        }

        addTrades(event, 1,
                item(CobblemonItems.POKE_BALL, 2, 1, 16, 1),
                item(CobblemonItems.HEAL_BALL, 4, 1, 12, 1),
                item(CobblemonItems.NEST_BALL, 5, 1, 12, 1),
                item(CobblemonItems.X_ATTACK, 3, 1, 12, 2),
                item(CobblemonItems.X_DEFENSE, 3, 1, 12, 2),
                item(CobblemonItems.X_SPEED, 3, 1, 12, 2),
                item(CobblemonItems.X_ACCURACY, 3, 1, 12, 2)
        );
        addTrades(event, 2,
                disk(40, 3, 8),
                item(CobblemonItems.GREAT_BALL, 6, 1, 12, 5),
                item(CobblemonItems.FRIEND_BALL, 8, 1, 8, 5),
                item(CobblemonItems.LURE_BALL, 8, 1, 8, 5),
                item(CobblemonItems.DIRE_HIT, 5, 1, 10, 5),
                item(CobblemonItems.GUARD_SPEC, 5, 1, 10, 5),
                item(CobblemonItems.X_SP_ATK, 4, 1, 12, 5),
                item(CobblemonItems.X_SP_DEF, 4, 1, 12, 5)
        );
        addTrades(event, 3,
                disk(32, 4, 10),
                disk(32, 4, 10),
                disk(32, 4, 10),
                disk(32, 4, 10),
                item(CobblemonItems.QUICK_BALL, 10, 1, 8, 10),
                item(CobblemonItems.REPEAT_BALL, 9, 1, 8, 10),
                item(CobblemonItems.TIMER_BALL, 9, 1, 8, 10),
                item(CobblemonItems.NET_BALL, 9, 1, 8, 10),
                item(CobblemonItems.DIVE_BALL, 9, 1, 8, 10),
                item(CobblemonItems.REVIVE, 10, 1, 2, 10),
                item(CobblemonItems.ETHER, 8, 1, 3, 10),
                item(CobblemonItems.EXPERIENCE_CANDY_XS, 12, 1, 3, 10)
        );
        addTrades(event, 4,
                disk(24, 5, 15),
                disk(24, 5, 15),
                disk(24, 5, 15),
                disk(24, 5, 15),
                disk(24, 5, 15),
                disk(24, 5, 15),
                item(CobblemonItems.DUSK_BALL, 12, 1, 8, 15),
                item(CobblemonItems.LUXURY_BALL, 12, 1, 8, 15),
                item(CobblemonItems.ELIXIR, 14, 1, 2, 15),
                item(CobblemonItems.MAX_ETHER, 18, 1, 2, 15),
                item(CobblemonItems.EXPERIENCE_CANDY_S, 20, 1, 2, 15)
        );
        addTrades(event, 5,
                disk(18, 6, 30),
                disk(18, 6, 30),
                disk(18, 6, 30),
                disk(18, 6, 30),
                disk(18, 6, 30),
                disk(18, 6, 30),
                eliteFourDisk(48, 1, 30),
                item(CobblemonItems.MOON_BALL, 14, 1, 8, 30),
                item(CobblemonItems.LEVEL_BALL, 14, 1, 8, 30),
                item(CobblemonItems.FAST_BALL, 14, 1, 8, 30),
                item(CobblemonItems.MAX_ELIXIR, 28, 1, 1, 30)
        );
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        List<LootInjection> injections = LOOT_INJECTIONS.get(event.getName());
        if (injections != null && !injections.isEmpty()) {
            for (LootInjection injection : injections) {
                event.getTable().addPool(LootPool.lootPool()
                        .name("cobblebash_" + injection.gymType().getId() + "_training_disk")
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(CobbleBash.TRAINING_DISKS.get(injection.gymType()).get()).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(injection.emptyWeight()))
                        .build());
            }
        }

        if (END_CITY_TREASURE.equals(event.getName())) {
            event.getTable().addPool(LootPool.lootPool()
                    .name("cobblebash_elite_four_training_disk")
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(CobbleBash.ELITE_FOUR_TRAINING_DISK.get()).setWeight(1))
                    .add(EmptyLootItem.emptyItem().setWeight(23))
                    .build());
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleRepresentativeInteraction(event.getEntity(), event.getTarget());
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleRepresentativeInteraction(event.getEntity(), event.getTarget());
    }

    @SubscribeEvent
    public void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getAbstractVillager() instanceof Villager villager
                && villager.getVillagerData().getProfession() == CobbleBash.LEAGUE_REPRESENTATIVE.get()) {
            ensureMasterTrainingDiskOffer(villager);
            CobbleBashCriteriaTriggers.triggerLeagueRepresentativeTraded(player);
        }
    }

    private static void handleRepresentativeInteraction(Entity playerEntity, Entity target) {
        if (playerEntity instanceof ServerPlayer player
                && target instanceof Villager villager
                && villager.getVillagerData().getProfession() == CobbleBash.LEAGUE_REPRESENTATIVE.get()) {
            ensureMasterTrainingDiskOffer(villager);
            CobbleBashCriteriaTriggers.triggerLeagueRepresentativeMet(player);
        }
    }

    private static void addTrades(VillagerTradesEvent event, int level, VillagerTrades.ItemListing... trades) {
        List<VillagerTrades.ItemListing> levelTrades = event.getTrades().computeIfAbsent(level, ignored -> new ArrayList<>());
        levelTrades.addAll(List.of(trades));
    }

    private static VillagerTrades.ItemListing item(Item item, int emeraldCost, int count, int maxUses, int xp) {
        return new VillagerTrades.ItemsForEmeralds(item, emeraldCost, count, maxUses, xp);
    }

    private static VillagerTrades.ItemListing disk(int emeraldCost, int maxUses, int xp) {
        return new RandomTrainingDiskForEmeralds(emeraldCost, maxUses, xp);
    }

    private static VillagerTrades.ItemListing eliteFourDisk(int emeraldCost, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(CobbleBash.ELITE_FOUR_TRAINING_DISK.get()),
                maxUses,
                xp,
                0.05F
        );
    }

    private static void ensureMasterTrainingDiskOffer(Villager villager) {
        if (villager.getVillagerData().getLevel() < 5 || hasTrainingDiskOffer(villager)) {
            return;
        }

        MerchantOffer offer = new RandomTrainingDiskForEmeralds(18, 6, 30).getOffer(villager, villager.getRandom());
        if (offer != null) {
            villager.getOffers().add(offer);
        }
    }

    private static boolean hasTrainingDiskOffer(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (isTrainingDisk(offer.getResult())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrainingDisk(ItemStack stack) {
        for (GymType type : GymType.values()) {
            if (stack.is(CobbleBash.TRAINING_DISKS.get(type).get())) {
                return true;
            }
        }
        return stack.is(CobbleBash.ELITE_FOUR_TRAINING_DISK.get());
    }

    private static List<ItemLike> buildTrainingDiskList() {
        List<ItemLike> disks = new ArrayList<>();
        for (GymType type : GymType.values()) {
            disks.add(CobbleBash.TRAINING_DISKS.get(type).get());
        }
        return List.copyOf(disks);
    }

    private static Map<ResourceLocation, List<LootInjection>> buildLootInjections() {
        Map<ResourceLocation, List<LootInjection>> injections = new java.util.HashMap<>();
        add(injections, GymType.NORMAL, 7, "village/village_plains_house", "simple_dungeon");
        add(injections, GymType.FIGHTING, 7, "pillager_outpost", "trial_chambers/reward");
        add(injections, GymType.FLYING, 7, "pillager_outpost", "shipwreck_map", "trial_chambers/supply");
        add(injections, GymType.POISON, 7, "jungle_temple", "abandoned_mineshaft");
        add(injections, GymType.GROUND, 7, "desert_pyramid", "village/village_desert_house");
        add(injections, GymType.ROCK, 7, "abandoned_mineshaft", "trial_chambers/corridor");
        add(injections, GymType.BUG, 7, "jungle_temple", "abandoned_mineshaft", "village/village_taiga_house");
        add(injections, GymType.GHOST, 7, "woodland_mansion", "ancient_city");
        add(injections, GymType.STEEL, 7, "village/village_armorer", "village/village_weaponsmith", "trial_chambers/reward");
        add(injections, GymType.FIRE, 7, "ruined_portal", "nether_bridge", "bastion_treasure");
        add(injections, GymType.WATER, 7, "shipwreck_treasure", "shipwreck_supply", "buried_treasure", "underwater_ruin_big", "underwater_ruin_small");
        add(injections, GymType.GRASS, 7, "jungle_temple", "village/village_plains_house", "village/village_taiga_house");
        add(injections, GymType.ELECTRIC, 7, "trial_chambers/reward", "ancient_city", "ruined_portal");
        add(injections, GymType.PSYCHIC, 7, "stronghold_library", "ancient_city");
        add(injections, GymType.ICE, 7, "igloo_chest", "ancient_city_ice_box");
        add(injections, GymType.DRAGON, 7, "end_city_treasure", "stronghold_library", "stronghold_corridor");
        add(injections, GymType.DARK, 7, "woodland_mansion", "ancient_city");
        add(injections, GymType.FAIRY, 7, "woodland_mansion", "village/village_temple", "stronghold_library");
        return Map.copyOf(injections);
    }

    private static void add(Map<ResourceLocation, List<LootInjection>> injections, GymType type, int emptyWeight, String... tables) {
        for (String table : tables) {
            ResourceLocation id = ResourceLocation.withDefaultNamespace("chests/" + table);
            injections.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new LootInjection(type, emptyWeight));
        }
    }

    private record LootInjection(GymType gymType, int emptyWeight) {
    }

    private static final class RandomTrainingDiskForEmeralds implements VillagerTrades.ItemListing {
        private final int emeraldCost;
        private final int maxUses;
        private final int xp;

        private RandomTrainingDiskForEmeralds(int emeraldCost, int maxUses, int xp) {
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            List<ItemLike> disks = buildTrainingDiskList();
            if (disks.isEmpty()) {
                return null;
            }

            ItemLike disk = disks.get(random.nextInt(disks.size()));
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, emeraldCost),
                    new ItemStack(disk),
                    maxUses,
                    xp,
                    0.05F
            );
        }
    }
}
