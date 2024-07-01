package org.dendrocopos.chzzkbot.chzzk.utils;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class EntityUtils {
    private final static Gson gson = new Gson();

    //methods to extract message content data
    public static HashMap<String, Object> getProfile(Map<String, Object> messageContent) {
        return (gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("profile"), HashMap.class));
    }

    public static String getUid(Map<String, Object> messageContent) {
        return ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("uid").toString();
    }

    public static String getNickname(Map<String, Object> messageContent) {
        if (getProfile(messageContent) == null) {
            return getUid(messageContent);
        }
        return getProfile(messageContent).get("nickname").toString();
    }

    public static String getMsg(Map<String, Object> messageContent) {
        return ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg").toString();
    }

    public static String getDonationType(Map<String, Object> messageContent) {
        HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);

        String donationType = null;
        if (extras.get("donationType") != null) {
            donationType = extras.get("donationType").toString();
        }

        return donationType;
    }

    public static String getCost(Map<String, Object> messageContent) {
        return (gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class)).get("payAmount").toString();
    }

}
