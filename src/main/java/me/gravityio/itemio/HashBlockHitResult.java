package me.gravityio.itemio;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class HashBlockHitResult extends BlockHitResult {
    public HashBlockHitResult(Vec3d pos, Direction side, BlockPos blockPos, boolean insideBlock) {
        super(pos, side, blockPos, insideBlock);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return this.getBlockPos().hashCode();
    }
}
