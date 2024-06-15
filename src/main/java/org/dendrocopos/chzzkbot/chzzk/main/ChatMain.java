package org.dendrocopos.chzzkbot.chzzk.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatenum.ChatCmd;
import org.dendrocopos.chzzkbot.chzzk.chatservice.ChzzkServices;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMain {
    private static final String CONTENT = "content";
    private static final String DATA = "data";
    private final ChzzkServices chzzkServices;
    private final Gson gson = new Gson();
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebSocketClient websocketclient;
    private final CommandMessageRepository messageRepository;
    HashMap<String, Object> openWebSocketJson = new HashMap();
    HashMap<String, Object> bdy = new HashMap();
    private LinkedTreeMap channelInfo;
    private LinkedTreeMap chatChannelInfo;
    private LinkedTreeMap tokenInfo;
    private LinkedTreeMap myInfo;
    private String svcid;
    private String cid;
    private String sid;
    @Value("${chzzk.ChannelName}")
    private String channelName;

    @EventListener(ApplicationReadyEvent.class)
    public void startWebSocket() {

        /**
         * 채널 검색
         */
        String searchChannelInfo = chzzkServices.reqChzzk("service/v1/search/channels?keyword=" + channelName + "&offset=0&size=13&withFirstChannelContent=false")
                .block();
        log.info("channelSearch : {}", searchChannelInfo);

        HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData = gson.fromJson(searchChannelInfo, HashMap.class);
        channelInfo = processChannelSearch(searchChannelData);

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
        String searchTokenInfo = chzzkServices.reqGame("nng_main/v1/chats/access-token?channelId=" + chatChannelInfo.get("chatChannelId") + "&chatType=STREAMING").block();
        HashMap searchToken = gson.fromJson(searchTokenInfo, HashMap.class);
        if (searchToken.get("code").toString().equals("200.0")) {
            tokenInfo = ((LinkedTreeMap) searchToken.get("content"));
            log.info("search tokenInfo : {}", tokenInfo);
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

        int serverId = Math.abs(chatChannelInfo.get("chatChannelId").toString().chars()
                .reduce(0, Integer::sum)) % 9 + 1;
        HashMap pongCmd = new HashMap();
        pongCmd.put("cmd", ChatCmd.PONG.getValue());
        pongCmd.put("ver", "2");

        websocketclient.execute(
                URI.create("wss://kr-ss" + serverId + ".chat.naver.com/chat"),
                session ->
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
                                        .doOnNext(s -> processReceivedMessage(session, s))
                        ).then()
        ).subscribe(null, null, () -> {
            log.info("종료시 실행할 곳");
        });
    }

    public void processReceivedMessage(WebSocketSession session, String receivedMessage) {
        log.info("Received WebSocket message: {}", receivedMessage);

        HashMap messageInfo = gson.fromJson(receivedMessage, HashMap.class);
        int cmd = Integer.parseInt(messageInfo.get("cmd").toString().split("\\.")[0]);

        if (messageInfo.get("svcid") != null) {
            svcid = messageInfo.get("svcid").toString();
        }
        if (messageInfo.get("cid") != null) {
            cid = messageInfo.get("cid").toString();
        }
        if (messageInfo.get("bdy") != null && messageInfo.get("bdy") instanceof LinkedTreeMap && ((LinkedTreeMap) messageInfo.get("bdy")).get("sid") != null) {
            sid = ((LinkedTreeMap) messageInfo.get("bdy")).get("sid").toString();
        }

        switch (ChatCmd.getCommand(cmd)) {
            case ChatCmd.PING:
                //log.info("PING : {}", ChatCmd.PING.getValue());
                break;
            case ChatCmd.PONG:
                //log.info("PONG : {}", ChatCmd.PONG.getValue());
                break;
            case ChatCmd.CONNECT:
                //log.info("CONNECT : {}", ChatCmd.CONNECT.getValue());
                break;
            case ChatCmd.CONNECTED:
                //log.info("CONNECTED : {}", ChatCmd.CONNECTED.getValue());
                bdy.put("sid", ((LinkedTreeMap) messageInfo.get("bdy")).get("sid"));
                openWebSocketJson.put("bdy", bdy);
                //log.info("openWebSocketJson : {}", openWebSocketJson);
                session.send(Mono.just(session.textMessage(gson.toJson(openWebSocketJson))));
                break;
            case ChatCmd.REQUEST_RECENT_CHAT:
                //log.info("REQUEST_RECENT_CHAT : {}", ChatCmd.REQUEST_RECENT_CHAT.getValue());
                break;
            case ChatCmd.RECENT_CHAT:
                //log.info("RECENT_CHAT : {}", ChatCmd.RECENT_CHAT.getValue());
                break;
            case ChatCmd.EVENT:
                //log.info("EVENT : {}", ChatCmd.EVENT.getValue());
                break;
            case ChatCmd.CHAT:
                //log.info("CHAT : {}", ChatCmd.CHAT.getValue());
                log.info("{} : {}",
                        (gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("profile"), HashMap.class)).get("nickname")
                        , ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("msg")
                );
                sendCommandMessage(session, gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("profile"), HashMap.class), ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("msg").toString());

                break;
            case ChatCmd.DONATION:
                //log.info("DONATION : {}", ChatCmd.DONATION.getValue());
                break;
            case ChatCmd.KICK:
                //log.info("KICK : {}", ChatCmd.KICK.getValue());
                break;
            case ChatCmd.BLOCK:
                //log.info("BLOCK : {}", ChatCmd.BLOCK.getValue());
                break;
            case ChatCmd.BLIND:
                //log.info("BLIND : {}", ChatCmd.BLIND.getValue());
                break;
            case ChatCmd.NOTICE:
                //log.info("NOTICE : {}", ChatCmd.NOTICE.getValue());
                break;
            case ChatCmd.PENALTY:
                //log.info("PENALTY : {}", ChatCmd.PENALTY.getValue());
                break;
            case ChatCmd.SEND_CHAT:
                //log.info("SEND_CHAT : {}", ChatCmd.SEND_CHAT.getValue());
                break;
            default:
                log.info("Unknown command : {}", ChatCmd.getCommand(cmd));
                break;
        }

    }

    private void sendCommandMessage(WebSocketSession session, HashMap userInfo, String message) {
        if (userInfo.get("nickname").toString().equals("뮤로나봇")) {
            return;
        }

        List<CommandMessageEntity> commandList = messageRepository.findAll();

        HashMap<String, Object> extras = new HashMap<>();
        HashMap<String, Object> sendOptions = new HashMap<>();
        HashMap<String, Object> bdy = new HashMap<>();
        //HashMap<String, Object> author = new HashMap<>();

        extras.put("chatType", "STREAMING");
        extras.put("emojis", "");
        extras.put("osType", "PC");
        extras.put("extraToken", tokenInfo.get("extraToken"));
        extras.put("streamingChannelId", channelInfo.get("channelId"));

        bdy.put("msgTypeCode", 1);
        bdy.put("extras", gson.toJson(extras));
        bdy.put("msgTime", System.currentTimeMillis());

        sendOptions.put("ver", "2");
        sendOptions.put("svcid", svcid);
        sendOptions.put("cid", cid);
        sendOptions.put("tid", 3);
        sendOptions.put("cmd", ChatCmd.SEND_CHAT.getValue());
        sendOptions.put("retry", false);
        sendOptions.put("sid", sid);

        /* 명령어 권한 있을때 CUD*/

        if (userInfo.get("userRoleCode").toString().equals("") || userInfo.get("nickname").toString().equals("칠색딱따구리")) {
            String[] commandMessage = message.split(" ");

            switch (commandMessage[0]) {
                case "!추가":
                    if (commandMessage.length == 4) {
                        bdy.put("msg", commandMessage[1] + "명령어가 추가되었습니다.");
                        messageRepository.save(CommandMessageEntity.builder()
                                .cmdStr(commandMessage[1])
                                .cmdMsg(commandMessage[2].replaceAll("_", " "))
                                .nickNameUse(commandMessage[3].equalsIgnoreCase("true"))
                                .build());
                        sendOptions.put("bdy", bdy);
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    } else {
                        bdy.put("msg", "!추가 [커맨드] [응답] [대상여부] 형식으로 입력해주세요." +
                                "[응답 띄어쓰기는 _ 로 바꿔주세요]." +
                                "[대상여부는 빈 값일 경우 true, false 로 입력해주세요].");
                        sendOptions.put("bdy", bdy);
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    }
                    break;
                case "!수정":
                    if (commandMessage.length == 4) {
                        messageRepository.save(CommandMessageEntity.builder()
                                .cmdStr(commandMessage[1])
                                .cmdMsg(commandMessage[2].replaceAll("_", " "))
                                .nickNameUse(commandMessage[3].equalsIgnoreCase("true"))
                                .build());
                        bdy.put("msg", commandMessage[1] + " 명령어가 수정되었습니다.");
                        sendOptions.put("bdy", bdy);
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    } else {
                        bdy.put("msg", "!수정 [커맨드] [응답] [대상여부] 형식으로 입력해주세요." +
                                "[응답 띄어쓰기는 _ 로 바꿔주세요]." +
                                "[대상여부는 빈 값일 경우 true, false 로 입력해주세요].");
                        sendOptions.put("bdy", bdy);
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    }
                    break;
                case "!삭제":
                    if (commandMessage.length == 2) {
                        messageRepository.deleteById(commandMessage[1]);
                        bdy.put("msg", commandMessage[0] + " 명령어가 삭제 되었습니다.");
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    } else {
                        bdy.put("msg", "!삭제 [커맨드] 형식으로 입력해주세요.");
                        sendOptions.put("bdy", bdy);
                        session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                    }
                    break;
            }
        }


        if (commandList.stream()
                .anyMatch(commandMessageEntity -> message.split(" ")[0].equals(commandMessageEntity.getCmdStr()))
        ) {
            if (
                    commandList.stream()
                            .filter(commandMessageEntity -> message.split(" ")[0].equals(commandMessageEntity.getCmdStr()))
                            .findFirst()
                            .map(CommandMessageEntity::isNickNameUse)
                            .orElse(false)
            ) {
                bdy.put("msg", userInfo.get("nickname") +
                        "님 " +
                        commandList.stream()
                                .filter(commandMessageEntity -> message.split(" ")[0].equals(commandMessageEntity.getCmdStr()))
                                .findFirst().get().getCmdMsg()
                );
                sendOptions.put("bdy", bdy);
                session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
            } else {
                if (message.split(" ")[0].equals("!명령어")) {
                    bdy.put("msg", commandList.stream().map(CommandMessageEntity::getCmdStr).collect(Collectors.toList()));
                    sendOptions.put("bdy", bdy);
                    session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                } else {
                    bdy.put("msg",
                            commandList.stream()
                                    .filter(commandMessageEntity -> message.split(" ")[0].equals(commandMessageEntity.getCmdStr()))
                                    .findFirst().get().getCmdMsg()
                    );
                    sendOptions.put("bdy", bdy);
                    session.send(Mono.just(session.textMessage(gson.toJson(sendOptions)))).subscribe();
                }
            }
        }

    }

    private LinkedTreeMap<String, Object> processChannelSearch(HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData) {
        for (String key : searchChannelData.keySet()) {
            if (CONTENT.equals(key)) {
                LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>> content = searchChannelData.get(CONTENT);

                for (String contentKey : content.keySet()) {
                    if (DATA.equals(contentKey)) {
                        List<LinkedTreeMap<String, Object>> dataList = content.get(DATA);
                        LinkedTreeMap<String, Object> channelInfo = dataList.get(0);
                        log.info("search channelInfo : {}", channelInfo);
                        return (LinkedTreeMap<String, Object>) channelInfo.get("channel");
                    }
                }
            }
        }
        return null;
    }

}
