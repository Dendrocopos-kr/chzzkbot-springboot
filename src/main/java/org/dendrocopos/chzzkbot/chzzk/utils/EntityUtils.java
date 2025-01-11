package org.dendrocopos.chzzkbot.chzzk.utils;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


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
        return Optional.ofNullable(getProfile(messageContent))
                .flatMap(profile -> Optional.ofNullable(profile.get("nickname"))
                        .map(Object::toString))
                .orElseGet(() -> getUid(messageContent));
    }

    public static String getMsg(Map<String, Object> messageContent) {
        return ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg").toString();
    }

    public static String getDonationType(Map<String, Object> messageContent) {
        HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);

        String donationType = null;
        if (extras.get("donationType") != null) {
            donationType = extras.get("donationType").toString();
        } else if (extras.get("month") != null) {
            donationType = "subscribe";
        } else if (extras.get("month") != null) {
            donationType = "gift";
        }

        return donationType;
    }

    public static String getCost(Map<String, Object> messageContent) {
        HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);

        String cost = null;
        if (extras.get("payAmount") != null) {
            cost = extras.get("payAmount").toString();
        } else if (extras.get("tierName") != null) {
            cost = extras.get("tierName").toString();
        } else if (extras.get("giftTierName") != null) {
            cost = extras.get("giftTierName").toString();
        }
        return cost;
    }

    public static String getGiftCount(Map<String, Object> messageContent) {
        HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);
        String giftCount = null;
        if (extras.get("quantity") != null) {
            giftCount = extras.get("quantity").toString();
        }
        return giftCount;
    }

    public static String getSelectType(Map<String, Object> messageContent) {
        HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);
        String getSelectType = null;
        if (extras.get("getSelectType") != null) {
            getSelectType = extras.get("getSelectType").toString();
        }


        return getSelectType;
    }

}
