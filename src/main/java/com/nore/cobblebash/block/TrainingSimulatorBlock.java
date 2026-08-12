package com.nore.cobblebash.block;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.simulator.TrainingSimulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class TrainingSimulatorBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_SHAPE_SOUTH = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 10.0D, 14.0D),
            Block.box(1.0D, 10.0D, 1.0D, 15.0D, 13.0D, 15.0D),
            Block.box(1.0D, 13.0D, 1.0D, 15.0D, 15.0D, 10.0D),
            Block.box(2.0D, 13.0D, 11.0D, 4.0D, 14.0D, 13.0D),
            Block.box(5.0D, 13.0D, 11.0D, 7.0D, 14.0D, 13.0D),
            Block.box(8.0D, 13.0D, 11.0D, 10.0D, 14.0D, 13.0D),
            Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D),
            Block.box(1.0D, 15.0D, 1.0D, 2.0D, 16.0D, 10.0D),
            Block.box(2.0D, 15.0D, 1.0D, 10.0D, 16.0D, 9.0D),
            Block.box(10.0D, 15.0D, 1.0D, 15.0D, 16.0D, 10.0D)
    );
    private static final VoxelShape UPPER_SHAPE_SOUTH = Shapes.or(
            Block.box(3.0D, 0.0D, 0.0D, 13.0D, 6.0D, 1.0D),
            Block.box(1.0D, 0.0D, 1.0D, 2.0D, 8.0D, 10.0D),
            Block.box(2.0D, 0.0D, 1.0D, 10.0D, 7.0D, 9.0D),
            Block.box(10.0D, 0.0D, 1.0D, 15.0D, 7.0D, 10.0D),
            Block.box(2.0D, 7.0D, 1.0D, 15.0D, 8.0D, 10.0D)
    );
    private static final Map<Direction, VoxelShape> LOWER_SHAPES = makeHorizontalShapes(LOWER_SHAPE_SOUTH);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = makeHorizontalShapes(UPPER_SHAPE_SOUTH);

    public TrainingSimulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        openSimulator(level, pos, state, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        openSimulator(level, pos, state, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openSimulator(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!lowerState.is(this) || lowerState.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new TrainingSimulatorMenu(
                        containerId,
                        inventory,
                        net.minecraft.world.inventory.ContainerLevelAccess.create(level, lowerPos)
                ),
                CobbleBash.TRAINING_SIMULATOR.get().getName()
        ));
    }

    private static VoxelShape getShapeForState(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return UPPER_SHAPES.get(facing);
        }
        return LOWER_SHAPES.get(facing);
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
