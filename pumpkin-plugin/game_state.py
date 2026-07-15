from enum import Enum, auto
from typing import Optional


class GameState(Enum):
    WAITING = auto()
    COUNTDOWN = auto()
    HEADSTART = auto()
    PRE_HUNT = auto()
    RUNNING = auto()
    PAUSED = auto()
    FINISHED = auto()


class StartMode(Enum):
    DREAMSTART = "Dreamstart"
    HEADSTART = "Headstart"


class ManhuntGameMode(Enum):
    NORMAL = "Normal"
    INFECTION = "Infection"
