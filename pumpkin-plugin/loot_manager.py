from typing import Any

from game_manager import GameManager
from player_manager import PlayerManager


class LootManager:
    def __init__(self, config_manager: Any, game_manager: GameManager):
        self._config_manager = config_manager
        self._game_manager = game_manager
        self._config: dict[str, Any] = {}

    def load_config(self) -> None:
        self._config = self._config_manager.get_loot_config()

    def is_enabled(self) -> bool:
        return self._config.get("enabled", False)

    def get_mob_drops(self, entity_type: str) -> dict[str, Any] | None:
        sources = self._config.get("sources", {}).get("mob-drops", {})
        return sources.get(entity_type.lower())

    def get_chest_loot(self, loot_table_key: str) -> dict[str, Any] | None:
        sources = self._config.get("sources", {}).get("chest-loot", {})
        normalized = loot_table_key.lower()
        if normalized.startswith("minecraft:"):
            normalized = normalized[len("minecraft:"):]
        result = sources.get(normalized)
        if result is not None:
            return result
        slash_idx = normalized.rfind("/")
        if slash_idx >= 0:
            suffix = normalized[slash_idx + 1:]
            return sources.get(suffix)
        return None

    def get_bartering_source(self, key: str) -> dict[str, Any] | None:
        sources = self._config.get("sources", {}).get("piglin-bartering", {})
        return sources.get(key.lower())

    def should_apply_role(self, configured_role: str, player_role: str) -> bool:
        if configured_role.lower() == "all":
            return True
        return configured_role.lower() == player_role.lower()
