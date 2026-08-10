package com.nore.cobblebash;

import com.nore.cobblebash.beacon.ChampionBeaconRenderer;
import com.nore.cobblebash.beacon.ChampionBeaconScreen;
import com.nore.cobblebash.client.model.ChampionRibbonModel;
import com.nore.cobblebash.client.model.EliteFourChampionModel;
import com.nore.cobblebash.client.model.EliteFourElectricGroundModel;
import com.nore.cobblebash.client.model.EliteFourFireFairyModel;
import com.nore.cobblebash.client.model.EliteFourGrassGhostModel;
import com.nore.cobblebash.client.model.EliteFourWaterSteelModel;
import com.nore.cobblebash.client.model.GymLeaderFemaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderFemaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV2Model;
import com.nore.cobblebash.client.model.LeagueRepresentativeVillagerModel;
import com.nore.cobblebash.client.render.GymLeaderRenderer;
import com.nore.cobblebash.client.render.GymTrainerRenderer;
import com.nore.cobblebash.client.model.TrainerRibbonModel;
import com.nore.cobblebash.client.render.LeagueRepresentativeVillagerRenderer;
import com.nore.cobblebash.client.render.RibbonCurioRenderer;
import com.nore.cobblebash.client.tooltip.ClientRibbonTooltipComponent;
import com.nore.cobblebash.client.tooltip.RibbonTooltipComponent;
import com.nore.cobblebash.elitefour.EliteFourChampionBeamRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CobbleBash.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = CobbleBash.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CobbleBashClient {
    private static final ResourceLocation TRAINER_RIBBON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleBash.MODID,
            "textures/item/trainer_ribbon.png"
    );
    private static final ResourceLocation CHAMPION_RIBBON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleBash.MODID,
            "textures/item/champion_ribbon.png"
    );

    public CobbleBashClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BlockEntityRenderers.register(
                CobbleBash.CHAMPION_BEACON_BLOCK_ENTITY.get(),
                ChampionBeaconRenderer::new
        ));
        event.enqueueWork(() -> BlockEntityRenderers.register(
                CobbleBash.ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY.get(),
                EliteFourChampionBeamRenderer::new
        ));
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(
                    CobbleBash.TRAINER_RIBBON.get(),
                    () -> new RibbonCurioRenderer(
                            new TrainerRibbonModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(TrainerRibbonModel.LAYER_LOCATION)),
                            TRAINER_RIBBON_TEXTURE
                    )
            );
            CuriosRendererRegistry.register(
                    CobbleBash.CHAMPION_RIBBON.get(),
                    () -> new RibbonCurioRenderer(
                            new ChampionRibbonModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ChampionRibbonModel.LAYER_LOCATION)),
                            CHAMPION_RIBBON_TEXTURE
                    )
            );
        });
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TrainerRibbonModel.LAYER_LOCATION, TrainerRibbonModel::createBodyLayer);
        event.registerLayerDefinition(ChampionRibbonModel.LAYER_LOCATION, ChampionRibbonModel::createBodyLayer);
        event.registerLayerDefinition(GymTrainerMaleV1Model.LAYER_LOCATION, GymTrainerMaleV1Model::createBodyLayer);
        event.registerLayerDefinition(GymTrainerMaleV2Model.LAYER_LOCATION, GymTrainerMaleV2Model::createBodyLayer);
        event.registerLayerDefinition(GymTrainerFemaleV1Model.LAYER_LOCATION, GymTrainerFemaleV1Model::createBodyLayer);
        event.registerLayerDefinition(GymTrainerFemaleV2Model.LAYER_LOCATION, GymTrainerFemaleV2Model::createBodyLayer);
        event.registerLayerDefinition(GymLeaderMaleV1Model.LAYER_LOCATION, GymLeaderMaleV1Model::createBodyLayer);
        event.registerLayerDefinition(GymLeaderMaleV2Model.LAYER_LOCATION, GymLeaderMaleV2Model::createBodyLayer);
        event.registerLayerDefinition(GymLeaderFemaleV1Model.LAYER_LOCATION, GymLeaderFemaleV1Model::createBodyLayer);
        event.registerLayerDefinition(GymLeaderFemaleV2Model.LAYER_LOCATION, GymLeaderFemaleV2Model::createBodyLayer);
        event.registerLayerDefinition(EliteFourElectricGroundModel.LAYER_LOCATION, EliteFourElectricGroundModel::createBodyLayer);
        event.registerLayerDefinition(EliteFourFireFairyModel.LAYER_LOCATION, EliteFourFireFairyModel::createBodyLayer);
        event.registerLayerDefinition(EliteFourGrassGhostModel.LAYER_LOCATION, EliteFourGrassGhostModel::createBodyLayer);
        event.registerLayerDefinition(EliteFourWaterSteelModel.LAYER_LOCATION, EliteFourWaterSteelModel::createBodyLayer);
        event.registerLayerDefinition(EliteFourChampionModel.LAYER_LOCATION, EliteFourChampionModel::createBodyLayer);
        event.registerLayerDefinition(
                LeagueRepresentativeVillagerModel.LAYER_LOCATION,
                LeagueRepresentativeVillagerModel::createBodyLayer
        );
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.VILLAGER, LeagueRepresentativeVillagerRenderer::new);
        event.registerEntityRenderer(CobbleBash.GYM_TRAINER.get(), GymTrainerRenderer::new);
        event.registerEntityRenderer(CobbleBash.GYM_LEADER.get(), GymLeaderRenderer::new);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CobbleBash.CHAMPION_BEACON_MENU.get(), ChampionBeaconScreen::new);
    }

    @SubscribeEvent
    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RibbonTooltipComponent.class, ClientRibbonTooltipComponent::new);
    }
}
