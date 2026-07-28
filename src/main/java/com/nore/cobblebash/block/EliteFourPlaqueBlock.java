package com.nore.cobblebash.block;

import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.structure.EliteFourStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class EliteFourPlaqueBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(-8.5D, -4.5D, 14.0D, 24.5D, 20.5D, 16.0D)
    );
    private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(SHAPE_SOUTH);
    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int GATE_HALF_WIDTH = 1;
    private static final int GATE_MIN_HEIGHT_OFFSET = -1;
    private static final int GATE_MAX_HEIGHT_OFFSET = 2;
    private static final int GATE_DEPTH = 9;

    private final EliteFourMember member;

    public EliteFourPlaqueBlock(EliteFourMember member, Properties properties) {
        super(properties);
        this.member = member;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (!tryOpenGate(serverLevel, pos, state.getValue(FACING), player)) {
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private boolean tryOpenGate(ServerLevel level, BlockPos pos, Direction facing, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        GymInstance instance = GymInstanceManager.getActive(serverPlayer.getUUID());
        if (instance == null || !EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            serverPlayer.sendSystemMessage(Component.literal("This Elite Four plaque is inactive."));
            return false;
        }

        if (instance.hasDefeatedEliteFourMember(member.getId())) {
            serverPlayer.sendSystemMessage(Component.literal(member.getDisplayName() + " is already defeated."));
            return false;
        }

        if (instance.hasActiveEliteFourMember()) {
            if (!instance.getActiveEliteFourMember().equals(member.getId())) {
                serverPlayer.sendSystemMessage(Component.literal("Complete previous Elite Four member."));
                return false;
            }

            serverPlayer.sendSystemMessage(Component.literal(member.getDisplayName() + " gate is already open."));
            return false;
        }

        if (!instance.selectEliteFourMember(member.getId())) {
            serverPlayer.sendSystemMessage(Component.literal("Complete previous Elite Four member."));
            return false;
        }

        openGate(level, pos, facing);
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverPlayer.sendSystemMessage(Component.literal(member.getDisplayName() + " gate opened."));
        return true;
    }

    public static void openGate(ServerLevel level, BlockPos plaquePos, Direction facing) {
        Direction widthDirection = facing.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;

        for (int depth = 1; depth <= GATE_DEPTH; depth++) {
            BlockPos gateCenter = plaquePos.relative(facing, depth);
            for (int width = -GATE_HALF_WIDTH; width <= GATE_HALF_WIDTH; width++) {
                for (int height = GATE_MIN_HEIGHT_OFFSET; height <= GATE_MAX_HEIGHT_OFFSET; height++) {
                    BlockPos clearPos = gateCenter.relative(widthDirection, width).offset(0, height, 0);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), CLEAR_FLAGS);
                }
            }
        }

        level.setBlock(plaquePos, Blocks.AIR.defaultBlockState(), CLEAR_FLAGS);
    }

    private static Map<Direction, VoxelShape> makeHorizontalShapes(VoxelShape southShape) {
        EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, rotateSouthShape(southShape, direction));
        }
        return shapes;
    }

    private static VoxelShape rotateSouthShape(VoxelShape shape, Direction direction) {
        if (direction == Direction.SOUTH) {
            return shape;
        }

        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (direction) {
                case NORTH -> Block.box(
                        16.0D - maxX * 16.0D, minY * 16.0D, 16.0D - maxZ * 16.0D,
                        16.0D - minX * 16.0D, maxY * 16.0D, 16.0D - minZ * 16.0D);
                case EAST -> Block.box(
                        minZ * 16.0D, minY * 16.0D, 16.0D - maxX * 16.0D,
                        maxZ * 16.0D, maxY * 16.0D, 16.0D - minX * 16.0D);
                case WEST -> Block.box(
                        16.0D - maxZ * 16.0D, minY * 16.0D, minX * 16.0D,
                        16.0D - minZ * 16.0D, maxY * 16.0D, maxX * 16.0D);
                default -> throw new IllegalStateException("Unexpected horizontal direction: " + direction);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0];
    }
}
