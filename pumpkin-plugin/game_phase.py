from enum import Enum, auto


class GamePhase(Enum):
    OVERWORLD_PREP = ("Phase I", "Overworld Prep")
    NETHER_RUSH = ("Phase II", "Nether Rush")
    FORTRESS_RUN = ("Phase III", "Fortress Run")
    BLAZE_ROD_RUN = ("Phase IV", "Blaze Rod Run")
    BASTION_ROUTE = ("Phase V", "Bastion Route")
    RETURN_EYES = ("Phase VI", "Return & Eyes")
    STRONGHOLD_DIVE = ("Phase VII", "Stronghold Dive")
    END_RUSH = ("Phase VIII", "End Rush")
    FINALE = ("Phase IX", "Finale")

    def __init__(self, number: str, fallback_name: str):
        self.number = number
        self.fallback_name = fallback_name

    def display(self) -> str:
        return f"{self.number} \u2014 {self.fallback_name}"
