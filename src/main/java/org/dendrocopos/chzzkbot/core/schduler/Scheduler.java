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
    public void checkConnection() {
        log.info("Checking connection on minute basis");
        if (isChatOpen()) {
            checkAndOpenWebSocket();
        }
    }

    @Scheduled(cron = "0 0 * * * *") // 60분마다 실행되는 스케줄러
    public void disconnect() {
        log.info("Disconnecting on 10 minute basis");
        if (!isChatOpen()) {
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
        log.info("Checking chat open scheduler");
        chatMain.fetchChannelInfo();
        chatMain.fetchUserStatus();
        chatMain.fetchChatChannelInfo();
        chatMain.fetchChannelDetail();
        chatMain.fetchToken();
        if (!chatMain.isServerIdChange()) {
            closeChat();
        }
        return statusIsOpen();
        //return true;
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
