package xyz.qincai.manhunt.platform;

import java.util.logging.Logger;

public interface ManhuntPlatform {
    Logger getLogger();
    Scheduler getScheduler();
    ConfigProvider getConfigProvider();
    UIFacade getUIFacade();
    PlayerRegistry getPlayerRegistry();
    WorldProvider getWorldProvider();
    String getPlatformName();
}
