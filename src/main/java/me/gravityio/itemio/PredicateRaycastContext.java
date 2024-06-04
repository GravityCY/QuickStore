package me.gravityio.itemio;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

import java.util.function.BiPredicate;

/**
 * Custom raycast content in order to pass to the normal raycast function to make only Inventory Blocks pass
 */
public class PredicateRaycastContext extends RaycastContext {
    private final BiPredicate<BlockView, BlockPos> ignore;

    public PredicateRaycastContext(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity, BiPredicate<BlockView, BlockPos> ignore) {
        super(start, end, shapeType, fluidHandling, entity);
        this.ignore = ignore;
    }

    @Override
    public VoxelShape getBlockShape(BlockState state, BlockView world, BlockPos pos) {
        if (this.ignore.test(world, pos)) return VoxelShapes.empty();
        return super.getBlockShape(state, world, pos);
    }
}
