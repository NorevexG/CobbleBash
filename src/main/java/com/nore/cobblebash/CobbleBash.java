package com.nore.cobblebash;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.nore.cobblebash.beacon.ChampionBeaconBlock;
import com.nore.cobblebash.beacon.ChampionBeaconBlockEntity;
import com.nore.cobblebash.beacon.ChampionBeaconMenu;
import com.nore.cobblebash.block.EliteFourChampionBeamBlock;
import com.nore.cobblebash.block.EliteFourPlaqueBlock;
import com.nore.cobblebash.block.TrainingSimulatorBlock;
import com.nore.cobblebash.elitefour.EliteFourChampionBeamBlockEntity;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.entity.GymLeaderEntity;
import com.nore.cobblebash.entity.GymTrainerEntity;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.item.ChampionRibbonItem;
import com.nore.cobblebash.item.EliteFourTrainingDiskItem;
import com.nore.cobblebash.item.TrainerRibbonItem;
import com.nore.cobblebash.item.TrainingDiskItem;
import com.google.common.collect.ImmutableSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.event.GymEventHandler;
import com.nore.cobblebash.event.LeagueRepresentativeEvents;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.stats.CobbleBashStats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MODID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, MODID);
    private static final SoundType CHAMPION_BEACON_SOUND = new SoundType(
            1.0F,
            1.0F,
            SoundEvents.BEACON_DEACTIVATE,
            SoundEvents.GLASS_STEP,
            SoundEvents.GLASS_PLACE,
            SoundEvents.GLASS_HIT,
            SoundEvents.GLASS_FALL
    );

    public static final ResourceKey<PoiType> LEAGUE_REPRESENTATIVE_POI_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "league_representative")
    );

    public static final DeferredBlock<TrainingSimulatorBlock> TRAINING_SIMULATOR = BLOCKS.register(
            "training_simulator",
            () -> new TrainingSimulatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .noOcclusion())
    );
    public static final DeferredItem<BlockItem> TRAINING_SIMULATOR_ITEM = ITEMS.registerSimpleBlockItem(
            "training_simulator",
            TRAINING_SIMULATOR
    );
    public static final DeferredBlock<ChampionBeaconBlock> CHAMPION_BEACON = BLOCKS.register(
            "champion_beacon",
            () -> new ChampionBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .strength(3.0F)
                    .sound(CHAMPION_BEACON_SOUND)
                    .lightLevel(state -> 15)
                    .noOcclusion())
    );
    public static final DeferredItem<BlockItem> CHAMPION_BEACON_ITEM = ITEMS.registerSimpleBlockItem(
            "champion_beacon",
            CHAMPION_BEACON
    );
    public static final DeferredBlock<EliteFourChampionBeamBlock> ELITE_FOUR_CHAMPION_BEAM = BLOCKS.register(
            "elite_four_champion_beam",
            () -> new EliteFourChampionBeamBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.0F)
                    .noCollission()
                    .noLootTable()
                    .noOcclusion())
    );
    public static final DeferredBlock<EliteFourPlaqueBlock> ELITE_FOUR_PLAQUE_ELECTRIC_GROUND = registerEliteFourPlaque(
            "elite_four_plaque_electric_ground",
            EliteFourMember.ELECTRIC_GROUND
    );
    public static final DeferredItem<BlockItem> ELITE_FOUR_PLAQUE_ELECTRIC_GROUND_ITEM = ITEMS.registerSimpleBlockItem(
            "elite_four_plaque_electric_ground",
            ELITE_FOUR_PLAQUE_ELECTRIC_GROUND
    );
    public static final DeferredBlock<EliteFourPlaqueBlock> ELITE_FOUR_PLAQUE_FIRE_FAIRY = registerEliteFourPlaque(
            "elite_four_plaque_fire_fairy",
            EliteFourMember.FIRE_FAIRY
    );
    public static final DeferredItem<BlockItem> ELITE_FOUR_PLAQUE_FIRE_FAIRY_ITEM = ITEMS.registerSimpleBlockItem(
            "elite_four_plaque_fire_fairy",
            ELITE_FOUR_PLAQUE_FIRE_FAIRY
    );
    public static final DeferredBlock<EliteFourPlaqueBlock> ELITE_FOUR_PLAQUE_GRASS_GHOST = registerEliteFourPlaque(
            "elite_four_plaque_grass_ghost",
            EliteFourMember.GRASS_GHOST
    );
    public static final DeferredItem<BlockItem> ELITE_FOUR_PLAQUE_GRASS_GHOST_ITEM = ITEMS.registerSimpleBlockItem(
            "elite_four_plaque_grass_ghost",
            ELITE_FOUR_PLAQUE_GRASS_GHOST
    );
    public static final DeferredBlock<EliteFourPlaqueBlock> ELITE_FOUR_PLAQUE_WATER_STEEL = registerEliteFourPlaque(
            "elite_four_plaque_water_steel",
            EliteFourMember.WATER_STEEL
    );
    public static final DeferredItem<BlockItem> ELITE_FOUR_PLAQUE_WATER_STEEL_ITEM = ITEMS.registerSimpleBlockItem(
            "elite_four_plaque_water_steel",
            ELITE_FOUR_PLAQUE_WATER_STEEL
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChampionBeaconBlockEntity>> CHAMPION_BEACON_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "champion_beacon",
                    () -> BlockEntityType.Builder.of(ChampionBeaconBlockEntity::new, CHAMPION_BEACON.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EliteFourChampionBeamBlockEntity>> ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "elite_four_champion_beam",
                    () -> BlockEntityType.Builder.of(EliteFourChampionBeamBlockEntity::new, ELITE_FOUR_CHAMPION_BEAM.get()).build(null)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<ChampionBeaconMenu>> CHAMPION_BEACON_MENU = MENUS.register(
            "champion_beacon",
            () -> new MenuType<>(ChampionBeaconMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
    public static final DeferredItem<ChampionRibbonItem> CHAMPION_RIBBON = ITEMS.register(
            "champion_ribbon",
            () -> new ChampionRibbonItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<TrainerRibbonItem> TRAINER_RIBBON = ITEMS.register(
            "trainer_ribbon",
            () -> new TrainerRibbonItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<EliteFourTrainingDiskItem> ELITE_FOUR_TRAINING_DISK = ITEMS.register(
            "elite_four_training_disk",
            () -> new EliteFourTrainingDiskItem(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<SmithingTemplateItem> CHAMPION_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "champion_upgrade_smithing_template",
            CobbleBash::createChampionUpgradeTemplate
    );
    public static final DeferredHolder<PoiType, PoiType> LEAGUE_REPRESENTATIVE_POI = POI_TYPES.register(
            "league_representative",
            () -> new PoiType(ImmutableSet.copyOf(TRAINING_SIMULATOR.get().getStateDefinition().getPossibleStates()), 1, 1)
    );
    public static final DeferredHolder<VillagerProfession, VillagerProfession> LEAGUE_REPRESENTATIVE = VILLAGER_PROFESSIONS.register(
            "league_representative",
            () -> new VillagerProfession(
                    MODID + ":league_representative",
                    holder -> holder.is(LEAGUE_REPRESENTATIVE_POI_KEY),
                    holder -> holder.is(LEAGUE_REPRESENTATIVE_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER
            )
    );
    public static final DeferredHolder<EntityType<?>, EntityType<GymTrainerEntity>> GYM_TRAINER = ENTITY_TYPES.register(
            "gym_trainer",
            () -> EntityType.Builder.of(GymTrainerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(MODID + ":gym_trainer")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<GymLeaderEntity>> GYM_LEADER = ENTITY_TYPES.register(
            "gym_leader",
            () -> EntityType.Builder.of(GymLeaderEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(MODID + ":gym_leader")
    );
    public static final Map<GymType, DeferredItem<TrainingDiskItem>> TRAINING_DISKS = registerTrainingDisks();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLEBASH_TAB = CREATIVE_MODE_TABS.register("cobblebash", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cobblebash"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> TRAINING_SIMULATOR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TRAINING_SIMULATOR_ITEM.get());
                output.accept(CHAMPION_BEACON_ITEM.get());
                output.accept(CHAMPION_UPGRADE_SMITHING_TEMPLATE.get());
                output.accept(TRAINER_RIBBON.get());
                output.accept(CHAMPION_RIBBON.get());
                output.accept(ELITE_FOUR_TRAINING_DISK.get());
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
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        POI_TYPES.register(modEventBus);
        VILLAGER_PROFESSIONS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        CobbleBashCriteriaTriggers.TRIGGERS.register(modEventBus);
        CobbleBashStats.CUSTOM_STATS.register(modEventBus);
        modEventBus.addListener(CobbleBash::registerEntityAttributes);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (CobbleBash) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(new DelayedTaskScheduler());
        NeoForge.EVENT_BUS.register(new GymEventHandler());
        NeoForge.EVENT_BUS.register(new LeagueRepresentativeEvents());

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

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(GYM_TRAINER.get(), GymTrainerEntity.createAttributes().build());
        event.put(GYM_LEADER.get(), GymLeaderEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TRAINING_SIMULATOR_ITEM);
            event.accept(CHAMPION_BEACON_ITEM);
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

    private static DeferredBlock<EliteFourPlaqueBlock> registerEliteFourPlaque(String id, EliteFourMember member) {
        return BLOCKS.register(
                id,
                () -> new EliteFourPlaqueBlock(member, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(-1.0F, 3600000.0F)
                        .noLootTable()
                        .noOcclusion())
        );
    }

    private static SmithingTemplateItem createChampionUpgradeTemplate() {
        return new SmithingTemplateItem(
                Component.translatable("item.cobblebash.smithing_template.champion_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.cobblebash.smithing_template.champion_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("upgrade.cobblebash.champion_upgrade").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.cobblebash.smithing_template.champion_upgrade.base_slot_description"),
                Component.translatable("item.cobblebash.smithing_template.champion_upgrade.additions_slot_description"),
                List.of(
                        ResourceLocation.withDefaultNamespace("item/empty_slot_smithing_template_netherite_upgrade"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")
                ),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"))
        );
    }
}
