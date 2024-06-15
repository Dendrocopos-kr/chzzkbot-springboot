package org.dendrocopos.chzzkbot.chzzk.main;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMain {
    private static final String CONTENT = "content";
    private static final String DATA = "data";
    private static final String NICKNAME = "nickname";
    private static final String USER_ROLE_CODE = "userRoleCode";
    private static final String COMMAND_ADD = "!추가";
    private static final String COMMAND_MODIFY = "!수정";
    private static final String COMMAND_DELETE = "!삭제";
    private static final int CONSTANTS_OF_LENGTH_FOR_ADD_OR_MODIFY = 4;
    private static final int CONSTANT_OF_LENGTH_FOR_DELETE = 2;
    private static final String SUCCESS_CODE = "200.0";
    private static final String COMMAND_ENTITY_TRUE = "true";
    final String SVCID_KEY = "svcid";
    final String CMD_KEY = "cmd";
    final String CID_KEY = "cid";
    final String BDY_KEY = "bdy";
    final String SID_KEY = "sid";
    final String DOT = "\\.";
    private final ChzzkServices chzzkServices;
    private final Gson gson = new Gson();
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
        fetchChannelInfo();
        fetchUserStatus();
        fetchChatChannelInfo();
        fetchToken();
        establishWebSocketConnection();
    }

    private void fetchChannelInfo() {
        String searchChannelInfo = chzzkServices.reqChzzk("service/v1/search/channels?keyword=" + channelName + "&offset=0&size=13&withFirstChannelContent=false")
                .block();
        log.info("channelSearch : {}", searchChannelInfo);
        HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData = gson.fromJson(searchChannelInfo, HashMap.class);
        channelInfo = processChannelSearch(searchChannelData);
    }

    private void fetchUserStatus() {
        String searchMyInfo = chzzkServices.reqGame("nng_main/v1/user/getUserStatus").block();
        HashMap myInfoContent = gson.fromJson(searchMyInfo, HashMap.class);
        if (myInfoContent.get("code").toString().equals(SUCCESS_CODE)) {
            myInfo = ((LinkedTreeMap) myInfoContent.get("content"));
            log.info("search myInfo : {}", myInfo);
        }
    }

    private void fetchChatChannelInfo() {
        String searchChatChannelInfo = chzzkServices.getStatus("polling/v2/channels/" + channelInfo.get("channelId") + "/live-status").block();
        HashMap searchChatChannel = gson.fromJson(searchChatChannelInfo, HashMap.class);
        if (searchChatChannel.get("code").toString().equals(SUCCESS_CODE)) {
            chatChannelInfo = ((LinkedTreeMap) searchChatChannel.get("content"));
            log.info("search chatChannelInfo : {}", chatChannelInfo);
        }
    }

    private void fetchToken() {
        String searchTokenInfo = chzzkServices.reqGame("nng_main/v1/chats/access-token?channelId=" + chatChannelInfo.get("chatChannelId") + "&chatType=STREAMING").block();
        HashMap searchToken = gson.fromJson(searchTokenInfo, HashMap.class);
        if (searchToken.get("code").toString().equals(SUCCESS_CODE)) {
            tokenInfo = ((LinkedTreeMap) searchToken.get("content"));
            log.info("search tokenInfo : {}", tokenInfo);
        }
    }

    private void establishWebSocketConnection() {
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
        int cmd = Integer.parseInt(messageInfo.get(CMD_KEY).toString().split(DOT)[0]);

        if (messageInfo.get(SVCID_KEY) != null) {
            svcid = messageInfo.get(SVCID_KEY).toString();
        }
        if (messageInfo.get(CID_KEY) != null) {
            cid = messageInfo.get(CID_KEY).toString();
        }
        if (messageInfo.get(BDY_KEY) != null
                && messageInfo.get(BDY_KEY) instanceof LinkedTreeMap
                && ((LinkedTreeMap) messageInfo.get(BDY_KEY)).get(SID_KEY) != null) {
            sid = ((LinkedTreeMap) messageInfo.get(BDY_KEY)).get(SID_KEY).toString();
        }

        switch (ChatCmd.getCommand(cmd)) {
            case ChatCmd.PING:
                log.info("PING : {}", ChatCmd.PING.getValue());
                break;
            case ChatCmd.PONG:
                log.info("PONG : {}", ChatCmd.PONG.getValue());
                break;
            case ChatCmd.CONNECT:
                log.info("CONNECT : {}", ChatCmd.CONNECT.getValue());
                break;
            case ChatCmd.CONNECTED:
                log.info("CONNECTED : {}", ChatCmd.CONNECTED.getValue());
                bdy.put("sid", ((LinkedTreeMap) messageInfo.get("bdy")).get("sid"));
                openWebSocketJson.put("bdy", bdy);
                log.info("openWebSocketJson : {}", openWebSocketJson);
                session.send(Mono.just(session.textMessage(gson.toJson(openWebSocketJson))));
                break;
            case ChatCmd.REQUEST_RECENT_CHAT:
                log.info("REQUEST_RECENT_CHAT : {}", ChatCmd.REQUEST_RECENT_CHAT.getValue());
                break;
            case ChatCmd.RECENT_CHAT:
                log.info("RECENT_CHAT : {}", ChatCmd.RECENT_CHAT.getValue());
                break;
            case ChatCmd.EVENT:
                log.info("EVENT : {}", ChatCmd.EVENT.getValue());
                break;
            case ChatCmd.CHAT:
                log.info("CHAT :{} : {} : {}",
                        ChatCmd.CHAT.getValue(),
                        (gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("profile"), HashMap.class)).get("nickname")
                        , ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("msg")
                );
                sendCommandMessage(session, gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("profile"), HashMap.class), ((LinkedTreeMap) ((ArrayList) messageInfo.get("bdy")).get(0)).get("msg").toString());

                break;
            case ChatCmd.DONATION:
                log.info("DONATION : {}", ChatCmd.DONATION.getValue());
                break;
            case ChatCmd.KICK:
                log.info("KICK : {}", ChatCmd.KICK.getValue());
                break;
            case ChatCmd.BLOCK:
                log.info("BLOCK : {}", ChatCmd.BLOCK.getValue());
                break;
            case ChatCmd.BLIND:
                log.info("BLIND : {}", ChatCmd.BLIND.getValue());
                break;
            case ChatCmd.NOTICE:
                log.info("NOTICE : {}", ChatCmd.NOTICE.getValue());
                break;
            case ChatCmd.PENALTY:
                log.info("PENALTY : {}", ChatCmd.PENALTY.getValue());
                break;
            case ChatCmd.SEND_CHAT:
                log.info("SEND_CHAT : {}", ChatCmd.SEND_CHAT.getValue());
                break;
            default:
                log.info("Unknown command : {}", ChatCmd.getCommand(cmd));
                break;
        }

    }

    private void sendCommandMessage(WebSocketSession session, HashMap userInfo, String commandInputMessage) {
        if (isSpecialUser(userInfo)) {
            return;
        }
        List<CommandMessageEntity> commandList = messageRepository.findAll();

        final AtomicReference<HashMap<String, Object>> messageSendOptions = initializeMessageSendOptions(); // Final

        if (hasCommandPermission(userInfo)) {
            String[] commandArguments = commandInputMessage.split(" ");
            String commandType = commandArguments[0];

            switch (commandType) {
                case COMMAND_ADD:
                case COMMAND_MODIFY:
                    handleAddOrModifyCommand(session, commandArguments, messageSendOptions);
                    break;
                case COMMAND_DELETE:
                    handleDeleteCommand(session, commandArguments, messageSendOptions);
                    break;
            }
        }
        checkForCommand(commandInputMessage, commandList, session, messageSendOptions, userInfo);
    }

    private void checkForCommand(String commandInputMessage, List<CommandMessageEntity> commandList, WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference, HashMap userInfo) {
        String command = commandInputMessage.split(" ")[0];

        if (commandList.stream().anyMatch(commandMessageEntity -> command.equals(commandMessageEntity.getCmdStr()))) {
            if (isCommandUsesNickname(command, commandList)) {
                String responseMessage = userInfo.get("nickname") + "님 " + getCommandMessage(command, commandList);
                sendMessageToUser(session, responseMessage, messageSendOptionsReference);
            } else {
                if (command.equals("!명령어")) {
                    String allCommands = commandList.stream().map(CommandMessageEntity::getCmdStr).collect(Collectors.joining(", "));
                    sendMessageToUser(session, allCommands, messageSendOptionsReference);
                } else {
                    String responseMessage = getCommandMessage(command, commandList);
                    sendMessageToUser(session, responseMessage, messageSendOptionsReference);
                }
            }
        }
    }

    private String getCommandMessage(String command, List<CommandMessageEntity> commandList) {
        return commandList.stream()
                .filter(commandMessageEntity -> command.equals(commandMessageEntity.getCmdStr()))
                .findFirst()
                .map(CommandMessageEntity::getCmdMsg)
                .orElse(null);
    }

    private boolean isCommandUsesNickname(String command, List<CommandMessageEntity> commandList) {
        return commandList.stream()
                .filter(commandMessageEntity -> command.equals(commandMessageEntity.getCmdStr()))
                .findFirst()
                .map(CommandMessageEntity::isNickNameUse)
                .orElse(false);
    }

    private boolean isSpecialUser(HashMap userInfo) {
        return userInfo.get(NICKNAME).toString().equals("뮤로나봇");
    }

    private boolean hasCommandPermission(HashMap userInfo) {
        return userInfo.get(USER_ROLE_CODE).toString().equals("") || userInfo.get(NICKNAME).toString().equals("칠색딱따구리");
    }

    private AtomicReference<HashMap<String, Object>> initializeMessageSendOptions() {
        HashMap<String, Object> messageSendOptions = new HashMap<>();
        messageSendOptions.put("ver", "2");
        messageSendOptions.put("svcid", svcid);
        messageSendOptions.put("cid", cid);
        messageSendOptions.put("tid", 3);
        messageSendOptions.put("cmd", ChatCmd.SEND_CHAT.getValue());
        messageSendOptions.put("retry", false);
        messageSendOptions.put("sid", sid);
        return new AtomicReference<>(messageSendOptions);
    }

    private void handleDeleteCommand(WebSocketSession session, String[] commandArguments, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        if (commandArguments.length == CONSTANT_OF_LENGTH_FOR_DELETE) {
            String command = commandArguments[1];
            messageRepository.deleteById(command);

            String feedbackMessage = command + " 명령어가 삭제 되었습니다.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        } else {
            String feedbackMessage = "!삭제 [커맨드] 형식으로 입력해주세요.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        }
    }

    private void handleAddOrModifyCommand(WebSocketSession session, String[] commandArguments, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        if (commandArguments.length == CONSTANTS_OF_LENGTH_FOR_ADD_OR_MODIFY) {
            String command = commandArguments[1];
            String response = commandArguments[2].replaceAll("_", " ");
            boolean nicknameUse = commandArguments[3].equalsIgnoreCase(COMMAND_ENTITY_TRUE);

            messageRepository.save(CommandMessageEntity.builder()
                    .cmdStr(command)
                    .cmdMsg(response)
                    .nickNameUse(nicknameUse)
                    .build());

            String feedbackMessage = command + " 명령어가 수정되었습니다.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        } else {
            String feedbackMessage = getUsageMessageForModifyOrAddCommand();
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        }
    }

    private String getUsageMessageForModifyOrAddCommand() {
        return "!추가 [커맨드] [응답] [대상여부] 형식으로 입력해주세요." +
                "[응답 띄어쓰기는 _ 로 바꿔주세요]." +
                "[대상여부는 빈 값일 경우 true, false 로 입력해주세요].";
    }

    private void sendMessageToUser(WebSocketSession session, String msg, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        HashMap<String, Object> messageBody = initializeMessageBody();
        messageBody.put("msg", msg);
        messageSendOptionsReference.get().put("bdy", messageBody);
        session.send(Mono.just(session.textMessage(gson.toJson(messageSendOptionsReference.get())))).subscribe();
    }

    private HashMap<String, Object> initializeMessageBody() {
        HashMap<String, Object> messageExtras = initializeMessageExtras();
        HashMap<String, Object> messageBody = new HashMap<>();
        messageBody.put("msgTypeCode", 1);
        messageBody.put("extras", gson.toJson(messageExtras));
        messageBody.put("msgTime", System.currentTimeMillis());
        return messageBody;
    }

    private HashMap<String, Object> initializeMessageExtras() {
        HashMap<String, Object> messageExtras = new HashMap<>();
        messageExtras.put("chatType", "STREAMING");
        messageExtras.put("emojis", "");
        messageExtras.put("osType", "PC");
        messageExtras.put("extraToken", tokenInfo.get("extraToken"));
        messageExtras.put("streamingChannelId", channelInfo.get("channelId"));
        return messageExtras;
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
