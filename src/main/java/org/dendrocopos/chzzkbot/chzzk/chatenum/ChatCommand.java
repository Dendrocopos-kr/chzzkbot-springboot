package org.dendrocopos.chzzkbot.chzzk.chatenum;

import lombok.Getter;

import java.util.Objects;

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
    MEMBER_SYNC(94201);//Member Sync 멤버 목록 동기화.

    private final int value;

    ChatCommand(int value) {
        this.value = value;
    }

    public static Object getCommandValue(int value) {
        for (ChatCommand cmd : values()) {
            if (Objects.equals(cmd.value, value)) {
                return cmd;
            }
        }
        return value;
    }

}
