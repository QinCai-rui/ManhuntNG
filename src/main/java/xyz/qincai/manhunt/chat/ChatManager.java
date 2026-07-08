package xyz.qincai.manhunt.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
                .append(Component.text("[Global] ", NamedTextColor.GOLD))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
                .build();
        sendToAllGameParticipants(msg);
    }

    public void sendTeamMessage(Player sender, String message) {
        PlayerRole role = plugin.getPlayerManager().getRole(sender.getUniqueId());
        Component msg = Component.text()
                .append(Component.text("[Team] ", NamedTextColor.DARK_GREEN))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.GRAY))
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
                sender.sendMessage(Component.text("You can only use global chat!", NamedTextColor.RED));
        }
    }

    private void sendToAllGameParticipants(Component message) {
        Match match = plugin.getGameManager().getMatch();
        for (UUID uuid : match.getHunterUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        if (match.getRunnerUuid() != null) {
            Player p = Bukkit.getPlayer(match.getRunnerUuid());
            if (p != null) p.sendMessage(message);
        }
        for (UUID uuid : match.getSpectatorUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
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

        String message = event.getMessage();
        ChatMode mode = getChatMode(uuid);
        if (mode == ChatMode.TEAM && isTeamSinglePlayer(uuid)) {
            mode = ChatMode.GLOBAL;
        }

        if (mode == ChatMode.GLOBAL) {
            sendGlobalMessage(player, message);
        } else {
            sendTeamMessage(player, message);
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
