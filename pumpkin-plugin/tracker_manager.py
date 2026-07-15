import math
from typing import Optional

from game_manager import GameManager
from player_manager import PlayerManager
from player_role import PlayerRole


class TrackerManager:
    def __init__(self, game_manager: GameManager, player_manager: PlayerManager):
        self._game_manager = game_manager
        self._player_manager = player_manager
        self._all_runner_last_known: dict[str, dict[str, dict[str, float]]] = {}

    def update_runner_last_known(self, runner_uuid: str, dimension: str, x: float, y: float, z: float) -> None:
        if runner_uuid not in self._all_runner_last_known:
            self._all_runner_last_known[runner_uuid] = {}
        self._all_runner_last_known[runner_uuid][dimension] = {"x": x, "y": y, "z": z}

    def get_runner_last_known_location(self, runner_uuid: str, dimension: str) -> Optional[dict[str, float]]:
        runner_locs = self._all_runner_last_known.get(runner_uuid)
        if runner_locs is None:
            return None
        return runner_locs.get(dimension)

    def get_last_runner_location(self, dimension: str) -> Optional[dict[str, float]]:
        for runner_locs in self._all_runner_last_known.values():
            loc = runner_locs.get(dimension)
            if loc is not None:
                return loc
        return None

    def find_nearest_runner(self, hunter_uuid: str, hunter_dimension: str,
                            hunter_x: float, hunter_y: float, hunter_z: float) -> Optional[dict]:
        match = self._game_manager.match
        nearest_uuid: Optional[str] = None
        nearest_distance = float("inf")

        for runner_uuid in match.runner_uuids:
            role = self._player_manager.get_role(runner_uuid)
            if role != PlayerRole.RUNNER:
                continue

            last_known = self.get_runner_last_known_location(runner_uuid, hunter_dimension)
            if last_known is not None:
                dx = hunter_x - last_known["x"]
                dy = hunter_y - last_known["y"]
                dz = hunter_z - last_known["z"]
                distance = dx * dx + dy * dy + dz * dz
                if distance < nearest_distance:
                    nearest_distance = distance
                    nearest_uuid = runner_uuid

        if nearest_uuid is None:
            return None

        loc = self.get_runner_last_known_location(nearest_uuid, hunter_dimension)
        return {"uuid": nearest_uuid, "location": loc, "distance": math.sqrt(nearest_distance) if loc else 0}

    def clear_tracking(self) -> None:
        self._all_runner_last_known.clear()
