package xyz.qincai.manhunt.fabric.tracker;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.Optional;

public class FabricTrackerManager {
    private final FabricGameManager gameManager;

    public FabricTrackerManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public ItemStack createTrackerCompass(ServerPlayerEntity target) {
        ItemStack compass = new ItemStack(Items.COMPASS);
        BlockPos pos = target.getBlockPos();
        var globalPos = GlobalPos.create(target.getWorld().getRegistryKey(), pos);
        compass.set(DataComponentTypes.LODESTONE_TRACKER,
                new LodestoneTrackerComponent(Optional.of(globalPos), false));
        return compass;
    }

    public void giveTrackerToPlayer(ServerPlayerEntity player, ServerPlayerEntity target) {
        ItemStack compass = createTrackerCompass(target);
        player.getInventory().offerOrDrop(compass);
    }
}
