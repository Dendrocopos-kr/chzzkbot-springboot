package org.dendrocopos.chzzkbot.core.schduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.main.ChatMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.dendrocopos.chzzkbot.chzzk.utils.Constants.STATUS;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Scheduler {
    private static final String STATUS_OPEN = "OPEN";
    private final ChatMain chatMain;

    @Scheduled(cron = "0 * * * * *") // 1분마다
    //@Scheduled(cron = "*/5 * * * * *") //5초마다
    public void checkLive() {
        log.debug("Checking live scheduler");
        if (isChatOpen()) {
            checkAndOpenWebSocket();
        } else {
            checkAndCloseWebSocket();
        }
    }

    private void checkAndOpenWebSocket() {
        if (!isWebSocketOpen()) {
            openChat();
        }
    }

    private void checkAndCloseWebSocket() {
        if (isWebSocketOpen()) {
            closeChat();
        }
    }

    private boolean isWebSocketOpen() {
        return chatMain.isWebSocketOpen();
    }

    private boolean isChatOpen() {
        log.debug("Checking chat open scheduler");
        chatMain.fetchChannelInfo();
        chatMain.fetchUserStatus();
        chatMain.fetchChatChannelInfo();
        chatMain.fetchChannelDetail();
        chatMain.fetchToken();
        if (!chatMain.isServerIdChange()) {
            closeChat();
        }
        return statusIsOpen();
    }

    private boolean statusIsOpen() {
        return chatMain.getChannelInfoDetail().get(STATUS).equals(STATUS_OPEN);
    }

    private void openChat() {
        log.info("Opening chat");
        chatMain.startWebSocket();
    }

    private void closeChat() {
        log.info("Closing chat");
        chatMain.stopWebSocketConnection();
    }
}
