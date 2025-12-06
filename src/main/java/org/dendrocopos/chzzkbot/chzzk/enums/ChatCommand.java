package org.dendrocopos.chzzkbot.chzzk.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ChatCommand {
    PING(0),
    PONG(10000),
    CONNECT(100),
    CONNECTED(10100),
    REQUEST_RECENT_CHAT(5101),
    RECENT_CHAT(15101),
    EVENT(93006),
    CHAT(93101),
    DONATION(93102),
    KICK(94005),
    BLOCK(94006),
    BLIND(94008),
    NOTICE(94010),
    PENALTY(94015),
    SEND_CHAT(3101),
    MEMBER_SYNC(94201), // ✅ Member Sync 멤버 목록 동기화
    SUCCESS(13101)
        ;

    private final int value;

    ChatCommand(int value) {
        this.value = value;
    }

    /**
     * ✅ 안전한 반환 방식으로 변경 (Optional 사용)
     */
    public static Optional<ChatCommand> getCommand(int value) {
        return Arrays.stream(values())
                .filter(cmd -> cmd.value == value)
                .findFirst();
    }
}
