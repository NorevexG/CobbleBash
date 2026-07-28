package com.nore.cobblebash.structure;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.entity.GymLeaderEntity;
import com.nore.cobblebash.entity.GymTrainerEntity;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class GymPlatformBuilder {
    private static final String TRAINER_ENTITY_TAG = "cobblebash_rct_trainer";
    private static final int PLACE_STRUCTURE_FLAGS = Block.UPDATE_ALL;
    private static final int PRESERVE_CONNECTION_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int STRUCTURE_CLEANUP_PADDING = 8;
    private static final int ELITE_FOUR_ELECTRIC_GROUND_MODEL = 4;
    private static final int ELITE_FOUR_FIRE_FAIRY_MODEL = 5;
    private static final int ELITE_FOUR_GRASS_GHOST_MODEL = 6;
    private static final int ELITE_FOUR_WATER_STEEL_MODEL = 7;
    private static final int ELITE_FOUR_CHAMPION_MODEL = 8;
    private static final String[] MALE_TRAINER_NAMES = {
            "Aiden", "Ben", "Caleb", "Dante", "Eli", "Felix", "Grant", "Hugo",
            "Ivan", "Jasper", "Kai", "Leo", "Miles", "Nolan", "Owen", "Theo"
    };
    private static final String[] FEMALE_TRAINER_NAMES = {
            "Ava", "Bianca", "Clara", "Daphne", "Elena", "Freya", "Gwen", "Iris",
            "Jade", "Kira", "Lena", "Maya", "Nora", "Piper", "Rhea", "Talia"
    };

    private record TrainerVisual(int modelVariant, int textureVariant, String displayName) {
    }

    private record GymVisualPlan(TrainerVisual trainerOne, TrainerVisual trainerTwo, TrainerVisual boss) {
    }

    private record TrainerCleanupResult(LivingEntity keeper, int removed) {
    }

    public record TrainerEntityDebug(String trainerIdPart, String trainerId, int total, int exactTagged, int nearbyDisplays, List<String> entries) {
    }

    public static void buildTestPlatform(ServerLevel level, BlockPos origin) {
        buildTestPlatform(level, origin, "bug", 0, new int[]{10, 12, 14});
    }

    public static void buildTestPlatform(ServerLevel level, BlockPos origin, int[] trainerLevels) {
        buildTestPlatform(level, origin, "bug", 0, trainerLevels);
    }

    public static void buildGym(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            buildStructureGym(level, origin, definition, slotId, trainerLevels);
            return;
        }

        buildTestPlatform(level, origin, gymType, slotId, trainerLevels);
    }

    public static void buildTestPlatform(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
        clearTestPlatform(level, origin);

        int blockY = origin.getY() - 1;

        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z < 12; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 1), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 4), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 7), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 10), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);

        GymDoorController.buildClosedTestDoors(level, origin);
        GymVisualPlan visualPlan = createGymVisualPlan(level, gymType, slotId);
        spawnTrainer(level, origin, gymType, slotId, "trainer_1", trainerLevels[0], origin.offset(0, 0, 4), visualPlan.trainerOne());
        spawnTrainer(level, origin, gymType, slotId, "trainer_2", trainerLevels[1], origin.offset(0, 0, 7), visualPlan.trainerTwo());
        spawnTrainer(level, origin, gymType, slotId, "boss", trainerLevels[2], origin.offset(0, 0, 10), visualPlan.boss());
    }

    public static void clearGym(ServerLevel level, BlockPos origin, String gymType) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            clearStructureGym(level, origin, definition);
            return;
        }

        clearTestPlatform(level, origin);
    }

    public static void clearTestPlatform(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, origin);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = 0; z < 12; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        clearSlotEntities(level, origin);
    }

    public static BlockPos getPlayerSpawn(BlockPos origin, String gymType) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            return origin.offset(definition.playerSpawnOffset());
        }

        return origin.offset(0, 0, 1);
    }

    public static float getPlayerSpawnYaw(String gymType, float fallbackYaw) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        return definition == null ? fallbackYaw : definition.playerYaw();
    }

    public static float getPlayerSpawnPitch(String gymType, float fallbackPitch) {
        return GymStructureDefinition.get(gymType) == null ? fallbackPitch : 0.0F;
    }

    public static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
        return attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, null);
    }

    private static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos) {
        return attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, null);
    }

    private static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos, LivingEntity preferredEntity) {
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
        if (trainer == null) return false;

        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        LivingEntity entity = pos == null
                ? keepSingleTrainerEntity(findTrainerEntities(level, origin, trainerId))
                : cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, preferredEntity).keeper();
        if (entity == null) return false;

        trainer.setEntity(entity);
        return true;
    }

    private static void buildStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition, int slotId, int[] trainerLevels) {
        StructureTemplate template = getStructureTemplate(level, definition);
        if (template == null) {
            CobbleBash.LOGGER.warn(
                    "Could not build {} gym because structure template {} was not found.",
                    definition.gymType(),
                    definition.templateId()
            );
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true);
        clearStructureGym(level, origin, definition);
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), PLACE_STRUCTURE_FLAGS);
        restoreSavedConnectionStates(level, origin, settings, template);
        restoreDecorativeEntities(level, origin, settings, template);
        paintStructureBiome(level, origin, definition);

        BlockPos playerSpawn = getPlayerSpawn(origin, definition.gymType());
        GymVisualPlan visualPlan = createGymVisualPlan(level, definition.gymType(), slotId);
        boolean trainerOneSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "trainer_1", trainerLevels[0],
                playerSpawn.offset(definition.trainerOneOffset()), definition.trainerOneYaw(), visualPlan.trainerOne());
        boolean trainerTwoSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "trainer_2", trainerLevels[1],
                playerSpawn.offset(definition.trainerTwoOffset()), definition.trainerTwoYaw(), visualPlan.trainerTwo());
        boolean bossSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "boss", trainerLevels[2],
                playerSpawn.offset(definition.bossOffset()), definition.bossYaw(), visualPlan.boss());

        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_1", trainerOneSpawned);
        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_2", trainerTwoSpawned);
        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "boss", bossSpawned);
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "trainer_1", trainerLevels[0],
                playerSpawn.offset(definition.trainerOneOffset()), definition.trainerOneYaw(), visualPlan.trainerOne());
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "trainer_2", trainerLevels[1],
                playerSpawn.offset(definition.trainerTwoOffset()), definition.trainerTwoYaw(), visualPlan.trainerTwo());
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "boss", trainerLevels[2],
                playerSpawn.offset(definition.bossOffset()), definition.bossYaw(), visualPlan.boss());
    }

    private static void clearStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        AABB cleanupBox = getStructureCleanupBox(level, origin, definition);
        clearSlotEntities(level, cleanupBox);
        clearDroppedItems(level, cleanupBox);
        clearSlotEntities(level, cleanupBox);
        clearDroppedItems(level, cleanupBox);
    }

    private static StructureTemplate getStructureTemplate(ServerLevel level, GymStructureDefinition definition) {
        return level.getStructureManager().get(definition.templateId()).orElse(null);
    }

    private static void paintStructureBiome(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        Holder<Biome> biome = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(definition.biomeKey());
        AABB box = getStructureCleanupBox(level, origin, definition);

        int minChunkX = SectionPos.blockToSectionCoord(BlockPos.containing(box.minX, box.minY, box.minZ).getX());
        int maxChunkX = SectionPos.blockToSectionCoord(BlockPos.containing(box.maxX, box.maxY, box.maxZ).getX());
        int minChunkZ = SectionPos.blockToSectionCoord(BlockPos.containing(box.minX, box.minY, box.minZ).getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(BlockPos.containing(box.maxX, box.maxY, box.maxZ).getZ());
        List<ChunkAccess> changedChunks = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, null);
                chunk.setUnsaved(true);
                changedChunks.add(chunk);
            }
        }

        if (!changedChunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(changedChunks);
        }
    }

    private static List<ChunkPos> getChunks(AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

        int minChunkX = SectionPos.blockToSectionCoord(min.getX());
        int maxChunkX = SectionPos.blockToSectionCoord(max.getX());
        int minChunkZ = SectionPos.blockToSectionCoord(min.getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(max.getZ());
        List<ChunkPos> chunks = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    private static void restoreSavedConnectionStates(
            ServerLevel level,
            BlockPos origin,
            StructurePlaceSettings settings,
            StructureTemplate template
    ) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!isSavedStateBlock(block)) {
                continue;
            }

            for (StructureTemplate.StructureBlockInfo blockInfo : template.filterBlocks(origin, settings, block)) {
                BlockPos pos = blockInfo.pos();
                BlockState currentState = level.getBlockState(pos);
                if (currentState.is(block) && !currentState.equals(blockInfo.state())) {
                    level.setBlock(pos, blockInfo.state(), PRESERVE_CONNECTION_FLAGS);
                }
            }
        }
    }

    private static boolean isSavedStateBlock(Block block) {
        return block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof IronBarsBlock
                || block instanceof SlabBlock
                || block instanceof StairBlock
                || block instanceof WallBlock;
    }

    private static void restoreDecorativeEntities(
            ServerLevel level,
            BlockPos origin,
            StructurePlaceSettings settings,
            StructureTemplate template
    ) {
        List<StructureTemplate.StructureEntityInfo> entityInfos = StructureTemplate.processEntityInfos(
                template,
                level,
                origin,
                settings,
                getTemplateEntityInfos(template)
        );

        for (StructureTemplate.StructureEntityInfo entityInfo : entityInfos) {
            if (settings.getBoundingBox() != null && !settings.getBoundingBox().isInside(entityInfo.blockPos)) {
                continue;
            }

            CompoundTag tag = entityInfo.nbt.copy();
            if (!isRestorableDecorativeEntityId(tag)) {
                continue;
            }

            ListTag posTag = new ListTag();
            posTag.add(DoubleTag.valueOf(entityInfo.pos.x));
            posTag.add(DoubleTag.valueOf(entityInfo.pos.y));
            posTag.add(DoubleTag.valueOf(entityInfo.pos.z));
            tag.put("Pos", posTag);
            tag.remove("UUID");

            EntityType.create(tag, level).ifPresent(entity -> {
                if (!isRestorableDecorativeEntity(entity)) {
                    return;
                }

                float yaw = entity.rotate(settings.getRotation());
                yaw += entity.mirror(settings.getMirror()) - entity.getYRot();
                entity.moveTo(entityInfo.pos.x, entityInfo.pos.y, entityInfo.pos.z, yaw, entity.getXRot());
                level.addFreshEntityWithPassengers(entity);
            });
        }
    }

    private static List<StructureTemplate.StructureEntityInfo> getTemplateEntityInfos(StructureTemplate template) {
        CompoundTag savedTemplate = template.save(new CompoundTag());
        ListTag entityTags = savedTemplate.getList("entities", Tag.TAG_COMPOUND);
        List<StructureTemplate.StructureEntityInfo> entityInfos = new ArrayList<>();

        for (int i = 0; i < entityTags.size(); i++) {
            CompoundTag entityInfoTag = entityTags.getCompound(i);
            if (!entityInfoTag.contains("nbt")) {
                continue;
            }

            ListTag posTag = entityInfoTag.getList("pos", Tag.TAG_DOUBLE);
            ListTag blockPosTag = entityInfoTag.getList("blockPos", Tag.TAG_INT);
            Vec3 pos = new Vec3(posTag.getDouble(0), posTag.getDouble(1), posTag.getDouble(2));
            BlockPos blockPos = new BlockPos(blockPosTag.getInt(0), blockPosTag.getInt(1), blockPosTag.getInt(2));
            entityInfos.add(new StructureTemplate.StructureEntityInfo(pos, blockPos, entityInfoTag.getCompound("nbt")));
        }

        return entityInfos;
    }

    private static boolean isRestorableDecorativeEntityId(CompoundTag tag) {
        String id = tag.getString("id");
        return "minecraft:armor_stand".equals(id)
                || "minecraft:glow_item_frame".equals(id)
                || "minecraft:item_frame".equals(id)
                || "minecraft:painting".equals(id);
    }

    private static boolean isRestorableDecorativeEntity(Entity entity) {
        return entity instanceof ArmorStand
                || entity instanceof GlowItemFrame
                || entity instanceof ItemFrame
                || entity instanceof Painting;
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos) {
        return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, 0.0F);
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, TrainerVisual visual) {
        return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, 0.0F, visual);
    }

    public static boolean spawnTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw) {
        boolean spawned = spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw);
        verifySpawnedTrainer(level, origin, gymType, slotId, trainerIdPart, spawned);
        scheduleTrainerRepair(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw);
        return spawned;
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw) {
        return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, null);
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw, TrainerVisual visual) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        discardTrainerEntities(level, origin, trainerId);
        discardNearbyTrainerDisplays(level, pos);
        CobbleBash.LOGGER.info(
                "Spawning gym trainer {} at {} in {} gym slot {} with level {} and yaw {}.",
                trainerId,
                pos.toShortString(),
                gymType,
                slotId,
                trainerLevel,
                yaw
        );

        String displayName = getTrainerDisplayName(level, gymType, trainerIdPart, visual);
        boolean registered = RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel, displayName);
        if (!registered) {
            CobbleBash.LOGGER.error("RCT trainer registration failed for {}; spawning visible trainer entity anyway.", trainerId);
        }

        Mob entity = createTrainerDisplayEntity(level, gymType, slotId, trainerIdPart, trainerLevel, visual);
        if (entity == null) {
            CobbleBash.LOGGER.error("Skipping entity spawn for {} because trainer entity creation returned null.", trainerId);
            return false;
        }

        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setCustomName(Component.literal(displayName));
        entity.setCustomNameVisible(true);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        entity.setPersistenceRequired();
        entity.setInvulnerable(true);
        entity.addTag(TRAINER_ENTITY_TAG);
        entity.addTag(trainerId);

        if (!level.addFreshEntity(entity)) {
            CobbleBash.LOGGER.error("Failed to add trainer entity {} to the world at {}.", trainerId, pos.toShortString());
            return false;
        }

        boolean attached = registered && attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, entity);
        if (!attached) {
            CobbleBash.LOGGER.error("Spawned trainer entity {} but failed to attach it to the RCT trainer.", trainerId);
            return true;
        }

        scheduleTrainerCleanup(level, origin, gymType, slotId, trainerIdPart, pos);
        CobbleBash.LOGGER.info("Spawned and attached trainer entity {} with entity UUID {}.", trainerId, entity.getUUID());
        return true;
    }

    private static Mob createTrainerDisplayEntity(
            ServerLevel level,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            TrainerVisual visual
    ) {
        if (usesGymTrainerVisual(trainerIdPart)) {
            GymTrainerEntity entity = CobbleBash.GYM_TRAINER.get().create(level);
            if (entity != null) {
                TrainerVisual resolvedVisual = visual != null ? visual : selectGymTrainerVisual(gymType, slotId, trainerIdPart, trainerLevel);
                entity.setVisual(
                        resolvedVisual.modelVariant(),
                        resolvedVisual.textureVariant()
                );
            }
            return entity;
        }

        if (usesGymLeaderVisual(gymType, trainerIdPart)) {
            GymLeaderEntity entity = CobbleBash.GYM_LEADER.get().create(level);
            if (entity != null) {
                TrainerVisual resolvedVisual = visual != null ? visual : selectGymLeaderVisual(gymType, slotId, trainerLevel);
                entity.setVisual(
                        resolvedVisual.modelVariant(),
                        resolvedVisual.textureVariant()
                );
            }
            return entity;
        }

        return EntityType.VILLAGER.create(level);
    }

    private static boolean usesGymTrainerVisual(String trainerIdPart) {
        return "trainer_1".equals(trainerIdPart) || "trainer_2".equals(trainerIdPart);
    }

    private static boolean usesGymLeaderVisual(String gymType, String trainerIdPart) {
        return "boss".equals(trainerIdPart) && !EliteFourStructure.GYM_TYPE.equals(gymType);
    }

    private static GymVisualPlan createGymVisualPlan(ServerLevel level, String gymType, int slotId) {
        List<Integer> models = new ArrayList<>();
        List<String> usedNames = new ArrayList<>();
        int modelCount = Math.min(GymTrainerEntity.MODEL_VARIANT_COUNT, GymLeaderEntity.MODEL_VARIANT_COUNT);
        for (int i = 0; i < modelCount; i++) {
            models.add(i);
        }

        int trainerOneModel = takeRandomModel(level, models);
        int trainerTwoModel = takeRandomModel(level, models);
        int bossModel = takeRandomModel(level, models);
        GymVisualPlan plan = new GymVisualPlan(
                new TrainerVisual(
                        trainerOneModel,
                        level.getRandom().nextInt(GymTrainerEntity.TEXTURE_VARIANT_COUNT),
                        createRolledDisplayName(level, "Trainer", trainerOneModel, usedNames)
                ),
                new TrainerVisual(
                        trainerTwoModel,
                        level.getRandom().nextInt(GymTrainerEntity.TEXTURE_VARIANT_COUNT),
                        createRolledDisplayName(level, "Trainer", trainerTwoModel, usedNames)
                ),
                new TrainerVisual(
                        bossModel,
                        level.getRandom().nextInt(GymLeaderEntity.TEXTURE_VARIANT_COUNT),
                        createRolledDisplayName(level, "Gym Leader", bossModel, usedNames)
                )
        );
        CobbleBash.LOGGER.info(
                "Gym visual roll for {} slot {}: trainer_1={} model {}, texture {}; trainer_2={} model {}, texture {}; boss={} model {}, texture {}.",
                gymType,
                slotId,
                plan.trainerOne().displayName(),
                plan.trainerOne().modelVariant(),
                plan.trainerOne().textureVariant(),
                plan.trainerTwo().displayName(),
                plan.trainerTwo().modelVariant(),
                plan.trainerTwo().textureVariant(),
                plan.boss().displayName(),
                plan.boss().modelVariant(),
                plan.boss().textureVariant()
        );
        return plan;
    }

    private static String getTrainerDisplayName(ServerLevel level, String gymType, String trainerIdPart, TrainerVisual visual) {
        if (visual != null && visual.displayName() != null && !visual.displayName().isBlank()) {
            return visual.displayName();
        }

        return RctApiProbe.getTrainerDisplayName(level.getServer(), gymType, trainerIdPart);
    }

    private static int takeRandomModel(ServerLevel level, List<Integer> models) {
        return models.remove(level.getRandom().nextInt(models.size()));
    }

    private static String createRolledDisplayName(ServerLevel level, String title, int modelVariant, List<String> usedNames) {
        String[] pool = isFemaleTrainerModel(modelVariant) ? FEMALE_TRAINER_NAMES : MALE_TRAINER_NAMES;
        List<String> availableNames = new ArrayList<>();
        for (String name : pool) {
            String displayName = title + " " + name;
            if (!usedNames.contains(displayName)) {
                availableNames.add(name);
            }
        }

        String name = availableNames.isEmpty()
                ? pool[level.getRandom().nextInt(pool.length)]
                : availableNames.get(level.getRandom().nextInt(availableNames.size()));
        String displayName = title + " " + name;
        usedNames.add(displayName);
        return displayName;
    }

    private static boolean isFemaleTrainerModel(int modelVariant) {
        return modelVariant == 2 || modelVariant == 3;
    }

    private static TrainerVisual selectGymTrainerVisual(String gymType, int slotId, String trainerIdPart, int trainerLevel) {
        return new TrainerVisual(
                selectGymTrainerModel(gymType, slotId, trainerIdPart),
                selectGymTrainerTexture(gymType, slotId, trainerIdPart, trainerLevel),
                null
        );
    }

    private static TrainerVisual selectGymLeaderVisual(String gymType, int slotId, int trainerLevel) {
        TrainerVisual eliteFourVisual = selectEliteFourVisual(gymType);
        if (eliteFourVisual != null) {
            return eliteFourVisual;
        }

        return new TrainerVisual(
                selectGymLeaderModel(gymType, slotId),
                selectGymLeaderTexture(gymType, slotId, trainerLevel),
                null
        );
    }

    private static TrainerVisual selectEliteFourVisual(String gymType) {
        if (EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE.equals(gymType)) {
            return new TrainerVisual(ELITE_FOUR_CHAMPION_MODEL, 0, null);
        }

        EliteFourMember member = EliteFourMember.fromTrainerGymType(gymType);
        if (member == null) {
            return null;
        }

        return switch (member) {
            case ELECTRIC_GROUND -> new TrainerVisual(ELITE_FOUR_ELECTRIC_GROUND_MODEL, 0, null);
            case FIRE_FAIRY -> new TrainerVisual(ELITE_FOUR_FIRE_FAIRY_MODEL, 0, null);
            case GRASS_GHOST -> new TrainerVisual(ELITE_FOUR_GRASS_GHOST_MODEL, 0, null);
            case WATER_STEEL -> new TrainerVisual(ELITE_FOUR_WATER_STEEL_MODEL, 0, null);
        };
    }

    private static int selectGymTrainerModel(String gymType, int slotId, String trainerIdPart) {
        int firstModel = Math.floorMod(Objects.hash(gymType, slotId), GymTrainerEntity.MODEL_VARIANT_COUNT);
        if ("trainer_1".equals(trainerIdPart)) {
            return firstModel;
        }

        return (firstModel + 1 + Math.floorMod(Objects.hash(gymType, slotId, trainerIdPart), GymTrainerEntity.MODEL_VARIANT_COUNT - 1))
                % GymTrainerEntity.MODEL_VARIANT_COUNT;
    }

    private static int selectGymTrainerTexture(String gymType, int slotId, String trainerIdPart, int trainerLevel) {
        return Math.floorMod(
                Objects.hash(gymType, slotId, trainerIdPart, trainerLevel),
                GymTrainerEntity.TEXTURE_VARIANT_COUNT
        );
    }

    private static int selectGymLeaderModel(String gymType, int slotId) {
        int trainerOneModel = selectGymTrainerModel(gymType, slotId, "trainer_1");
        int trainerTwoModel = selectGymTrainerModel(gymType, slotId, "trainer_2");
        int firstCandidate = Math.floorMod(Objects.hash(gymType, slotId, "leader"), GymLeaderEntity.MODEL_VARIANT_COUNT);

        for (int offset = 0; offset < GymLeaderEntity.MODEL_VARIANT_COUNT; offset++) {
            int candidate = (firstCandidate + offset) % GymLeaderEntity.MODEL_VARIANT_COUNT;
            if (candidate != trainerOneModel && candidate != trainerTwoModel) {
                return candidate;
            }
        }

        return firstCandidate;
    }

    private static int selectGymLeaderTexture(String gymType, int slotId, int trainerLevel) {
        return Math.floorMod(
                Objects.hash(gymType, slotId, trainerLevel, "leader"),
                GymLeaderEntity.TEXTURE_VARIANT_COUNT
        );
    }

    private static void verifySpawnedTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, boolean spawnResult) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        List<LivingEntity> entities = findTrainerEntities(level, origin, trainerId);
        LivingEntity entity = keepSingleTrainerEntity(entities);
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);

        if (entity == null || trainer == null || trainer.getEntity() == null) {
            CobbleBash.LOGGER.error(
                    "Gym trainer verification failed for {}. spawnResult={}, entityFound={}, rctTrainerFound={}, rctAttached={}.",
                    trainerId,
                    spawnResult,
                    entity != null,
                    trainer != null,
                    trainer != null && trainer.getEntity() != null
            );
            return;
        }

        CobbleBash.LOGGER.info(
                "Gym trainer verification passed for {} at {}.",
                trainerId,
                entity.blockPosition().toShortString()
        );
    }

    private static void scheduleTrainerRepair(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            BlockPos pos,
            float yaw
    ) {
        scheduleTrainerRepair(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, null);
    }

    private static void scheduleTrainerRepair(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            BlockPos pos,
            float yaw,
            TrainerVisual visual
    ) {
        DelayedTaskScheduler.schedule(2, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual));
        DelayedTaskScheduler.schedule(20, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual));
        DelayedTaskScheduler.schedule(60, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
    }

    private static void repairMissingTrainer(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            BlockPos pos,
            float yaw,
            TrainerVisual visual
    ) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
        LivingEntity attachedEntity = trainer == null ? null : trainer.getEntity();
        LivingEntity entity = cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, attachedEntity).keeper();

        if (entity != null && trainer != null && trainer.getEntity() == entity) {
            return;
        }

        CobbleBash.LOGGER.warn(
                "Repairing missing gym trainer {}. entityFound={}, rctTrainerFound={}, rctAttached={}.",
                trainerId,
                entity != null,
                trainer != null,
                trainer != null && trainer.getEntity() != null
        );

        if (entity != null) {
            if (trainer == null && !RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel)) {
                CobbleBash.LOGGER.error("Failed to repair {} RCT attachment because RCT registration failed. Visible entity remains in world.", trainerId);
                return;
            }

            if (!attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, entity)) {
                CobbleBash.LOGGER.error("Failed to repair {} because the existing entity could not be attached.", trainerId);
                return;
            }

            CobbleBash.LOGGER.info("Reattached existing gym trainer entity {}.", trainerId);
            return;
        }

        spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual);
        verifySpawnedTrainer(level, origin, gymType, slotId, trainerIdPart, true);
    }

    private static void clearTrainerEntities(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, getEntityCleanupBox(origin));
    }

    public static List<TrainerEntityDebug> debugTrainerEntities(ServerLevel level, BlockPos origin, String gymType, int slotId) {
        List<TrainerEntityDebug> debug = new ArrayList<>();
        for (String trainerIdPart : new String[]{"trainer_1", "trainer_2", "boss"}) {
            String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
            BlockPos pos = getExpectedTrainerPos(origin, gymType, trainerIdPart);
            List<LivingEntity> exactEntities = findTrainerEntities(level, origin, trainerId);
            List<LivingEntity> nearbyDisplays = findNearbyTrainerDisplays(level, pos);
            List<LivingEntity> allEntities = mergeEntities(exactEntities, nearbyDisplays);
            List<String> entries = allEntities.stream()
                    .map(entity -> describeTrainerEntity(entity, trainerId, pos))
                    .toList();

            debug.add(new TrainerEntityDebug(
                    trainerIdPart,
                    trainerId,
                    allEntities.size(),
                    exactEntities.size(),
                    nearbyDisplays.size(),
                    entries
            ));
        }

        return debug;
    }

    public static int cleanupTrainerEntities(ServerLevel level, BlockPos origin, String gymType, int slotId) {
        int removed = 0;
        for (String trainerIdPart : new String[]{"trainer_1", "trainer_2", "boss"}) {
            TrainerCleanupResult result = cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, getExpectedTrainerPos(origin, gymType, trainerIdPart));
            removed += result.removed();
        }

        return removed;
    }

    public static int discardOneTrainerDisplay(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        BlockPos pos = getExpectedTrainerPos(origin, gymType, trainerIdPart);
        List<LivingEntity> entities = mergeEntities(findTrainerEntities(level, origin, trainerId), findNearbyTrainerDisplays(level, pos));
        if (entities.isEmpty()) {
            return 0;
        }

        LivingEntity target = entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)))
                .orElse(entities.get(0));
        target.discard();
        CobbleBash.LOGGER.warn("Debug discarded one trainer display for {} at {}: {}", trainerId, pos.toShortString(), describeTrainerEntity(target, trainerId, pos));

        return mergeEntities(findTrainerEntities(level, origin, trainerId), findNearbyTrainerDisplays(level, pos)).size();
    }

    public static int discardTrainerDisplays(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        BlockPos pos = getExpectedTrainerPos(origin, gymType, trainerIdPart);
        List<LivingEntity> entities = mergeEntities(findTrainerEntities(level, origin, trainerId), findNearbyTrainerDisplays(level, pos));
        entities.forEach(LivingEntity::discard);
        CobbleBash.LOGGER.warn("Debug discarded {} trainer displays for {} at {}.", entities.size(), trainerId, pos.toShortString());
        return entities.size();
    }

    private static void clearSlotEntities(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, getEntityCleanupBox(origin));
    }

    private static void clearSlotEntities(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class,
                box,
                entity -> !(entity instanceof Player)
        ).forEach(entity -> entity.discard());
    }

    private static void clearDroppedItems(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(ItemEntity.class, box).forEach(entity -> entity.discard());
    }

    private static LivingEntity findTrainerEntity(ServerLevel level, BlockPos origin, String trainerId) {
        return findTrainerEntities(level, origin, trainerId).stream().findFirst().orElse(null);
    }

    private static List<LivingEntity> findTrainerEntities(ServerLevel level, BlockPos origin, String trainerId) {
        AABB box = getTrainerSearchBox(level, origin, trainerId);

        return level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.getTags().contains(trainerId)
        );
    }

    private static LivingEntity keepSingleTrainerEntity(List<LivingEntity> entities) {
        if (entities.isEmpty()) {
            return null;
        }

        LivingEntity keeper = entities.get(0);
        discardDuplicateTrainerEntities(entities, keeper);

        return keeper;
    }

    private static void scheduleTrainerCleanup(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            BlockPos pos
    ) {
        DelayedTaskScheduler.schedule(1, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
        DelayedTaskScheduler.schedule(5, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
        DelayedTaskScheduler.schedule(20, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
        DelayedTaskScheduler.schedule(60, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
    }

    private static TrainerCleanupResult cleanupTrainerStack(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            BlockPos pos
    ) {
        return cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, null);
    }

    private static TrainerCleanupResult cleanupTrainerStack(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            BlockPos pos,
            LivingEntity preferredKeeper
    ) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        int removedWrongDisplays = discardNearbyTrainerDisplaysExcept(level, pos, trainerId);
        List<LivingEntity> entities = findTrainerEntities(level, origin, trainerId);
        LivingEntity keeper = keepTrainerEntity(entities, pos, preferredKeeper);
        int removedDuplicates = Math.max(0, entities.size() - (keeper == null ? 0 : 1));
        return new TrainerCleanupResult(keeper, removedWrongDisplays + removedDuplicates);
    }

    private static LivingEntity keepClosestTrainerEntity(List<LivingEntity> entities, BlockPos pos) {
        return keepTrainerEntity(entities, pos, null);
    }

    private static LivingEntity keepTrainerEntity(List<LivingEntity> entities, BlockPos pos, LivingEntity preferredKeeper) {
        if (entities.isEmpty()) {
            return null;
        }

        LivingEntity keeper = findPreferredTrainerEntity(entities, preferredKeeper);
        if (keeper == null) {
            keeper = entities.stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)))
                    .orElse(entities.get(0));
        }

        discardDuplicateTrainerEntities(entities, keeper);

        return keeper;
    }

    private static LivingEntity findPreferredTrainerEntity(List<LivingEntity> entities, LivingEntity preferredKeeper) {
        if (preferredKeeper == null || preferredKeeper.isRemoved()) {
            return null;
        }

        for (LivingEntity entity : entities) {
            if (entity.getUUID().equals(preferredKeeper.getUUID())) {
                return entity;
            }
        }

        return null;
    }

    private static void discardDuplicateTrainerEntities(List<LivingEntity> entities, LivingEntity keeper) {
        int discarded = 0;
        for (int i = 0; i < entities.size(); i++) {
            LivingEntity entity = entities.get(i);
            if (entity == keeper) {
                continue;
            }

            entity.discard();
            discarded++;
        }

        if (discarded > 0) {
            CobbleBash.LOGGER.warn("Discarded {} duplicate CobbleBash trainer display entities.", discarded);
        }
    }

    private static void discardTrainerEntities(ServerLevel level, BlockPos origin, String trainerId) {
        findTrainerEntities(level, origin, trainerId).forEach(LivingEntity::discard);
    }

    private static void discardNearbyTrainerDisplays(ServerLevel level, BlockPos pos) {
        AABB box = getTrainerSpawnBox(pos);
        level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                GymPlatformBuilder::isTrainerDisplayEntity
        ).forEach(LivingEntity::discard);
    }

    private static int discardNearbyTrainerDisplaysExcept(ServerLevel level, BlockPos pos, String trainerId) {
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                getTrainerSpawnBox(pos),
                entity -> isTrainerDisplayEntity(entity)
                        && !entity.getTags().contains(trainerId)
        );

        entities.forEach(LivingEntity::discard);

        if (!entities.isEmpty()) {
            CobbleBash.LOGGER.warn(
                    "Discarded {} stale CobbleBash trainer display entities near {} while keeping {}.",
                    entities.size(),
                    pos.toShortString(),
                    trainerId
            );
        }

        return entities.size();
    }

    private static List<LivingEntity> findNearbyTrainerDisplays(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(
                LivingEntity.class,
                getTrainerSpawnBox(pos),
                GymPlatformBuilder::isTrainerDisplayEntity
        );
    }

    private static boolean isTrainerDisplayEntity(LivingEntity entity) {
        return entity instanceof GymTrainerEntity
                || entity.getTags().contains(TRAINER_ENTITY_TAG);
    }

    private static List<LivingEntity> mergeEntities(List<LivingEntity> first, List<LivingEntity> second) {
        List<LivingEntity> merged = new ArrayList<>(first);
        for (LivingEntity entity : second) {
            boolean alreadyPresent = false;
            for (LivingEntity existing : merged) {
                if (existing.getUUID().equals(entity.getUUID())) {
                    alreadyPresent = true;
                    break;
                }
            }

            if (!alreadyPresent) {
                merged.add(entity);
            }
        }

        return merged;
    }

    private static String describeTrainerEntity(LivingEntity entity, String trainerId, BlockPos expectedPos) {
        String visual = entity instanceof GymTrainerEntity trainer
                ? ", model=" + trainer.modelVariant() + ", texture=" + trainer.textureVariant()
                : "";
        String exactTag = entity.getTags().contains(trainerId) ? ", exactTag=true" : ", exactTag=false";

        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                + " uuid="
                + entity.getUUID()
                + " pos=("
                + String.format(java.util.Locale.ROOT, "%.2f", entity.getX())
                + ", "
                + String.format(java.util.Locale.ROOT, "%.2f", entity.getY())
                + ", "
                + String.format(java.util.Locale.ROOT, "%.2f", entity.getZ())
                + ") d2="
                + String.format(java.util.Locale.ROOT, "%.3f", entity.distanceToSqr(expectedPos.getX() + 0.5D, expectedPos.getY(), expectedPos.getZ() + 0.5D))
                + exactTag
                + visual;
    }

    private static BlockPos getExpectedTrainerPos(BlockPos origin, String gymType, String trainerIdPart) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            BlockPos playerSpawn = getPlayerSpawn(origin, gymType);
            if ("trainer_1".equals(trainerIdPart)) {
                return playerSpawn.offset(definition.trainerOneOffset());
            }

            if ("trainer_2".equals(trainerIdPart)) {
                return playerSpawn.offset(definition.trainerTwoOffset());
            }

            return playerSpawn.offset(definition.bossOffset());
        }

        if ("trainer_1".equals(trainerIdPart)) {
            return origin.offset(0, 0, 4);
        }

        if ("trainer_2".equals(trainerIdPart)) {
            return origin.offset(0, 0, 7);
        }

        return origin.offset(0, 0, 10);
    }

    private static AABB getTrainerSpawnBox(BlockPos pos) {
        return new AABB(
                pos.getX() - 1.25D,
                pos.getY() - 0.5D,
                pos.getZ() - 1.25D,
                pos.getX() + 2.25D,
                pos.getY() + 3.0D,
                pos.getZ() + 2.25D
        );
    }

    private static AABB getEntityCleanupBox(BlockPos origin) {
        return new AABB(
                origin.getX() - 16,
                origin.getY() - 8,
                origin.getZ() - 8,
                origin.getX() + 16,
                origin.getY() + 12,
                origin.getZ() + 24
        );
    }

    private static AABB getTrainerSearchBox(ServerLevel level, BlockPos origin, String trainerId) {
        if (trainerId.startsWith("cobblebash_elite4_")) {
            return EliteFourStructure.getStructureBox(level, origin);
        }

        for (GymStructureDefinition definition : GymStructureDefinition.values()) {
            if (trainerId.startsWith("cobblebash_" + definition.gymType() + "_slot_")) {
                return getStructureCleanupBox(level, origin, definition);
            }
        }

        return getEntityCleanupBox(origin);
    }

    private static AABB getStructureCleanupBox(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        BlockPos min = origin.offset(-STRUCTURE_CLEANUP_PADDING, -STRUCTURE_CLEANUP_PADDING, -STRUCTURE_CLEANUP_PADDING);
        BlockPos max = origin.offset(STRUCTURE_CLEANUP_PADDING, STRUCTURE_CLEANUP_PADDING, STRUCTURE_CLEANUP_PADDING);

        StructureTemplate template = getStructureTemplate(level, definition);
        if (template != null) {
            BlockPos size = new BlockPos(template.getSize());
            max = max(max, origin.offset(size.getX(), size.getY(), size.getZ()));
        }

        min = min(min, origin.offset(definition.playerSpawnOffset()));
        max = max(max, origin.offset(definition.playerSpawnOffset()));

        min = includePlayerRelativeMin(origin, definition, min, definition.trainerOneOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.trainerOneOffset());
        min = includePlayerRelativeMin(origin, definition, min, definition.trainerTwoOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.trainerTwoOffset());
        min = includePlayerRelativeMin(origin, definition, min, definition.bossOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.bossOffset());

        for (GymStructureDefinition.GateBox gate : definition.stageOneGates()) {
            min = includePlayerRelativeMin(origin, definition, min, gate.min());
            min = includePlayerRelativeMin(origin, definition, min, gate.max());
            max = includePlayerRelativeMax(origin, definition, max, gate.min());
            max = includePlayerRelativeMax(origin, definition, max, gate.max());
        }

        for (GymStructureDefinition.GateBox gate : definition.stageTwoGates()) {
            min = includePlayerRelativeMin(origin, definition, min, gate.min());
            min = includePlayerRelativeMin(origin, definition, min, gate.max());
            max = includePlayerRelativeMax(origin, definition, max, gate.min());
            max = includePlayerRelativeMax(origin, definition, max, gate.max());
        }

        return new AABB(
                min.getX() - STRUCTURE_CLEANUP_PADDING,
                min.getY() - STRUCTURE_CLEANUP_PADDING,
                min.getZ() - STRUCTURE_CLEANUP_PADDING,
                max.getX() + STRUCTURE_CLEANUP_PADDING,
                max.getY() + STRUCTURE_CLEANUP_PADDING,
                max.getZ() + STRUCTURE_CLEANUP_PADDING
        );
    }

    private static BlockPos includePlayerRelativeMin(BlockPos origin, GymStructureDefinition definition, BlockPos currentMin, BlockPos offset) {
        return min(currentMin, origin.offset(definition.playerRelative(offset)));
    }

    private static BlockPos includePlayerRelativeMax(BlockPos origin, GymStructureDefinition definition, BlockPos currentMax, BlockPos offset) {
        return max(currentMax, origin.offset(definition.playerRelative(offset)));
    }

    private static BlockPos min(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
    }

    private static BlockPos max(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
    }
}
