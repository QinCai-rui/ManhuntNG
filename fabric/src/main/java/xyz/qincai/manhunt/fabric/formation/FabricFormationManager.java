package xyz.qincai.manhunt.fabric.formation;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class FabricFormationManager {

    public List<BlockPos> arrangeInCircle(BlockPos center, int count, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        double angleStep = 2 * Math.PI / count;

        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            int x = center.getX() + (int) (radius * Math.cos(angle));
            int z = center.getZ() + (int) (radius * Math.sin(angle));
            positions.add(new BlockPos(x, center.getY(), z));
        }

        return positions;
    }

    public BlockPos findSafeSpawn(ServerWorld world, BlockPos origin) {
        BlockPos.Mutable pos = origin.mutableCopy();
        for (int y = origin.getY(); y < world.getTopY(); y++) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir()) {
                return pos.toImmutable();
            }
        }
        return origin;
    }
}
