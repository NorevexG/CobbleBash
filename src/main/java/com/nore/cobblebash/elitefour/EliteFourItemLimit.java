package com.nore.cobblebash.elitefour;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.pokemon.healing.PokemonHealedEvent;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.structure.EliteFourStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the one shared item allowance for an entire Elite Four run. */
public final class EliteFourItemLimit {
    private static final int ACTION_BAR_REFRESH_TICKS = 40;
    private static final int PENDING_USE_TIMEOUT_TICKS = 20 * 60;
    private static final TagKey<Item> COUNTED_ITEMS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "elite_four_battle_items")
    );
    private static final Map<UUID, PendingItemUse> PENDING_USES = new ConcurrentHashMap<>();

    private EliteFourItemLimit() {
    }

    public static int battleAllowance(ServerPlayer player) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance == null) {
            return Config.MAX_BATTLE_ITEM_USES.get();
        }

        int maximum = Config.ELITE_FOUR_MAX_ITEM_USES.get();
        return maximum < 0 ? -1 : Math.max(0, maximum - instance.getEliteFourItemUses());
    }

    public static void prepareForBattle(ServerPlayer player) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance != null) {
            instance.beginEliteFourBattleItemSync();
            PENDING_USES.remove(player.getUUID());
            showCounter(player, instance);
        }
    }

    /**
     * Keep the live count already observed during the battle. RCT's finalized state can replay
     * bag-item instructions during teardown, so reading it again here would double-count uses.
     */
    public static void finishBattle(ServerPlayer player) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance != null) {
            instance.finishEliteFourBattleItemSync();
            PENDING_USES.remove(player.getUUID());
            showCounter(player, instance);
        }
    }

    /** Returns false when a tagged item must be blocked before Cobblemon opens its target picker. */
    public static boolean beginPotentialUse(ServerPlayer player, ItemStack stack) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance == null || !stack.is(COUNTED_ITEMS)) {
            return true;
        }

        int maximum = Config.ELITE_FOUR_MAX_ITEM_USES.get();
        if (maximum < 0) {
            return true;
        }

        if (instance.getEliteFourItemUses() >= maximum) {
            showCounter(player, instance);
            return false;
        }

        if (BattleRegistry.getBattleByParticipatingPlayer(player) == null) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            PENDING_USES.put(
                    player.getUUID(),
                    new PendingItemUse(itemId, countItem(player, itemId), PENDING_USE_TIMEOUT_TICKS)
            );
        }

        return true;
    }

    /**
     * Cobblemon emits this cancellable event after validating HP healing/revival but before applying it.
     * In-battle uses are counted by RCT's successful bag-item instruction instead.
     */
    public static void handlePokemonHealed(PokemonHealedEvent event) {
        ServerPlayer player = event.getPokemon().getOwnerPlayer();
        if (player == null || BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            return;
        }

        GymInstance instance = getEliteFourInstance(player);
        if (instance == null || !(event.getSource() instanceof Item item)) {
            return;
        }

        ItemStack sourceStack = new ItemStack(item);
        if (!sourceStack.is(COUNTED_ITEMS)) {
            return;
        }

        int maximum = Config.ELITE_FOUR_MAX_ITEM_USES.get();
        if (maximum < 0) {
            return;
        }

        if (instance.getEliteFourItemUses() >= maximum) {
            event.cancel();
            PENDING_USES.remove(player.getUUID());
            showCounter(player, instance);
            return;
        }

        PENDING_USES.remove(player.getUUID());
        recordUses(player, instance, 1);
    }

    public static void tick(ServerPlayer player) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance == null) {
            PENDING_USES.remove(player.getUUID());
            return;
        }

        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        if (battle != null) {
            PENDING_USES.remove(player.getUUID());
            BattleState battleState = BattleState.findFirst(battle);
            if (battleState != null && instance.isEliteFourBattleItemSyncActive()) {
                syncBattleUses(player, battleState);
            }
        } else {
            instance.finishEliteFourBattleItemSync();
            checkPendingUse(player, instance);
        }

        if (Config.ELITE_FOUR_MAX_ITEM_USES.get() >= 0
                && player.tickCount % ACTION_BAR_REFRESH_TICKS == 0) {
            showCounter(player, instance);
        }
    }

    public static void syncBattleUses(ServerPlayer player, BattleState battleState) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance == null
                || !instance.isEliteFourBattleItemSyncActive()
                || Config.ELITE_FOUR_MAX_ITEM_USES.get() < 0
                || battleState.getBattle() == null) {
            return;
        }

        BattleActor actor = battleState.getBattle().getActor(player);
        if (actor == null) {
            return;
        }

        int currentBattleUses = battleState.getState(actor.getUuid()).getItemsUsed();
        int newlyObserved = currentBattleUses - instance.getEliteFourObservedBattleItemUses();
        if (newlyObserved > 0) {
            recordUses(player, instance, newlyObserved);
        }
        instance.setEliteFourObservedBattleItemUses(currentBattleUses);
    }

    public static void showCounter(ServerPlayer player) {
        GymInstance instance = getEliteFourInstance(player);
        if (instance != null) {
            showCounter(player, instance);
        }
    }

    public static void clear(ServerPlayer player) {
        PENDING_USES.remove(player.getUUID());
    }

    private static void checkPendingUse(ServerPlayer player, GymInstance instance) {
        PendingItemUse pending = PENDING_USES.get(player.getUUID());
        if (pending == null) {
            return;
        }

        int currentCount = countItem(player, pending.itemId());
        if (currentCount < pending.initialCount()) {
            int consumed = pending.initialCount() - currentCount;
            PENDING_USES.remove(player.getUUID());
            recordUses(player, instance, consumed);
            return;
        }

        if (pending.ticksRemaining() <= 1) {
            PENDING_USES.remove(player.getUUID());
        } else {
            PENDING_USES.put(player.getUUID(), pending.tickDown());
        }
    }

    private static void recordUses(ServerPlayer player, GymInstance instance, int requested) {
        int maximum = Config.ELITE_FOUR_MAX_ITEM_USES.get();
        int amount = maximum < 0
                ? requested
                : Math.min(requested, Math.max(0, maximum - instance.getEliteFourItemUses()));
        instance.addEliteFourItemUses(amount);
        showCounter(player, instance);
    }

    private static int countItem(ServerPlayer player, ResourceLocation itemId) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static GymInstance getEliteFourInstance(ServerPlayer player) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        return instance != null && EliteFourStructure.GYM_TYPE.equals(instance.getGymType()) ? instance : null;
    }

    private static void showCounter(ServerPlayer player, GymInstance instance) {
        int maximum = Config.ELITE_FOUR_MAX_ITEM_USES.get();
        if (maximum < 0) {
            return;
        }

        int used = Math.min(instance.getEliteFourItemUses(), maximum);
        ChatFormatting color = usageColor(used, maximum);
        Component amount = Component.literal(used + "/" + maximum).withStyle(color);
        player.sendSystemMessage(
                Component.translatable("message.cobblebash.elite_four_items_used", amount),
                true
        );
    }

    static ChatFormatting usageColor(int used, int maximum) {
        if (maximum <= 0 || (long) used * 5L >= (long) maximum * 4L) {
            return ChatFormatting.RED;
        }
        if ((long) used * 5L >= (long) maximum * 3L) {
            return ChatFormatting.GOLD;
        }
        if ((long) used * 5L >= (long) maximum * 2L) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }

    private record PendingItemUse(ResourceLocation itemId, int initialCount, int ticksRemaining) {
        PendingItemUse tickDown() {
            return new PendingItemUse(itemId, initialCount, ticksRemaining - 1);
        }
    }
}
