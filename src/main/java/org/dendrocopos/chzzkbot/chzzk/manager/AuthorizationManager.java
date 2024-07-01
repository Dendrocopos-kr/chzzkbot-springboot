package org.dendrocopos.chzzkbot.chzzk.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class AuthorizationManager {

    private static final String USER_ROLE_CODE = "userRoleCode";
    private static final String NICKNAME = "nickname";
    private static final String SPECIAL_USERNAME = "칠색딱따구리";

    @Value("${chzzk.bot.name}")
    private String botName;

    public boolean isSpecialUser(HashMap userInfo) {
        return userInfo.get(NICKNAME).toString().equals(botName);
    }

    public boolean hasCommandPermission(HashMap userInfo) {
        return userInfo.get(USER_ROLE_CODE).toString().equals("streamer") || userInfo.get(NICKNAME).toString().equals(SPECIAL_USERNAME);
    }
}
