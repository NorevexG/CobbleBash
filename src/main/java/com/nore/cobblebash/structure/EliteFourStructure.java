package com.nore.cobblebash.structure;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.block.EliteFourPlaqueBlock;
import com.nore.cobblebash.elitefour.EliteFourMember;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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
import java.util.List;
import java.util.function.Supplier;

public class EliteFourStructure {
    public static final String GYM_TYPE = "elite4";
    private static final ResourceLocation TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "elite4/elite4");
    private static final int CLEAR_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final int PLACE_FLAGS = Block.UPDATE_ALL;
    private static final int PRESERVE_CONNECTION_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int CLEANUP_PADDING = 8;
    private static final BlockPos PLAYER_SPAWN_OFFSET = new BlockPos(68, 25, 49);
    private static final String ELITE_FOUR_TRAINER_ID_PART = "boss";
    public static final String CHAMPION_TRAINER_GYM_TYPE = "elite4_champion";
    private static final int ELITE_FOUR_BASE_LEVEL = 95;
    private static final int CHAMPION_LEVEL = 100;
    private static final GateBox CHAMPION_GATE = new GateBox(new BlockPos(67, 24, 48), new BlockPos(69, 24, 50));
    private static final GateBox CHAMPION_SLOW_FALL_FIELD = new GateBox(new BlockPos(67, 21, 48), new BlockPos(69, 21, 50));
    private static final BlockPos LEGACY_CHAMPION_BEAM_MIN = new BlockPos(68, 4, 49);
    private static final BlockPos CHAMPION_BEAM_MIN = new BlockPos(68, 5, 49);
    private static final BlockPos CHAMPION_BEAM_MAX = new BlockPos(68, 30, 49);
    public static final int CHAMPION_BEAM_HEIGHT = CHAMPION_BEAM_MAX.getY() - CHAMPION_BEAM_MIN.getY() + 1;
    private static final PlaquePlacement[] PLAQUES = {
            new PlaquePlacement(EliteFourMember.ELECTRIC_GROUND, new BlockPos(55, 29, 49), Direction.WEST, CobbleBash.ELITE_FOUR_PLAQUE_ELECTRIC_GROUND::get),
            new PlaquePlacement(EliteFourMember.WATER_STEEL, new BlockPos(68, 29, 62), Direction.SOUTH, CobbleBash.ELITE_FOUR_PLAQUE_WATER_STEEL::get),
            new PlaquePlacement(EliteFourMember.GRASS_GHOST, new BlockPos(81, 29, 49), Direction.EAST, CobbleBash.ELITE_FOUR_PLAQUE_GRASS_GHOST::get),
            new PlaquePlacement(EliteFourMember.FIRE_FAIRY, new BlockPos(68, 29, 36), Direction.NORTH, CobbleBash.ELITE_FOUR_PLAQUE_FIRE_FAIRY::get)
    };
    private static final TrainerPlacement[] TRAINERS = {
            new TrainerPlacement(EliteFourMember.ELECTRIC_GROUND.getTrainerGymType(), new BlockPos(33, 28, 49), -90.0F, ELITE_FOUR_BASE_LEVEL),
            new TrainerPlacement(EliteFourMember.FIRE_FAIRY.getTrainerGymType(), new BlockPos(68, 28, 14), 0.0F, ELITE_FOUR_BASE_LEVEL),
            new TrainerPlacement(EliteFourMember.GRASS_GHOST.getTrainerGymType(), new BlockPos(103, 28, 49), 90.0F, ELITE_FOUR_BASE_LEVEL),
            new TrainerPlacement(EliteFourMember.WATER_STEEL.getTrainerGymType(), new BlockPos(68, 28, 84), 180.0F, ELITE_FOUR_BASE_LEVEL),
            new TrainerPlacement(CHAMPION_TRAINER_GYM_TYPE, new BlockPos(68, 3, 33), 0.0F, CHAMPION_LEVEL)
    };

    private EliteFourStructure() {
    }

    public static void build(ServerLevel level, BlockPos origin, int slotId) {
        StructureTemplate template = getTemplate(level);
        if (template == null) {
            CobbleBash.LOGGER.warn("Could not build Elite Four because structure template {} was not found.", TEMPLATE_ID);
            return;
        }

        AABB structureBox = getCleanupBox(origin, template);
        clear(level, origin);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true);
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), PLACE_FLAGS);
        restoreSavedConnectionStates(level, origin, settings, template);
        restoreDecorativeEntities(level, origin, settings, template);
        paintStructureBiome(level, structureBox);
        placePlaques(level, origin);
        spawnTrainers(level, origin, slotId);
    }

    public static void build(ServerLevel level, BlockPos origin) {
        build(level, origin, 0);
    }

    public static void clear(ServerLevel level, BlockPos origin) {
        StructureTemplate template = getTemplate(level);
        if (template == null) {
            return;
        }

        AABB cleanupBox = getCleanupBox(origin, template);
        clearEntities(level, cleanupBox);
        clearEntities(level, cleanupBox);
    }

    public static BlockPos getPlayerSpawn(ServerLevel level, BlockPos origin) {
        StructureTemplate template = getTemplate(level);
        if (template == null) {
            return origin.offset(0, 1, 0);
        }

        return origin.offset(PLAYER_SPAWN_OFFSET);
    }

    public static boolean openMemberGate(ServerLevel level, BlockPos origin, EliteFourMember member) {
        for (PlaquePlacement plaque : PLAQUES) {
            if (plaque.member() != member) {
                continue;
            }

            EliteFourPlaqueBlock.openGate(level, origin.offset(plaque.offset()), plaque.facing());
            return true;
        }

        return false;
    }

    public static void openChampionGate(ServerLevel level, BlockPos origin) {
        clearGateBox(level, origin, CHAMPION_GATE);
    }

    public static boolean isInsideSlowFallField(BlockPos origin, BlockPos pos) {
        return CHAMPION_SLOW_FALL_FIELD.contains(origin, pos);
    }

    public static BlockPos getChampionBeamMin(BlockPos origin) {
        return origin.offset(CHAMPION_BEAM_MIN);
    }

    public static BlockPos getChampionBeamMax(BlockPos origin) {
        return origin.offset(CHAMPION_BEAM_MAX);
    }

    public static void startChampionBeam(ServerLevel level, BlockPos origin) {
        stopChampionBeamAt(level, origin.offset(LEGACY_CHAMPION_BEAM_MIN));
        BlockPos pos = getChampionBeamMin(origin);
        BlockState state = level.getBlockState(pos);
        if (!state.is(CobbleBash.ELITE_FOUR_CHAMPION_BEAM.get())) {
            level.setBlock(pos, CobbleBash.ELITE_FOUR_CHAMPION_BEAM.get().defaultBlockState(), PLACE_FLAGS);
        }
    }

    public static void stopChampionBeam(ServerLevel level, BlockPos origin) {
        stopChampionBeamAt(level, origin.offset(LEGACY_CHAMPION_BEAM_MIN));
        stopChampionBeamAt(level, getChampionBeamMin(origin));
    }

    private static void stopChampionBeamAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(CobbleBash.ELITE_FOUR_CHAMPION_BEAM.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), CLEAR_FLAGS);
        }
    }

    public static AABB getStructureBox(ServerLevel level, BlockPos origin) {
        StructureTemplate template = getTemplate(level);
        if (template == null) {
            return new AABB(
                    origin.getX() - CLEANUP_PADDING,
                    origin.getY() - CLEANUP_PADDING,
                    origin.getZ() - CLEANUP_PADDING,
                    origin.getX() + 132,
                    origin.getY() + 122,
                    origin.getZ() + 116
            );
        }

        return getCleanupBox(origin, template);
    }

    private static void placePlaques(ServerLevel level, BlockPos origin) {
        for (PlaquePlacement plaque : PLAQUES) {
            BlockState state = plaque.block().get().defaultBlockState()
                    .setValue(EliteFourPlaqueBlock.FACING, plaque.facing());
            level.setBlock(origin.offset(plaque.offset()), state, PLACE_FLAGS);
        }
    }

    private static void spawnTrainers(ServerLevel level, BlockPos origin, int slotId) {
        for (TrainerPlacement trainer : TRAINERS) {
            GymPlatformBuilder.spawnTrainerEntity(
                    level,
                    origin,
                    trainer.gymType(),
                    slotId,
                    ELITE_FOUR_TRAINER_ID_PART,
                    trainer.level(),
                    origin.offset(trainer.offset()),
                    trainer.yaw()
            );
        }
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
                if (level.getBlockState(pos).is(block)) {
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

    private static void paintStructureBiome(ServerLevel level, AABB box) {
        Holder<Biome> biome = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.PLAINS);
        List<ChunkAccess> changedChunks = new ArrayList<>();

        for (ChunkPos chunkPos : getChunks(box)) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, null);
            chunk.setUnsaved(true);
            changedChunks.add(chunk);
        }

        if (!changedChunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(changedChunks);
        }
    }

    private static StructureTemplate getTemplate(ServerLevel level) {
        return level.getStructureManager().get(TEMPLATE_ID).orElse(null);
    }

    private static AABB getCleanupBox(BlockPos origin, StructureTemplate template) {
        BlockPos size = new BlockPos(template.getSize());
        return new AABB(
                origin.getX() - CLEANUP_PADDING,
                origin.getY() - CLEANUP_PADDING,
                origin.getZ() - CLEANUP_PADDING,
                origin.getX() + size.getX() + CLEANUP_PADDING,
                origin.getY() + size.getY() + CLEANUP_PADDING,
                origin.getZ() + size.getZ() + CLEANUP_PADDING
        );
    }

    private static void clearGateBox(ServerLevel level, BlockPos origin, GateBox box) {
        BlockPos min = box.min(origin);
        BlockPos max = box.max(origin);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), CLEAR_FLAGS);
                }
            }
        }
    }

    private static void clearEntities(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(
                Entity.class,
                box,
                entity -> !(entity instanceof Player) || entity instanceof ItemEntity
        ).forEach(Entity::discard);
    }

    private static List<ChunkPos> getChunks(AABB box) {
        BlockPos min = BlockPos.containing(
                box.minX,
                box.minY,
                box.minZ
        );
        BlockPos max = BlockPos.containing(
                box.maxX,
                box.maxY,
                box.maxZ
        );

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

    private record PlaquePlacement(EliteFourMember member, BlockPos offset, Direction facing, Supplier<Block> block) {
    }

    private record TrainerPlacement(String gymType, BlockPos offset, float yaw, int level) {
    }

    private record GateBox(BlockPos first, BlockPos second) {
        private BlockPos min(BlockPos origin) {
            BlockPos a = origin.offset(first);
            BlockPos b = origin.offset(second);
            return new BlockPos(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ())
            );
        }

        private BlockPos max(BlockPos origin) {
            BlockPos a = origin.offset(first);
            BlockPos b = origin.offset(second);
            return new BlockPos(
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ())
            );
        }

        private boolean contains(BlockPos origin, BlockPos pos) {
            BlockPos min = min(origin);
            BlockPos max = max(origin);
            return pos.getX() >= min.getX()
                    && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY()
                    && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ()
                    && pos.getZ() <= max.getZ();
        }
    }
}
