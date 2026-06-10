package com.nore.cobblebash;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.nore.cobblebash.block.TrainingSimulatorBlock;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.item.TrainingDiskItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.event.GymEventHandler;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.stats.CobbleBashStats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CobbleBash.MODID)
public class CobbleBash {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "cobblebash";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "cobblebash" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "cobblebash" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "cobblebash" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<TrainingSimulatorBlock> TRAINING_SIMULATOR = BLOCKS.register(
            "training_simulator",
            () -> new TrainingSimulatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F))
    );
    public static final DeferredItem<BlockItem> TRAINING_SIMULATOR_ITEM = ITEMS.registerSimpleBlockItem(
            "training_simulator",
            TRAINING_SIMULATOR
    );
    public static final Map<GymType, DeferredItem<TrainingDiskItem>> TRAINING_DISKS = registerTrainingDisks();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLEBASH_TAB = CREATIVE_MODE_TABS.register("cobblebash", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cobblebash"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> TRAINING_SIMULATOR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TRAINING_SIMULATOR_ITEM.get());
                TRAINING_DISKS.values().forEach(disk -> output.accept(disk.get()));
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CobbleBash(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        CobbleBashCriteriaTriggers.TRIGGERS.register(modEventBus);
        CobbleBashStats.CUSTOM_STATS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (CobbleBash) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(new DelayedTaskScheduler());
        NeoForge.EVENT_BUS.register(new GymEventHandler());

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        RctApiProbe.logLoaded();
        GymEventHandler.registerRctListeners();
        event.enqueueWork(CobbleBashStats::bootstrap);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TRAINING_SIMULATOR_ITEM);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GymCommand.register(event.getDispatcher());
    }

    private static Map<GymType, DeferredItem<TrainingDiskItem>> registerTrainingDisks() {
        Map<GymType, DeferredItem<TrainingDiskItem>> disks = new EnumMap<>(GymType.class);
        for (GymType type : GymType.values()) {
            disks.put(type, ITEMS.register(
                    type.getId() + "_training_disk",
                    () -> new TrainingDiskItem(type, new Item.Properties().stacksTo(16))
            ));
        }
        return Collections.unmodifiableMap(disks);
    }
}
