from typing import Any


class StatisticsManager:
    def __init__(self):
        self._player_stats: dict[str, dict[str, int]] = {}
        self.runner_wins: int = 0
        self.hunter_wins: int = 0

    def reset(self) -> None:
        self._player_stats.clear()
        self.runner_wins = 0
        self.hunter_wins = 0

    def record_kill(self, killer: str, victim: str) -> None:
        self._ensure_player(killer)
        self._ensure_player(victim)
        self._player_stats[killer]["kills"] += 1
        self._player_stats[victim]["deaths"] += 1

    def record_death(self, uuid: str) -> None:
        self._ensure_player(uuid)
        self._player_stats[uuid]["deaths"] += 1

    def record_win(self, runner_won: bool) -> None:
        if runner_won:
            self.runner_wins += 1
        else:
            self.hunter_wins += 1

    def get_player_stats(self, uuid: str) -> dict[str, int]:
        return self._player_stats.get(uuid, {"kills": 0, "deaths": 0, "time_played": 0})

    def _ensure_player(self, uuid: str) -> None:
        if uuid not in self._player_stats:
            self._player_stats[uuid] = {"kills": 0, "deaths": 0, "time_played": 0}
