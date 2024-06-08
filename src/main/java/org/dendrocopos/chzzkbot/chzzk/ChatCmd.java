package org.dendrocopos.chzzkbot.chzzk;

import java.util.Objects;

public enum ChatCmd {
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
    SEND_CHAT(3101);

    private final int value;

    ChatCmd(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Object getCommand(int value) {
        for (ChatCmd cmd: values()) {
            if (Objects.equals(cmd.value, value)) {
                return cmd;
            }
        }
        return value;
    }

}
