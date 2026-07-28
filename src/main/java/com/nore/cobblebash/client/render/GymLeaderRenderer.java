package com.nore.cobblebash.client.render;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.client.model.EliteFourChampionModel;
import com.nore.cobblebash.client.model.EliteFourElectricGroundModel;
import com.nore.cobblebash.client.model.EliteFourFireFairyModel;
import com.nore.cobblebash.client.model.EliteFourGrassGhostModel;
import com.nore.cobblebash.client.model.EliteFourWaterSteelModel;
import com.nore.cobblebash.client.model.GymLeaderFemaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderFemaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderOperativeModel;
import com.nore.cobblebash.entity.GymLeaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GymLeaderRenderer extends MobRenderer<GymLeaderEntity, GymLeaderOperativeModel> {
    private static final String[] VARIANT_PREFIXES = {"male_v1", "male_v2", "female_v1", "female_v2"};
    private static final ResourceLocation[] ELITE_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/entity/elite_four/electric_ground.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/entity/elite_four/fire_fairy.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/entity/elite_four/grass_ghost.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/entity/elite_four/water_steel.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/entity/elite_four/champion.png")
    };
    private static final ResourceLocation[][] TEXTURES = createTextures();

    public GymLeaderRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new GymLeaderOperativeModel(
                        context.bakeLayer(GymLeaderMaleV1Model.LAYER_LOCATION),
                        context.bakeLayer(GymLeaderMaleV2Model.LAYER_LOCATION),
                        context.bakeLayer(GymLeaderFemaleV1Model.LAYER_LOCATION),
                        context.bakeLayer(GymLeaderFemaleV2Model.LAYER_LOCATION),
                        context.bakeLayer(EliteFourElectricGroundModel.LAYER_LOCATION),
                        context.bakeLayer(EliteFourFireFairyModel.LAYER_LOCATION),
                        context.bakeLayer(EliteFourGrassGhostModel.LAYER_LOCATION),
                        context.bakeLayer(EliteFourWaterSteelModel.LAYER_LOCATION),
                        context.bakeLayer(EliteFourChampionModel.LAYER_LOCATION)
                ),
                0.35F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(GymLeaderEntity leader) {
        int modelVariant = leader.modelVariant();
        if (modelVariant >= VARIANT_PREFIXES.length) {
            return ELITE_TEXTURES[Math.min(modelVariant - VARIANT_PREFIXES.length, ELITE_TEXTURES.length - 1)];
        }

        return TEXTURES[modelVariant][leader.textureVariant()];
    }

    private static ResourceLocation[][] createTextures() {
        ResourceLocation[][] textures = new ResourceLocation[VARIANT_PREFIXES.length][GymLeaderEntity.TEXTURE_VARIANT_COUNT];

        for (int model = 0; model < VARIANT_PREFIXES.length; model++) {
            for (int texture = 0; texture < GymLeaderEntity.TEXTURE_VARIANT_COUNT; texture++) {
                int skinTone = texture / 8 + 1;
                int hairVariant = texture % 8 + 1;
                textures[model][texture] = ResourceLocation.fromNamespaceAndPath(
                        CobbleBash.MODID,
                        "textures/entity/gym_leader/" + VARIANT_PREFIXES[model] + "_" + skinTone + "_" + hairVariant + ".png"
                );
            }
        }

        return textures;
    }
}
