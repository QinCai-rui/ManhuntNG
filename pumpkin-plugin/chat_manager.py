from typing import Optional

from chat_mode import ChatMode
from player_role import PlayerRole
from game_manager import GameManager
from player_manager import PlayerManager


class ChatManager:
    def __init__(self, game_manager: GameManager, player_manager: PlayerManager):
        self._game_manager = game_manager
        self._player_manager = player_manager
        self._player_chat_modes: dict[str, ChatMode] = {}

    def get_chat_mode(self, uuid: str) -> ChatMode:
        explicit = self._player_chat_modes.get(uuid)
        if explicit is not None:
            return explicit
        role = self._player_manager.get_role(uuid)
        if role == PlayerRole.HUNTER:
            return ChatMode.TEAM
        return ChatMode.GLOBAL

    def set_chat_mode(self, uuid: str, mode: ChatMode) -> None:
        role = self._player_manager.get_role(uuid)
        if role == PlayerRole.SPECTATOR:
            return
        default = self._get_default_mode(role)
        if mode == default:
            self._player_chat_modes.pop(uuid, None)
        else:
            self._player_chat_modes[uuid] = mode

    def _get_default_mode(self, role: PlayerRole) -> ChatMode:
        if role == PlayerRole.HUNTER:
            return ChatMode.TEAM
        return ChatMode.GLOBAL

    def reset_defaults(self) -> None:
        self._player_chat_modes.clear()

    def is_team_single_player(self, uuid: str) -> bool:
        role = self._player_manager.get_role(uuid)
        match = self._game_manager.match
        if role == PlayerRole.HUNTER:
            return len(match.hunter_uuids) <= 1
        return True

    def send_global_message(self, sender_uuid: str, sender_name: str, message: str) -> str:
        prefix = self._player_manager._game_manager._config_manager.get_message("chat.global-prefix") if hasattr(self._player_manager._game_manager, '_config_manager') else "[Global] "
        return f"{prefix}{sender_name}: {message}"

    def send_team_message(self, sender_uuid: str, sender_name: str, message: str) -> str:
        role = self._player_manager.get_role(sender_uuid)
        match = self._game_manager.match

        if role == PlayerRole.HUNTER:
            recipients = match.hunter_uuids
        elif role == PlayerRole.RUNNER:
            recipients = match.runner_uuids
        else:
            return ""

        return f"[Team] {sender_name}: {message}"

    def get_recipients_for_team(self, sender_uuid: str) -> set[str]:
        role = self._player_manager.get_role(sender_uuid)
        match = self._game_manager.match

        if role == PlayerRole.HUNTER:
            return set(match.hunter_uuids)
        elif role == PlayerRole.RUNNER:
            return set(match.runner_uuids)
        return set()

    def get_all_participants(self) -> set[str]:
        match = self._game_manager.match
        return match.runner_uuids | match.hunter_uuids | match.spectator_uuids
