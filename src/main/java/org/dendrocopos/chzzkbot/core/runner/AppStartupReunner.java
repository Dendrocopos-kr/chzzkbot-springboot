package org.dendrocopos.chzzkbot.core.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.ChatCmd;
import org.dendrocopos.chzzkbot.chzzk.ChzzkServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class AppStartupReunner implements ApplicationRunner {
    private static final String CONTENT = "content";
    private static final String DATA = "data";
    private final ChzzkServices chzzkServices;
    private final Gson gson;
    private LinkedTreeMap channelInfo;
    private LinkedTreeMap chatChannelInfo;
    private LinkedTreeMap tokenInfo;
    private LinkedTreeMap myInfo;
    private final ObjectMapper mapper = new ObjectMapper();
    HashMap<String, Object> openWebSocketJson = new HashMap();
    HashMap<String, Object> bdy = new HashMap();
    HashMap<String, Object> extras = new HashMap<>();
    HashMap<String, Object> sendOpt = new HashMap<>();
    private String svcid;
    private String cid;
    private String sid;

    private WebSocketClient websocketclient;

    @Value("${Chzzk.ChannelName}")
    private String channelName;

    public AppStartupReunner(ChzzkServices chzzkServices, WebSocketClient websocketclient) {
        this.chzzkServices = chzzkServices;
        this.websocketclient = websocketclient;
        this.gson = new Gson();
    }

    @Override
    public void run(ApplicationArguments args) {

        /**
         * 채널 검색
         */
        String searchChannelInfo = chzzkServices.reqChzzk("service/v1/search/channels?keyword=" + channelName + "&offset=0&size=13&withFirstChannelContent=false")
                .block();
        log.info("channelSearch : {}", searchChannelInfo);

        HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData = gson.fromJson(searchChannelInfo, HashMap.class);
        processChannelSearch(searchChannelData);

        /**
         * UID 가져오기
         */

        String searchMyInfo = chzzkServices.reqGame("nng_main/v1/user/getUserStatus").block();
        HashMap myInfoContent = gson.fromJson(searchMyInfo, HashMap.class);
        if (myInfoContent.get("code").toString().equals("200.0")) {
            myInfo = ((LinkedTreeMap) myInfoContent.get("content"));
            log.info("search myInfo : {}", myInfo);
        }

        /**
         * 채널 ChatId 가져오기
         */
        String searchChatChannelInfo = chzzkServices.getStatus("polling/v2/channels/" + channelInfo.get("channelId") + "/live-status").block();

        HashMap searchChatChannel = gson.fromJson(searchChatChannelInfo, HashMap.class);
        if (searchChatChannel.get("code").toString().equals("200.0")) {
            chatChannelInfo = ((LinkedTreeMap) searchChatChannel.get("content"));
            log.info("search chatChannelInfo : {}", chatChannelInfo);
        }

        /**
         * 채널 accessToken 가져오기
         */
        String searchTokkenInfo = chzzkServices.reqGame("nng_main/v1/chats/access-token?channelId=" + chatChannelInfo.get("chatChannelId") + "&chatType=STREAMING").block();
        HashMap searchTokken = gson.fromJson(searchTokkenInfo, HashMap.class);
        if (searchTokken.get("code").toString().equals("200.0")) {
            tokenInfo = ((LinkedTreeMap) searchTokken.get("content"));
            log.info("search tokkenInfo : {}", tokenInfo);
        }

        /**
         * 채널ID로 챗 연결
         */


        openWebSocketJson = new HashMap();
        bdy = new HashMap();
        bdy.put("accTkn", tokenInfo.get("accessToken"));
        bdy.put("auth", "SEND");
        bdy.put("devType", 2001);
        bdy.put("uid", myInfo.get("userIdHash"));
        openWebSocketJson.put("bdy", bdy);
        openWebSocketJson.put("cid", chatChannelInfo.get("chatChannelId"));
        openWebSocketJson.put("svcid", "game");
        openWebSocketJson.put("ver", "2");
        openWebSocketJson.put("tid", 1);
        openWebSocketJson.put("cmd", 100);

        processSendMessage(gson.toJson(openWebSocketJson));
    }

    public void processSendMessage(String message) {

        int serverId = Math.abs(chatChannelInfo.get("chatChannelId").toString().chars().reduce(0, Integer::sum)) % 9 + 1;
        HashMap pongCmd = new HashMap();
        pongCmd.put("cmd", ChatCmd.PONG.getValue());
        pongCmd.put("ver", "2");

        websocketclient.execute(
                        URI.create("wss://kr-ss" + serverId + ".chat.naver.com/chat"),
                session -> /*session.send(Mono.just(session.textMessage(message)))
                        .thenMany(
                                Flux.interval(Duration.ofSeconds(20)) // Interval setup
                                        .flatMap(time -> session.send(Mono.just(session.textMessage(gson.toJson(pongCmd))))) // Message sending every 20 seconds
                        ).then()
                        .thenMany(
                                session.receive()
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .doOnNext(s -> processReceivedMessage(session,s))
                        ).then()*/
                        Flux.merge(
                                // Periodic message sending every 20 seconds
                                session.send(Mono.just(session.textMessage(message))) // Initial message after connection setup
                                        .thenMany(
                                                Flux.interval(Duration.ofSeconds(20)) // Interval setup
                                                        .flatMap(time -> session.send(Mono.just(session.textMessage(gson.toJson(pongCmd))))
                                                        ) // Message sending every 20 seconds
                                        ),
                                // Handling received messages from server
                                session.receive() // Handling received messages similar to original code
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .doOnNext(s -> processReceivedMessage(session,s))
                        ).then()
        ).subscribe(null, null, () -> {
            log.info("종료시 실행할 곳");
            /*processSendMessage(gson.toJson(openWebSocketJson));*/
        });
    }

    public void processReceivedMessage(WebSocketSession session, String receivedMessage) {
        log.info("Received WebSocket message: {}", receivedMessage);
/*
        if (data.cid) this.#chatID = data.cid;
        if (data.svcid) this.#svcid = data.svcid;
        if (data.bdy && data.bdy.sid) this.#sid = data.bdy.sid;
        */

        HashMap messageInfo = gson.fromJson(receivedMessage, HashMap.class);
        int cmd = Integer.parseInt(messageInfo.get("cmd").toString().split("\\.")[0]);

        if(messageInfo.get("svcid") != null)
        {
            svcid = messageInfo.get("svcid").toString();
        }
        if(messageInfo.get("cid") != null)
        {
            cid = messageInfo.get("cid").toString();
        }
        if(messageInfo.get("bdy") != null && messageInfo.get("bdy") instanceof LinkedTreeMap && ((LinkedTreeMap)messageInfo.get("bdy")).get("sid") != null)
        {
            sid = ((LinkedTreeMap)messageInfo.get("bdy")).get("sid").toString();
        }
        switch (ChatCmd.getCommand(cmd)){
            case ChatCmd.PING:
                log.info("PING : {}",ChatCmd.PING.getValue());
                break;
            case ChatCmd.PONG:
                log.info("PONG : {}",ChatCmd.PONG.getValue());
                break;
            case ChatCmd.CONNECT:
                log.info("CONNECT : {}",ChatCmd.CONNECT.getValue());
                break;
            case ChatCmd.CONNECTED:
                log.info("CONNECTED : {}",ChatCmd.CONNECTED.getValue());
                bdy.put("sid", ((LinkedTreeMap)messageInfo.get("bdy")).get("sid"));
                openWebSocketJson.put("bdy", bdy);
                log.info("openWebSocketJson : {}",openWebSocketJson);
                session.send(Mono.just(session.textMessage(gson.toJson(openWebSocketJson))));
                break;
            case ChatCmd.REQUEST_RECENT_CHAT:
                log.info("REQUEST_RECENT_CHAT : {}",ChatCmd.REQUEST_RECENT_CHAT.getValue());
                break;
            case ChatCmd.RECENT_CHAT:
                log.info("RECENT_CHAT : {}",ChatCmd.RECENT_CHAT.getValue());
                break;
            case ChatCmd.EVENT:
                log.info("EVENT : {}",ChatCmd.EVENT.getValue());
                break;
            case ChatCmd.CHAT:
                log.info("CHAT : {}",ChatCmd.CHAT.getValue());
                log.info("{} : {}",
                        (gson.fromJson((String)((LinkedTreeMap)((ArrayList)messageInfo.get("bdy")).get(0)).get("profile"),HashMap.class)).get("nickname")
                        ,((LinkedTreeMap)((ArrayList)messageInfo.get("bdy")).get(0)).get("msg")
                );
                break;
            case ChatCmd.DONATION:
                log.info("DONATION : {}",ChatCmd.DONATION.getValue());
                break;
            case ChatCmd.KICK:
                log.info("KICK : {}",ChatCmd.KICK.getValue());
                break;
            case ChatCmd.BLOCK:
                log.info("BLOCK : {}",ChatCmd.BLOCK.getValue());
                break;
            case ChatCmd.BLIND:
                log.info("BLIND : {}",ChatCmd.BLIND.getValue());
                break;
            case ChatCmd.NOTICE:
                log.info("NOTICE : {}",ChatCmd.NOTICE.getValue());
                break;
            case ChatCmd.PENALTY:
                log.info("PENALTY : {}",ChatCmd.PENALTY.getValue());
                break;
            case ChatCmd.SEND_CHAT:
                log.info("SEND_CHAT : {}",ChatCmd.SEND_CHAT.getValue());
                break;
            default:
                log.info("Unknown command : {}",ChatCmd.getCommand(cmd));
                break;
        }

    }

    private void processChannelSearch(HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData) {
        for (String key : searchChannelData.keySet()) {
            if (CONTENT.equals(key)) {
                LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>> content = searchChannelData.get(CONTENT);

                for (String contentKey : content.keySet()) {
                    if (DATA.equals(contentKey)) {
                        List<LinkedTreeMap<String, Object>> dataList = content.get(DATA);
                        LinkedTreeMap<String, Object> channelInfo = dataList.get(0);
                        this.channelInfo = (LinkedTreeMap<String, Object>) channelInfo.get("channel");
                        log.info("search channelInfo : {}", channelInfo);
                    }
                }
            }
        }
    }
}
