package net.chris.flushabletoilet.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A toilet. Right-click to flush: it swirls, gurgles, swallows any items
 * sitting in the bowl, then refills after a couple of seconds.
 */
public class ToiletBlock extends HorizontalFacingBlock {
	public static final MapCodec<ToiletBlock> CODEC = createCodec(ToiletBlock::new);
	public static final BooleanProperty FLUSHING = BooleanProperty.of("flushing");

	private static final int FLUSH_TICKS = 50;

	// Boxes for the north-facing model (bowl toward -Z, tank at the back), in pixels.
	private static final double[][] NORTH_BOXES = {
			{5, 0, 3, 11, 6, 12},     // pedestal
			{3, 6, 1, 13, 10, 13},    // bowl + seat
			{2, 8, 13, 14, 16, 16},   // tank + lid
	};
	private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

	public ToiletBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(FLUSHING, false));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, FLUSHING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		// The bowl faces the player who placed it.
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES.get(state.get(FACING));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(FLUSHING)) {
			// Tank is still refilling.
			return ActionResult.PASS;
		}
		if (world instanceof ServerWorld serverWorld) {
			flush(state, serverWorld, pos);
		}
		return ActionResult.SUCCESS;
	}

	private void flush(BlockState state, ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, state.with(FLUSHING, true), Block.NOTIFY_ALL);
		world.scheduleBlockTick(pos, this, FLUSH_TICKS);

		world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6f, 1.4f);
		world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.BLOCKS, 1.0f, 1.0f);
		world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0f, 0.8f);

		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 0.6;
		double cz = pos.getZ() + 0.5;
		world.spawnParticles(ParticleTypes.SPLASH, cx, cy, cz, 30, 0.2, 0.05, 0.2, 0.1);
		world.spawnParticles(ParticleTypes.BUBBLE_POP, cx, cy, cz, 15, 0.15, 0.05, 0.15, 0.02);

		// Anything sitting in the bowl goes down the drain.
		Box bowl = new Box(pos).expand(0.0, 0.25, 0.0).offset(0.0, 0.25, 0.0);
		List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, bowl, item -> true);
		for (ItemEntity item : items) {
			world.spawnParticles(ParticleTypes.BUBBLE_POP, item.getX(), item.getY(), item.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
			item.discard();
		}
		if (!items.isEmpty()) {
			world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.5f, 1.2f);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(FLUSHING)) {
			world.setBlockState(pos, state.with(FLUSHING, false), Block.NOTIFY_ALL);
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.7f, 1.2f);
		}
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	private static Map<Direction, VoxelShape> buildShapes() {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.Type.HORIZONTAL) {
			VoxelShape shape = VoxelShapes.empty();
			for (double[] b : NORTH_BOXES) {
				double[] r = rotateBox(b, dir);
				shape = VoxelShapes.union(shape, Block.createCuboidShape(r[0], r[1], r[2], r[3], r[4], r[5]));
			}
			shapes.put(dir, shape);
		}
		return shapes;
	}

	/** Rotates a north-facing box clockwise (viewed from above) to face {@code dir}. */
	private static double[] rotateBox(double[] b, Direction dir) {
		double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
		int turns = switch (dir) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			// (x, z) -> (16 - z, x)
			double nx1 = 16 - z2, nx2 = 16 - z1;
			double nz1 = x1, nz2 = x2;
			x1 = nx1; x2 = nx2; z1 = nz1; z2 = nz2;
		}
		return new double[] {x1, b[1], z1, x2, b[4], z2};
	}
}
