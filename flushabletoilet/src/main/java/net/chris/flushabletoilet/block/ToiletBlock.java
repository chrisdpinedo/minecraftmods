package net.chris.flushabletoilet.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A toilet. Right-click to flush: it swirls, gurgles, swallows any items
 * sitting in the bowl, then refills after a couple of seconds.
 */
public class ToiletBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty FLUSHING = BooleanProperty.create("flushing");

	private static final int FLUSH_TICKS = 50;

	// Boxes for the north-facing model (bowl toward -Z, tank at the back), in pixels.
	private static final double[][] NORTH_BOXES = {
			{5, 0, 3, 11, 6, 12},     // pedestal
			{3, 6, 1, 13, 10, 13},    // bowl + seat
			{2, 8, 13, 14, 16, 16},   // tank + lid
	};
	private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

	public ToiletBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FLUSHING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FLUSHING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// The bowl faces the player who placed it.
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(FLUSHING)) {
			// Tank is still refilling.
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			flush(state, serverLevel, pos);
		}
		return InteractionResult.SUCCESS;
	}

	private void flush(BlockState state, ServerLevel level, BlockPos pos) {
		level.setBlock(pos, state.setValue(FLUSHING, true), Block.UPDATE_ALL);
		level.scheduleTick(pos, this, FLUSH_TICKS);

		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 1.4f);
		level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.BLOCKS, 1.0f, 1.0f);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 0.8f);

		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 0.6;
		double cz = pos.getZ() + 0.5;
		level.sendParticles(ParticleTypes.SPLASH, cx, cy, cz, 30, 0.2, 0.05, 0.2, 0.1);
		level.sendParticles(ParticleTypes.BUBBLE_POP, cx, cy, cz, 15, 0.15, 0.05, 0.15, 0.02);

		// Anything sitting in the bowl goes down the drain.
		AABB bowl = new AABB(pos).inflate(0.0, 0.25, 0.0).move(0.0, 0.25, 0.0);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, bowl);
		for (ItemEntity item : items) {
			level.sendParticles(ParticleTypes.BUBBLE_POP, item.getX(), item.getY(), item.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
			item.discard();
		}
		if (!items.isEmpty()) {
			level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.2f);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(FLUSHING)) {
			level.setBlock(pos, state.setValue(FLUSHING, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7f, 1.2f);
		}
	}

	private static Map<Direction, VoxelShape> buildShapes() {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] b : NORTH_BOXES) {
				double[] r = rotateBox(b, dir);
				shape = Shapes.or(shape, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
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
