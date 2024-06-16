package org.dendrocopos.chzzkbot.core.schduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.main.ChatMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Scheduler {
    private static final String STATUS_OPEN = "OPEN";
    private final ChatMain chatMain;

    @Scheduled(cron = "0 * * * * *") // 1분마다
    //@Scheduled(cron = "*/5 * * * * *") //5초마다
    public void checkLive() {
        log.info("Checking live scheduler");

        if (isChatOpen()) {
            openChat();
        } else {
            closeChat();
        }

    }

    private boolean isChatOpen() {
        log.info("Checking chat open scheduler");
        chatMain.fetchChannelInfo();
        chatMain.fetchChannelDetail();
        return chatMain.getChannelInfoDetail().get(ChatMain.STATUS).equals(STATUS_OPEN);
    }

    private void openChat() {
        if (!chatMain.isWebSocketOpen()) {
            log.info("Opening chat");
            chatMain.startWebSocket();
        }
    }

    private void closeChat() {
        if (chatMain.isWebSocketOpen()) {
            log.info("Closing chat");
            chatMain.stopWebSocketConnection();
        }
    }
}
