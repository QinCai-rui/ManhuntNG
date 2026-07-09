package xyz.qincai.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {
    private final ManhuntNG plugin;

    // Cooldown to prevent pause-blocked spam messages
    private final Map<UUID, Long> pauseMessageCooldowns = new HashMap<>();

    // Saved armor/offhand for hunters who keep inventory on death
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, ItemStack> savedOffhand = new HashMap<>();

    // Saved location for participants who disconnect mid-game, so they can be
    // teleported back to where they left when they reconnect
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public GameListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    /*
     * Clears saved armor/offhand caches.
     * Called when game resets.
     */
    public void clearSavedItems() {
        savedArmor.clear();
        savedOffhand.clear();
        savedLocations.clear();
    }

    /*
     * Sends a "game paused" message with cooldown to avoid spam.
     */
    private void sendPauseBlockedMessage(Player player) {
        long now = System.currentTimeMillis();

        // Remove old cooldown entries
        pauseMessageCooldowns.entrySet().removeIf(e -> now - e.getValue() > 60_000);

        UUID uuid = player.getUniqueId();
        if (now - pauseMessageCooldowns.getOrDefault(uuid, 0L) > 5000) {
            player.sendMessage(Component.text("The game is paused — action blocked", NamedTextColor.GRAY));
            pauseMessageCooldowns.put(uuid, now);
        }
    }

    /*
     * Cancels restricted actions during HEADSTART (hunter-only),
     * and during PRE_HUNT / COUNTDOWN / PAUSED (runner + hunter).
     */
    private void cancelRestrictedAction(Cancellable event, Player player) {
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.HEADSTART) {
            if (plugin.getPlayerManager().isHunter(uuid)) {
                event.setCancelled(true);
            }
            return;
        }

        if (match.getState() == GameState.PRE_HUNT ||
            match.getState() == GameState.COUNTDOWN ||
            match.getState() == GameState.PAUSED) {

            if (plugin.getPlayerManager().isRunner(uuid) ||
                plugin.getPlayerManager().isHunter(uuid)) {

                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    /*
     * Handles player joining the server.
     * If game is inactive -> they become spectator.
     * If hunter joins during RUNNING -> give compass after short delay.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getGameManager().isGameActive()) {
            plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            match.addSpectator(player.getUniqueId());
            return;
        }

        // Reconnecting participant: teleport back to where they left the game
        Location savedLocation = savedLocations.remove(player.getUniqueId());
        if (savedLocation != null && savedLocation.getWorld() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(savedLocation);
                }
            }, 5L);
        }

        // Hunters joining mid-game get a compass
        if (plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            if (match.getState() == GameState.RUNNING) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getTrackerManager().giveCompassToPlayer(player);
                    }
                }, 20L);
            }
        }

        // Re-apply the role nametag (covers mid-game rejoins where the role is retained)
        plugin.getNameTagManager().applyTag(player, plugin.getPlayerManager().getRole(player.getUniqueId()));
    }

    /*
     * Updates runner last-known location when switching worlds.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.RUNNING &&
                match.isRunner(player.getUniqueId())) {
            plugin.getTrackerManager().updateRunnerLastKnown(player);
        }
    }

    /*
     * If runner quits mid-game -> pause the game automatically.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        // Save location of any participant leaving an active game so they can be
        // teleported back to where they left when they reconnect
        if (plugin.getGameManager().isGameActive() && match.isParticipant(uuid)) {
            savedLocations.put(uuid, player.getLocation());
        }

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        // No participants of a kind left -> nothing to pause for
        if (match.getRunnerUuids().isEmpty() || match.getHunterUuids().isEmpty()) return;

        // Check if any runner/hunter (other than the one quitting) is still online.
        // The quitting player is excluded because they are no longer part of the
        // active game even if the server still reports them as present.
        boolean anyRunnerOnline = match.getRunnerUuids().stream()
                .filter(id -> !id.equals(uuid))
                .anyMatch(id -> {
                    Player p = Bukkit.getPlayer(id);
                    return p != null && p.isOnline();
                });

        boolean anyHunterOnline = match.getHunterUuids().stream()
                .filter(id -> !id.equals(uuid))
                .anyMatch(id -> {
                    Player p = Bukkit.getPlayer(id);
                    return p != null && p.isOnline();
                });

        // Only pause when EVERY runner or EVERY hunter has disconnected
        if (!anyRunnerOnline) {
            plugin.getUiManager().sendToAll("<yellow>All runners have disconnected — pausing game!");
            plugin.getGameManager().pauseGame();
        } else if (!anyHunterOnline) {
            plugin.getUiManager().sendToAll("<yellow>All hunters have disconnected — pausing game!");
            plugin.getGameManager().pauseGame();
        }
    }

    /*
     * Handles player death logic for runner + hunters.
     * - Runner death -> hunters win
     * - Hunter death -> respawn logic, inventory rules, respawn limits
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        destroyTrackingCompass(player, event);

        // Runner death
        if (plugin.getPlayerManager().isRunner(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(Component.text("(Runner) ", NamedTextColor.RED))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            // Infection mode: runner becomes a hunter (only during RUNNING)
            if (match.getGameMode() == xyz.qincai.manhunt.game.ManhuntGameMode.INFECTION) {
                // Record death before any conversion
                plugin.getStatsManager().recordDeath(uuid);

                // Only convert to hunter if match is RUNNING
                if (match.getState() == GameState.RUNNING) {
                    if (plugin.getConfigManager().isRunnerKeepInventory()) {
                        event.setKeepInventory(true);
                        event.getDrops().clear();
                    } else {
                        event.setKeepInventory(false);
                    }
                    plugin.getGameManager().infectPlayer(uuid);
                    return;
                }

                // For HEADSTART or other states, handle as normal elimination
                plugin.getPlayerManager().eliminateRunner(uuid);
                plugin.getGameManager().huntersWin();
                return;
            }

            // Normal mode: runner is eliminated, hunters win when all runners are dead
            plugin.getStatsManager().recordDeath(uuid);
            plugin.getPlayerManager().eliminateRunner(uuid);
            if (match.getRunnerUuids().isEmpty()) {
                plugin.getGameManager().huntersWin();
            }
            return;
        }

        // Hunter death -> respawn logic
        if (plugin.getPlayerManager().isHunter(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(Component.text("(Hunter) ", NamedTextColor.GOLD))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            plugin.getPlayerManager().addHunterRespawn(uuid);

            // Check respawn limit
            if (!plugin.getConfigManager().isHunterInfiniteRespawns()) {
                int limit = plugin.getConfigManager().getHunterRespawnLimit();
                if (plugin.getPlayerManager().getHunterRespawnCount(uuid) > limit) {
                    plugin.getPlayerManager().eliminateHunter(uuid);
                    return;
                }
            }

            // Inventory rules
            if (plugin.getConfigManager().isHunterKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);

                // Keep armor/offhand if configured
                if (plugin.getConfigManager().isHunterKeepArmor()) {
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    savedArmor.put(uuid, armor.clone());

                    for (ItemStack item : armor) {
                        if (item != null && item.getType() != Material.AIR) {
                            event.getDrops().remove(item);
                        }
                    }
                }

                if (plugin.getConfigManager().isHunterKeepOffhand()) {
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (offhand != null && offhand.getType() != Material.AIR) {
                        savedOffhand.put(uuid, offhand.clone());
                        event.getDrops().remove(offhand);
                    }
                }
            }
        }
    }

    /*
     * Removes tracking compasses from drops + inventory.
     */
    private void destroyTrackingCompass(Player player, PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> plugin.getTrackerManager().isTrackerCompass(item));

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (plugin.getTrackerManager().isTrackerCompass(item)) {
                player.getInventory().clear(i);
            }
        }
    }

    /*
     * Handles hunter respawn logic:
     * - Restore armor/offhand if saved
     * - Give compass (only during RUNNING, not HEADSTART)
     * - Announce respawn
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        if (plugin.getPlayerManager().isHunter(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack[] armor = savedArmor.remove(uuid);
                ItemStack offhand = savedOffhand.remove(uuid);

                if (!player.isOnline()) return;

                player.setGameMode(GameMode.SURVIVAL);

                if (armor != null) player.getInventory().setArmorContents(armor);
                if (offhand != null) player.getInventory().setItemInOffHand(offhand);

                // Only give compass during RUNNING
                if (match.getState() == GameState.RUNNING) {
                    plugin.getTrackerManager().giveCompassToPlayer(player);
                }
                plugin.getUiManager().sendToAll("<yellow>" + player.getName() + " has respawned!");
            }, 1L);
        }
    }

    /*
     * Handles PvP rules:
     * - PAUSED -> cancel
     * - HEADSTART -> cancel (prep phase)
     * - PRE_HUNT -> runner hitting hunter starts hunt
     * - RUNNING -> allow
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Match match = plugin.getGameManager().getMatch();

        // No combat during pause
        if (match.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            sendPauseBlockedMessage(damager);
            return;
        }

        // HEADSTART: no combat
        if (match.getState() == GameState.HEADSTART) {
            event.setCancelled(true);
            return;
        }

        // PRE_HUNT: runner must hit hunter to start game
        if (match.getState() == GameState.PRE_HUNT) {
            event.setCancelled(true);

            if (plugin.getPlayerManager().isRunner(damager.getUniqueId()) &&
                plugin.getPlayerManager().isHunter(victim.getUniqueId())) {
                plugin.getGameManager().startHunt();
            }
            return;
        }

        // No combat outside RUNNING
        if (match.getState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    /*
     * Movement restrictions:
     * - PAUSED -> freeze
     * - HEADSTART -> hunters freeze, runner free
     * - PRE_HUNT -> freeze
     * - COUNTDOWN -> freeze
     * - RUNNING -> allow, but track runner world changes
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        // Freeze movement during PAUSED
        if (match.getState() == GameState.PAUSED) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        // HEADSTART: hunters are frozen, runner can move freely
        if (match.getState() == GameState.HEADSTART) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        // Freeze movement during PRE_HUNT
        if (match.getState() == GameState.PRE_HUNT) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                var to = event.getTo();
                var from = event.getFrom();
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
            }
            return;
        }

        // Freeze movement during COUNTDOWN
        if (match.getState() == GameState.COUNTDOWN) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        // Track runner world changes during RUNNING
        if (match.getState() == GameState.RUNNING && plugin.getPlayerManager().isRunner(uuid)) {
            World fromWorld = event.getFrom().getWorld();
            World toWorld = event.getTo().getWorld();
            if (!fromWorld.equals(toWorld)) {
                plugin.getTrackerManager().updateRunnerLastKnown(Bukkit.getPlayer(uuid));
            }
        }
    }

    /*
     * Prevent interaction during restricted phases.
     * HEADSTART: runners can interact, hunters cannot.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        cancelRestrictedAction(event, event.getPlayer());
    }

    /*
     * Prevent block breaking during restricted phases.
     * HEADSTART: runners can break blocks, hunters cannot.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        cancelRestrictedAction(event, event.getPlayer());
    }

    /*
     * Prevent block placing during restricted phases.
     * HEADSTART: runners can place blocks, hunters cannot.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelRestrictedAction(event, event.getPlayer());
    }

    /*
     * Prevent hunters from dropping their tracking compass.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (plugin.getTrackerManager().isTrackerCompass(event.getItemDrop().getItemStack())
                && plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /*
     * Prevent entity interaction during restricted phases.
     * HEADSTART: runners can interact with entities, hunters cannot.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        cancelRestrictedAction(event, event.getPlayer());
    }

    /*
     * Prevent furnace smelting during pause.
     */
    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getBlock().getWorld();

        if (world.equals(match.getGameWorld()) ||
            world.equals(match.getNetherWorld()) ||
            world.equals(match.getEndWorld())) {

            event.setCancelled(true);
        }
    }

    /*
     * Prevent furnace burning during pause.
     */
    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getBlock().getWorld();

        if (world.equals(match.getGameWorld()) ||
            world.equals(match.getNetherWorld()) ||
            world.equals(match.getEndWorld())) {

            event.setCancelled(true);
        }
    }

    /*
     * Prevent crafting during pause.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGameManager().isGamePaused()) return;

        if (!plugin.getPlayerManager().isRunner(player.getUniqueId()) &&
            !plugin.getPlayerManager().isHunter(player.getUniqueId())) return;

        event.setCancelled(true);
        sendPauseBlockedMessage(player);
    }

    /*
     * Prevent mobs from targeting players during pause.
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;
        if (!(event.getTarget() instanceof Player target)) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getEntity().getWorld();

        if (!(world.equals(match.getGameWorld()) ||
              world.equals(match.getNetherWorld()) ||
              world.equals(match.getEndWorld()))) return;

        if (plugin.getPlayerManager().isRunner(target.getUniqueId()) ||
            plugin.getPlayerManager().isHunter(target.getUniqueId())) {

            event.setCancelled(true);
        }
    }

    /*
     * Ender Dragon death -> runner wins.
     */
    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        plugin.getGameManager().runnerWins();
    }

    /*
     * Tracks runner advancements to update progression flags.
     * These flags are used by the UI and stats systems to show game progress.
     */
    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        // Only track advancements when game is active (RUNNING or PRE_HUNT)
        if (!plugin.getGameManager().isGameActive()) return;

        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        // Only runner advancements matter for progression
        // does NOT count hunters' advancements
        if (!plugin.getPlayerManager().isRunner(player.getUniqueId())) return;

        NamespacedKey key = event.getAdvancement().getKey();

        // Update progression flags (using advancement keys)
        switch (key.toString()) {
            case "minecraft:nether/find_fortress" -> {
                // Runner found a fortress
                match.setFortressDiscovered(true);
            }
            case "minecraft:nether/find_bastion" -> {
                // Runner found a bastion
                match.setBastionDiscovered(true);
            }
            case "minecraft:nether/obtain_blaze_rod" -> {
                // Runner got a blaze roid
                match.setBlazeRodObtained(true);
            }
            case "minecraft:story/follow_ender_eye" -> {
                // Runner entered the stronghold
                match.setStrongholdDiscovered(true);
            }
        }
    }
}
