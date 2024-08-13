package me.gravityio.itemio.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

/**
 * Custom raycast content in order to pass to the normal raycast function to make only Inventory Blocks pass
 */
public class PredicateRaycastContext extends ClipContext {
    private final BiPredicate<BlockGetter, BlockPos> ignore;

    public PredicateRaycastContext(Vec3 start, Vec3 end, Block shapeType, Fluid fluidHandling, Entity entity, BiPredicate<BlockGetter, BlockPos> ignore) {
        super(start, end, shapeType, fluidHandling, entity);
        this.ignore = ignore;
    }

    @Override
    public @NotNull VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
        if (this.ignore.test(world, pos)) return Shapes.empty();
        return super.getBlockShape(state, world, pos);
    }
}
