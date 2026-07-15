import os
import json
from typing import Any, Optional


class ConfigManager:
    def __init__(self, data_folder: str):
        self._data_folder = data_folder
        self._config: dict[str, Any] = {}
        self._messages: dict[str, str] = {}
        self._loot_config: dict[str, Any] = {}

    @property
    def data_folder(self) -> str:
        return self._data_folder

    def load_configs(self) -> None:
        self._config = self._load_yaml("config.yml")
        self._messages = self._load_yaml("messages.yml")
        self._load_loot()

    def reload_configs(self) -> None:
        self.load_configs()

    def _load_yaml(self, filename: str) -> dict[str, Any]:
        path = os.path.join(self._data_folder, filename)
        if not os.path.exists(path):
            resources_path = os.path.join(os.path.dirname(__file__), "resources", filename)
            if os.path.exists(resources_path):
                with open(resources_path, "r", encoding="utf-8") as f:
                    return self._parse_yaml(f.read())
            return {}

        with open(path, "r", encoding="utf-8") as f:
            return self._parse_yaml(f.read())

    def _parse_yaml(self, content: str) -> dict[str, Any]:
        result: dict[str, Any] = {}
        current_section: dict[str, Any] = result
        section_stack: list[dict[str, Any]] = [result]
        indent_stack: list[int] = [-1]

        for line in content.split("\n"):
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue

            indent = len(line) - len(line.lstrip())

            while indent_stack and indent <= indent_stack[-1]:
                section_stack.pop()
                indent_stack.pop()

            current_section = section_stack[-1]

            if ":" in stripped:
                key, _, value = stripped.partition(":")
                key = key.strip()
                value = value.strip()

                if value:
                    current_section[key] = self._parse_yaml_value(value)
                else:
                    new_section: dict[str, Any] = {}
                    current_section[key] = new_section
                    section_stack.append(new_section)
                    indent_stack.append(indent)

        return result

    def _parse_yaml_value(self, value: str) -> Any:
        if value.startswith("'") and value.endswith("'"):
            return value[1:-1]
        if value.startswith('"') and value.endswith('"'):
            return value[1:-1]
        if value.lower() == "true":
            return True
        if value.lower() == "false":
            return False
        if value.lower() in ("null", "~"):
            return None
        try:
            if "." in value:
                return float(value)
            return int(value)
        except ValueError:
            return value

    def _load_loot(self) -> None:
        loot_path = os.path.join(self._data_folder, "loot.json")
        if not os.path.exists(loot_path):
            resources_path = os.path.join(os.path.dirname(__file__), "resources", "loot.json")
            if os.path.exists(resources_path):
                loot_path = resources_path
            else:
                return

        try:
            with open(loot_path, "r", encoding="utf-8") as f:
                self._loot_config = json.load(f)
        except (json.JSONDecodeError, OSError):
            self._loot_config = {}

    def get_config(self) -> dict[str, Any]:
        return self._config

    def get_messages(self) -> dict[str, str]:
        return self._messages

    def get_message(self, key: str, **replacements: str) -> str:
        msg = self._messages.get(key, key)
        for placeholder, value in replacements.items():
            msg = msg.replace(placeholder, value)
        return msg

    def _get_config_value(self, path: str, default: Any = None) -> Any:
        keys = path.split(".")
        current: Any = self._config
        for key in keys:
            if isinstance(current, dict):
                current = current.get(key)
                if current is None:
                    return default
            else:
                return default
        return current if current is not None else default

    def get_int(self, path: str, default: int = 0) -> int:
        val = self._get_config_value(path, default)
        try:
            return int(val)
        except (TypeError, ValueError):
            return default

    def get_bool(self, path: str, default: bool = False) -> bool:
        val = self._get_config_value(path, default)
        if isinstance(val, bool):
            return val
        return default

    def get_float(self, path: str, default: float = 0.0) -> float:
        val = self._get_config_value(path, default)
        try:
            return float(val)
        except (TypeError, ValueError):
            return default

    def get_string(self, path: str, default: str = "") -> str:
        val = self._get_config_value(path, default)
        return str(val) if val is not None else default

    def get_list(self, path: str) -> list[Any]:
        val = self._get_config_value(path)
        if isinstance(val, list):
            return val
        return []

    def get_headstart_duration(self) -> int:
        return self.get_int("headstart.duration", 10)

    def is_headstart_compass_enabled(self) -> bool:
        return self.get_bool("headstart.compassEnabled", False)

    def get_pre_hunt_countdown(self) -> int:
        return self.get_int("preHunt.countdown", 5)

    def get_hunter_circle_radius(self) -> float:
        return self.get_float("preHunt.hunterCircleRadius", 3.0)

    def is_tracking_enabled(self) -> bool:
        return self.get_bool("tracking.enabled", True)

    def get_tracking_update_ticks(self) -> int:
        return self.get_int("tracking.updateTicks", 5)

    def is_tracking_show_distance(self) -> bool:
        return self.get_bool("tracking.showDistance", True)

    def get_runner_respawn_limit(self) -> int:
        return self.get_int("runner.respawnLimit", 0)

    def is_runner_keep_inventory(self) -> bool:
        return self.get_bool("runner.keepInventory", False)

    def get_hunter_respawn_limit(self) -> int:
        return self.get_int("hunters.respawnLimit", -1)

    def is_chat_log_to_console(self) -> bool:
        return self.get_bool("chat.logToConsole", True)

    def is_scoreboard_enabled(self) -> bool:
        return self.get_bool("scoreboard.enabled", True)

    def is_action_bar_enabled(self) -> bool:
        return self.get_bool("actionBar.enabled", True)

    def is_nametags_enabled(self) -> bool:
        return self.get_bool("nameTags.enabled", True)

    def is_nametags_overhead_enabled(self) -> bool:
        return self.get_bool("nameTags.overhead", True)

    def is_nametags_tab_list_enabled(self) -> bool:
        return self.get_bool("nameTags.tabList", True)

    def is_pause_timeout_enabled(self) -> bool:
        return self.get_bool("pauseTimeout.enabled", False)

    def get_pause_timeout_duration(self) -> int:
        return self.get_int("pauseTimeout.duration", 60)

    def is_hunter_keep_inventory(self) -> bool:
        return self.get_bool("hunters.keepInventory", False)

    def is_hunter_keep_armor(self) -> bool:
        return self.get_bool("hunters.keepArmor", False)

    def is_hunter_keep_offhand(self) -> bool:
        return self.get_bool("hunters.keepOffhand", False)

    def get_runner_potion_effects(self) -> list[dict[str, Any]]:
        return self.get_list("potionEffects.runner")

    def get_hunter_potion_effects(self) -> list[dict[str, Any]]:
        return self.get_list("potionEffects.hunters")

    def get_loot_config(self) -> dict[str, Any]:
        return self._loot_config
