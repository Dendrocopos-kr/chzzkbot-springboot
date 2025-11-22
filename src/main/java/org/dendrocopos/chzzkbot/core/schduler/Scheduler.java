package org.dendrocopos.chzzkbot.core.schduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.main.ChatMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.dendrocopos.chzzkbot.chzzk.utils.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final ChatMain chatMain;

    /**
     * ✅ 1분마다 채팅방 연결 상태를 확인하고, 필요 시 WebSocket을 엽니다.
     */
    @Scheduled(cron = "0 * * * * *") // 1분마다 실행
    //@Scheduled(cron = "*/10 * * * * *") // 10초마다 실행
    public void checkConnection() {
        log.info("🔄 채팅방 연결 확인 중...");
        if (isChatOpen()) {
            checkAndOpenWebSocket();
        }
    }

    /**
     * ✅ 60분마다 실행하여 채팅방이 닫혀야 하는 경우 WebSocket을 닫습니다.
     */
    @Scheduled(cron = "0 * * * * *") // 1분마다 실행
    public void disconnect() {
        log.info("🔌 주기적인 연결 종료 검사...");
        if (!isChatOpen()) {
            checkAndCloseWebSocket();
        }
    }

    /**
     * ✅ WebSocket을 열어야 하는 경우 실행
     */
    private void checkAndOpenWebSocket() {
        if (!isWebSocketOpen()) {
            log.info("✅ WebSocket을 엽니다.");
            chatMain.startWebSocket();
        }
    }

    /**
     * ✅ WebSocket을 닫아야 하는 경우 실행
     */
    private void checkAndCloseWebSocket() {
        if (isWebSocketOpen()) {
            log.info("❌ WebSocket을 닫습니다.");
            chatMain.stopWebSocketConnection();
        }
    }

    /**
     * ✅ WebSocket이 현재 열려 있는지 확인
     */
    private boolean isWebSocketOpen() {
        return chatMain.isWebSocketOpen();
    }

    /**
     * ✅ 채팅방이 열려 있는지 확인
     * @return 채팅 가능 여부
     */
    private boolean isChatOpen() {
        log.info("🔍 채팅방 상태 확인 중...");

        boolean isChannelAvailable = chatMain.fetchChannelInfo()
                && chatMain.fetchUserStatus()
                && chatMain.fetchChatChannelInfo()
                && chatMain.fetchChannelDetail()
                && chatMain.fetchToken();

        if (!isChannelAvailable) {
            log.warn("⚠️ 채널 정보 조회 실패 - 채팅 불가능");
            return false;
        }

        // ✅ 서버 ID 변경 감지 시 채팅 닫기
        if (!chatMain.isServerIdChange()) {
            log.warn("⚠️ 서버 ID 변경 감지됨 - 채팅 닫기");
            return false;
        }

        return statusIsOpen();
        //return true;
    }

    /**
     * ✅ 채팅방 상태가 "OPEN"인지 확인
     */
    private boolean statusIsOpen() {
        return STATUS_OPEN.equals(chatMain.getChannelInfoDetail().get(STATUS));
    }
}
