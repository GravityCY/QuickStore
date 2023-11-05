package me.gravityio.itemio;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public record BlockRec(BlockPos pos, Direction side) {

    private static final int MAX_BREAK_SQUARED_DISTANCE = 36;

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

    public BlockHitResult toRaycast() {
        return new BlockHitResult(pos.toCenterPos(), side, pos, false);
    }

    public Vec3d getParticlePosition() {
        return pos.toCenterPos().offset(side, 0.75f);
    }

    public boolean isTooFar(PlayerEntity player) {
        return player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > MAX_BREAK_SQUARED_DISTANCE;
    }
}
