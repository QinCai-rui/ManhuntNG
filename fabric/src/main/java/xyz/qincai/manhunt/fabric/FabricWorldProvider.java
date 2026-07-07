package xyz.qincai.manhunt.fabric;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.KeyImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerResourcePackInfo;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.platform.ConfigProvider;
import xyz.qincai.manhunt.platform.WorldProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FabricWorldProvider implements WorldProvider {
    private final ManhuntNG platform;
    private MinecraftServer server;

    public FabricWorldProvider(ManhuntNG platform, MinecraftServer server) {
        this.platform = platform;
        this.server = server;
    }

    private static final Logger LOGGER = Logger.getLogger(FabricWorldProvider.class.getName());

    private ServerLevel getOverworld() {
        return server.getLevel(Level.OVERWORLD);
    }

    private ServerLevel getNether() {
        return server.getLevel(Level.NETHER);
    }

    private ServerLevel getEnd() {
        return server.getLevel(Level.END);
    }

    @Override
    public void createGameWorlds(Match match) {
        ServerLevel overworld = getOverworld();
        if (overworld == null) {
            LOGGER.log(Level.SEVERE, "Overworld not found. Cannot create game worlds.");
            return;
        }
        // Use Adventure Key for dimension identification
        match.setGameWorldName("minecraft:overworld");
        if (getNether() != null) match.setNetherWorldName("minecraft:nether");
        if (getEnd() != null) match.setEndWorldName("minecraft:the_end");

        setupGameRules(overworld);
    }

    private void setupGameRules(ServerLevel level) {
        try {
            // Use GameRules.getRule(ResourceKey<GameRule<Boolean>>) pattern
            GameRules.RuleKey<GameRules.BooleanValue> doDaylightCycleKey = GameRules.getRule(GameRules.DO_DAYLIGHT_CYCLE);
            if (doDaylightCycleKey != null) level.getGameRules().getRule(doDaylightCycleKey).set(GameRules.BooleanValue.create(true), server);
            
            GameRules.RuleKey<GameRules.BooleanValue> doWeatherCycleKey = GameRules.getRule(GameRules.DO_WEATHER_CYCLE);
            if (doWeatherCycleKey != null) level.getGameRules().getRule(doWeatherCycleKey).set(GameRules.BooleanValue.create(true), server);

            GameRules.RuleKey<GameRules.BooleanValue> doFireTickKey = GameRules.getRule(GameRules.DO_FIRE_TICK);
            if (doFireTickKey != null) level.getGameRules().getRule(doFireTickKey).set(GameRules.BooleanValue.create(true), server);
            
            GameRules.RuleKey<GameRules.BooleanValue> doMobSpawningKey = GameRules.getRule(GameRules.DO_MOB_SPAWNING);
            if (doMobSpawningKey != null) level.getGameRules().getRule(doMobSpawningKey).set(GameRules.BooleanValue.create(false), server);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set game rules", e);
        }
    }

    @Override
    public void teleportToMainWorld(Match match) {
        ServerLevel overworld = getOverworld();
        if (overworld == null) return;

        BlockPos spawnPos = overworld.getSharedSpawnPos();
        float yaw = overworld.getSpawnAngle() > 360 ? 0 : overworld.getSpawnAngle();
        float pitch = 0;

        Location targetLoc = Location.create(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, yaw, pitch);

        teleportPlayerTo(match.getRunnerUuid(), targetLoc);
        for (UUID uuid : match.getHunterUuids()) teleportPlayerTo(uuid, targetLoc);
        for (UUID uuid : match.getSpectatorUuids()) teleportPlayerTo(uuid, targetLoc);
    }

    @Override
    public void teleportToFormation(Match match) {
        ServerLevel overworld = getOverworld();
        if (overworld == null || match.getRunnerUuid() == null) return;

        BlockPos spawnPos = overworld.getSharedSpawnPos();
        float yaw = overworld.getSpawnAngle() > 360 ? 0 : overworld.getSpawnAngle();
        float pitch = 0;
        Location center = Location.create(overworld, spawnPos.getX() + 0.5, 0, spawnPos.getZ() + 0.5, yaw, pitch);

        Location formationCenter = findSafeSurfaceLocation(center.add(0, 20, 0));
        if (formationCenter == null) formationCenter = center;

        teleportPlayerTo(match.getRunnerUuid(), formationCenter.add(0, 1, 0));

        List<UUID> hunters = new ArrayList<>(match.getHunterUuids());
        int hunterCount = hunters.size();
        if (hunterCount == 0) return;

        double radius = platform.getConfigProvider().getHunterCircleRadius();
        double angleStep = 2 * Math.PI / hunterCount;

        for (int i = 0; i < hunterCount; i++) {
            double angle = angleStep * i;
            double x = formationCenter.getX() + radius * Math.cos(angle);
            double z = formationCenter.getZ() + radius * Math.sin(angle);
            Location hunterLoc = findSafeSurfaceLocation(Location.create(overworld, x, formationCenter.getY() + 20, z));
            if (hunterLoc == null) hunterLoc = formationCenter;
            teleportPlayerTo(hunters.get(i), hunterLoc);
        }
    }

    private Location findSafeSurfaceLocation(Location loc) {
        ServerLevel level = (ServerLevel) loc.level();
        BlockPos startPos = new BlockPos(loc.x(), loc.y(), loc.z());

        for (int y = startPos.getY(); y >= level.getMinBuildHeight() + 3; y--) {
            BlockPos currentPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockPos feet = currentPos.above();
            BlockPos head = feet.above();

            if (level.getBlockState(currentPos).isSolid() &&
                isPassable(level, feet) && isPassable(level, head)) {
                return Location.create(level, currentPos.getX() + 0.5, currentPos.getY() + 1.0, currentPos.getZ() + 0.5, loc.yaw(), loc.pitch());
            }
        }
        return null;
    }

    private boolean isPassable(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block.getMaterial().isReplaceable() || block.getMaterial().isLiquid();
    }

    private void teleportPlayerTo(UUID uuid, Location loc) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            ServerLevel targetLevel = (ServerLevel) loc.level();
            player.teleportTo(targetLevel, loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
        }
    }

    @Override public void freezeAllPlayers(Match match) {
        setPlayerMovementSpeed(match.getRunnerUuid(), 0f);
        for (UUID uuid : match.getHunterUuids()) setPlayerMovementSpeed(uuid, 0f);
    }

    @Override public void unfreezeAllPlayers(Match match) {
        setPlayerMovementSpeed(match.getRunnerUuid(), 0.1f); 
        for (UUID uuid : match.getHunterUuids()) setPlayerMovementSpeed(uuid, 0.1f);
    }

    @Override public void unfreezeHorizontalAllPlayers(Match match) { 
        setPlayerMovementSpeed(match.getRunnerUuid(), 0f); 
    }

    private void setPlayerMovementSpeed(UUID uuid, float speed) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) movementSpeed.setBaseValue(speed);

            AttributeInstance flyingSpeed = player.getAttribute(Attributes.FLYING_SPEED);
            if (flyingSpeed != null) flyingSpeed.setBaseValue(speed == 0f ? 0 : 0.05f);
        }
    }

    @Override
    public void setAllPlayersSurvival(Match match) {
        setPlayerGameMode(match.getRunnerUuid(), net.minecraft.world.level.GameType.SURVIVAL);
        for (UUID uuid : match.getHunterUuids()) setPlayerGameMode(uuid, net.minecraft.world.level.GameType.SURVIVAL);
    }

    private void setPlayerGameMode(UUID uuid, net.minecraft.world.level.GameType gameType) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) player.setGameMode(gameType);
    }

    @Override
    public void healAllPlayers(Match match) {
        healPlayer(match.getRunnerUuid());
        for (UUID uuid : match.getHunterUuids()) healPlayer(uuid);
    }

    private void healPlayer(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
    }

    @Override
    public void clearPlayerState(Match match) {
        clearPlayerInventory(match.getRunnerUuid());
        for (UUID uuid : match.getHunterUuids()) clearPlayerInventory(uuid);
        // Advancements clearing is complex and potentially game-breaking -- omitting for now.
    }

    private void clearPlayerInventory(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            player.getInventory().clear();
            player.getInventory().setArmorAndWearables(net.minecraft.world.entity.EquipmentSlot.entries(), net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    @Override
    public void setAllPlayersInvulnerable(Match match, boolean invulnerable) {
        setPlayerInvulnerable(match.getRunnerUuid(), invulnerable);
        for (UUID uuid : match.getHunterUuids()) setPlayerInvulnerable(uuid, invulnerable);
    }

    @Override
    public void setInvulnerable(UUID uuid, boolean invulnerable) {
        setPlayerInvulnerable(uuid, invulnerable);
    }

    private void setPlayerInvulnerable(UUID uuid, boolean invulnerable) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) player.setInvulnerable(invulnerable);
    }

    @Override
    public void setGameRule(Match match, String ruleName, boolean value) {
        ServerLevel level = getOverworld(); 
        if (level == null) return;
        try {
            // Need to get the correct GameRules.Key for the rule name
            Optional<GameRules.RuleKey<GameRules.BooleanValue>> ruleKeyOpt = GameRules.getGameRuleKey(ruleName); 
            
            if (ruleKeyOpt.isPresent()) {
                 GameRules.RuleKey<GameRules.BooleanValue> ruleKey = ruleKeyOpt.get();
                 level.getGameRules().getRule(ruleKey).set(GameRules.BooleanValue.create(value), server);
            } else {
                LOGGER.log(Level.WARNING, "Unknown game rule: " + ruleName);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set game rule: " + ruleName, e);
        }
    }

    @Override
    public void clearMobTargets(Match match) {
        ServerLevel level = getOverworld();
        if (level == null) return;
        Collection<Entity> entities = level.getEntities().getAll(); 
        for (Entity entity : entities) {
            if (entity instanceof Mob mob && mob.getTarget() instanceof ServerPlayer) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    public void clearEffects(Match match) {
        clearPlayerEffects(match.getRunnerUuid());
        for (UUID uuid : match.getHunterUuids()) clearPlayerEffects(uuid);
    }

    private void clearPlayerEffects(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            player.removeAllEffects();
        }
    }

    @Override
    public void sendTitle(UUID playerUuid, String title, String subtitle) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;

        net.kyori.adventure.text.Component titleComp = Component.text(title).color(NamedTextColor.GOLD);
        net.kyori.adventure.text.Component subtitleComp = subtitle != null ? Component.text(subtitle).color(NamedTextColor.YELLOW) : Component.empty();
        
        // Use Adventure's Title API
        Title adventureTitle = Title.title(titleComp, subtitleComp, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));
        
        player.getAudience().openSubtitle(adventureTitle); // Audience API handles sending titles
    }
}
