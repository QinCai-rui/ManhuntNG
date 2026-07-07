package xyz.qincai.manhunt.fabric.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

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

    public BlockPos findSafeSpawn(ServerLevel world, BlockPos origin) {
        BlockPos.Mutable pos = origin.mutable();
        for (int y = origin.getY(); y < world.getMaxBuildHeight(); y++) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return pos.immutable();
            }
        }
        return origin;
    }
}
