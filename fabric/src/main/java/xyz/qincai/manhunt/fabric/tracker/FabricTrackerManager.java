package xyz.qincai.manhunt.fabric.tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.Optional;

public class FabricTrackerManager {
    private final FabricGameManager gameManager;

    public FabricTrackerManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public ItemStack createTrackerCompass(ServerPlayer target) {
        ItemStack compass = new ItemStack(Items.COMPASS);
        BlockPos pos = target.blockPosition();
        var globalPos = GlobalPos.of(target.level().dimension(), pos);
        compass.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(globalPos), false));
        return compass;
    }

    public void giveTrackerToPlayer(ServerPlayer player, ServerPlayer target) {
        ItemStack compass = createTrackerCompass(target);
        if (!player.getInventory().add(compass)) {
            player.drop(compass, true);
        }
    }
}
