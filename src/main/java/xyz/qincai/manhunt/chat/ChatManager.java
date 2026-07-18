package xyz.qincai.manhunt.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager implements Listener {
    private final ManhuntNG plugin;
    private final Map<UUID, ChatMode> playerChatModes = new ConcurrentHashMap<>();

    public ChatManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public ChatMode getChatMode(UUID uuid) {
        ChatMode explicit = playerChatModes.get(uuid);
        if (explicit != null) return explicit;
        PlayerRole role = plugin.getPlayerManager().getRole(uuid);
        return switch (role) {
            case RUNNER -> ChatMode.GLOBAL;
            case HUNTER -> ChatMode.TEAM;
            case SPECTATOR -> ChatMode.GLOBAL;
        };
    }

    public void setChatMode(UUID uuid, ChatMode mode) {
        PlayerRole role = plugin.getPlayerManager().getRole(uuid);
        if (role == PlayerRole.SPECTATOR) return;
        if (mode == getDefaultMode(role)) {
            playerChatModes.remove(uuid);
        } else {
            playerChatModes.put(uuid, mode);
        }
    }

    private ChatMode getDefaultMode(PlayerRole role) {
        return switch (role) {
            case RUNNER -> ChatMode.GLOBAL;
            case HUNTER -> ChatMode.TEAM;
            case SPECTATOR -> ChatMode.GLOBAL;
        };
    }

    public void resetDefaults() {
        playerChatModes.clear();
    }

    public void sendGlobalMessage(Player sender, String message) {
        Component msg = Component.text()
                .append(plugin.getConfigManager().getMessageComponent("chat.global-prefix"))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(plugin.getConfigManager().getMessageComponent("chat.separator"))
                .append(Component.text(message, NamedTextColor.WHITE))
                .build();
        sendToAllGameParticipants(msg);
    }

    public void sendTeamMessage(Player sender, String message) {
        PlayerRole role = plugin.getPlayerManager().getRole(sender.getUniqueId());
        Component msg = Component.text()
                .append(plugin.getConfigManager().getMessageComponent("chat.team-prefix"))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(plugin.getConfigManager().getMessageComponent("chat.separator"))
                .append(Component.text(message, NamedTextColor.WHITE))
                .build();

        switch (role) {
            case HUNTER -> {
                Match match = plugin.getGameManager().getMatch();
                for (UUID uuid : match.getHunterUuids()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage(msg);
                }
            }
            case RUNNER -> {
                UUID runnerUuid = plugin.getGameManager().getMatch().getRunnerUuid();
                if (runnerUuid != null) {
                    Player p = Bukkit.getPlayer(runnerUuid);
                    if (p != null) p.sendMessage(msg);
                }
            }
            case SPECTATOR ->
                sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.spectator-global-only"));
        }
    }

    private void sendToAllGameParticipants(Component message) {
        Match match = plugin.getGameManager().getMatch();
        for (UUID uuid : match.getHunterUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player p = Bukkit.getPlayer(runnerUuid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID uuid : match.getSpectatorUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        if (!plugin.getGameManager().isGameActive()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerRole role = plugin.getPlayerManager().getRole(uuid);
        Match match = plugin.getGameManager().getMatch();
        boolean isParticipant = role == PlayerRole.HUNTER
                || role == PlayerRole.RUNNER
                || (role == PlayerRole.SPECTATOR && match.getSpectatorUuids().contains(uuid));

        if (!isParticipant) return;

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatMode mode = getChatMode(uuid);
        if (mode == ChatMode.TEAM && isTeamSinglePlayer(uuid)) {
            mode = ChatMode.GLOBAL;
        }

        String prefix = mode == ChatMode.GLOBAL ? "[Global]" : "[Team]";

        if (mode == ChatMode.GLOBAL) {
            sendGlobalMessage(player, message);
        } else {
            sendTeamMessage(player, message);
        }

        if (plugin.getConfigManager().isChatLogToConsole()) {
            plugin.getLogger().info("[Manhunt Chat] " + prefix + " " + player.getName() + ": " + message);
        }
    }

    public boolean isTeamSinglePlayer(UUID uuid) {
        PlayerRole role = plugin.getPlayerManager().getRole(uuid);
        Match match = plugin.getGameManager().getMatch();
        return switch (role) {
            case HUNTER -> match.getHunterUuids().size() <= 1;
            case RUNNER -> true;
            case SPECTATOR -> true;
        };
    }
}
