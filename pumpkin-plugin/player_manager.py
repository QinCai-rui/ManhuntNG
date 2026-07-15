from typing import Optional

from player_role import PlayerRole
from match import Match
from game_manager import GameManager


class PlayerManager:
    def __init__(self, game_manager: GameManager):
        self._game_manager = game_manager
        self._player_roles: dict[str, PlayerRole] = {}
        self._hunter_respawns: dict[str, int] = {}
        self._runner_respawns: dict[str, int] = {}

    def reset(self) -> None:
        self._player_roles.clear()
        self._hunter_respawns.clear()
        self._runner_respawns.clear()

    def set_role(self, uuid: str, role: PlayerRole) -> None:
        self._player_roles[uuid] = role

    def get_role(self, uuid: str) -> PlayerRole:
        return self._player_roles.get(uuid, PlayerRole.SPECTATOR)

    def is_runner(self, uuid: str) -> bool:
        return self._player_roles.get(uuid) == PlayerRole.RUNNER

    def is_hunter(self, uuid: str) -> bool:
        return self._player_roles.get(uuid) == PlayerRole.HUNTER

    def get_runner_uuids(self) -> set[str]:
        return {uuid for uuid, role in self._player_roles.items() if role == PlayerRole.RUNNER}

    def apply_role_to_player(self, uuid: str, role: PlayerRole) -> None:
        match = self._game_manager.match
        if role == PlayerRole.RUNNER:
            match.add_runner(uuid)
            match.remove_spectator(uuid)
            self.set_role(uuid, PlayerRole.RUNNER)
        elif role == PlayerRole.HUNTER:
            match.add_hunter(uuid)
            match.remove_spectator(uuid)
            self.set_role(uuid, PlayerRole.HUNTER)
        elif role == PlayerRole.SPECTATOR:
            match.add_spectator(uuid)
            self.set_role(uuid, PlayerRole.SPECTATOR)

    def infect_runner_to_hunter(self, runner_uuid: str) -> None:
        match = self._game_manager.match
        match.remove_runner(runner_uuid)
        match.add_hunter(runner_uuid)
        self.set_role(runner_uuid, PlayerRole.HUNTER)

    def remove_player_from_game(self, uuid: str) -> None:
        match = self._game_manager.match
        match.hunter_uuids.discard(uuid)
        match.spectator_uuids.discard(uuid)
        match.remove_runner(uuid)
        self._player_roles.pop(uuid, None)
        self._hunter_respawns.pop(uuid, None)
        self._runner_respawns.pop(uuid, None)

    def eliminate_runner(self, uuid: str) -> None:
        match = self._game_manager.match
        match.remove_runner(uuid)
        match.add_spectator(uuid)
        self.set_role(uuid, PlayerRole.SPECTATOR)

    def eliminate_hunter(self, uuid: str) -> None:
        match = self._game_manager.match
        match.remove_hunter(uuid)
        match.add_spectator(uuid)
        self.set_role(uuid, PlayerRole.SPECTATOR)

    def add_hunter_respawn(self, uuid: str) -> None:
        self._hunter_respawns[uuid] = self._hunter_respawns.get(uuid, 0) + 1

    def get_hunter_respawn_count(self, uuid: str) -> int:
        return self._hunter_respawns.get(uuid, 0)

    def add_runner_respawn(self, uuid: str) -> None:
        self._runner_respawns[uuid] = self._runner_respawns.get(uuid, 0) + 1

    def get_runner_respawn_count(self, uuid: str) -> int:
        return self._runner_respawns.get(uuid, 0)

    def get_alive_hunter_count(self) -> int:
        match = self._game_manager.match
        count = 0
        for uuid in match.hunter_uuids:
            role = self._player_roles.get(uuid)
            if role == PlayerRole.HUNTER:
                count += 1
        return count
