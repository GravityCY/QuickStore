package me.gravityio.itemio;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public record BlockRec(BlockPos pos, Direction side) {
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
        return Objects.hash(pos, side);
    }
}
