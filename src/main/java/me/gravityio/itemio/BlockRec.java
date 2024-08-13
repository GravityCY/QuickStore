package me.gravityio.itemio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record BlockRec(BlockPos pos, Direction side) {

    private static final int MAX_BREAK_SQUARED_DISTANCE = 36;

    public static BlockRec of(Level world, Player player, BlockPos pos, Direction side) {
        if (!world.getBlockState(pos.relative(side)).isAir()) {
            BlockPos diff = player.blockPosition().subtract(pos);
            side = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        }
        return new BlockRec(pos, side);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BlockRec blockRec = (BlockRec) obj;
        return this.pos.equals(blockRec.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    public net.minecraft.world.phys.BlockHitResult toBlockHitResult() {
        return new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), side, pos, false);
    }

    public Vec3 getParticlePosition() {
        return pos().getCenter().relative(side, 0.75f);
    }

    public boolean isTooFar(Player player) {
        return player.getEyePosition().distanceToSqr(pos.getCenter()) > MAX_BREAK_SQUARED_DISTANCE;
    }
}
