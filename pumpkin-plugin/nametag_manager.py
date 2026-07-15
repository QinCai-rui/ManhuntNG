from typing import Any

from game_manager import GameManager
from player_manager import PlayerManager
from player_role import PlayerRole


class NameTagManager:
    def __init__(self, config_manager: Any, player_manager: PlayerManager):
        self._config_manager = config_manager
        self._player_manager = player_manager

    def get_nametag_suffix(self, uuid: str) -> str:
        role = self._player_manager.get_role(uuid)
        if role == PlayerRole.HUNTER:
            return self._config_manager.get_string("nameTags.hunters.suffix", "<red>[H]</red>")
        elif role == PlayerRole.RUNNER:
            return self._config_manager.get_string("nameTags.runners.suffix", "<green>[R]</green>")
        return ""

    def should_show_overhead(self, uuid: str) -> bool:
        if not self._config_manager.is_nametags_enabled():
            return False
        if not self._config_manager.is_nametags_overhead_enabled():
            return False
        role = self._player_manager.get_role(uuid)
        return role != PlayerRole.SPECTATOR

    def should_show_tab_list(self, uuid: str) -> bool:
        if not self._config_manager.is_nametags_enabled():
            return False
        if not self._config_manager.is_nametags_tab_list_enabled():
            return False
        role = self._player_manager.get_role(uuid)
        return role != PlayerRole.SPECTATOR
