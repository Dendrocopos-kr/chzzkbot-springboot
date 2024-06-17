package org.dendrocopos.chzzkbot.chzzk.main;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatentity.DonationMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatentity.NormalMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatenum.ChatCommand;
import org.dendrocopos.chzzkbot.chzzk.chatservice.ChzzkServices;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.dendrocopos.chzzkbot.chzzk.repository.DonationMessageRepository;
import org.dendrocopos.chzzkbot.chzzk.repository.NormalMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMain {
    public static final String CONTENT = "content";
    public static final String DATA = "data";
    public static final String NICKNAME = "nickname";
    public static final String USER_ROLE_CODE = "userRoleCode";
    public static final String COMMAND_ADD = "!추가";
    public static final String COMMAND_MODIFY = "!수정";
    public static final String COMMAND_DELETE = "!삭제";
    public static final int CONSTANT_OF_LENGTH_FOR_DELETE = 2;
    public static final String SUCCESS_CODE = "200.0";
    public static final String COMMAND_ENTITY_TRUE = "true";
    public static final String COMMAND = "!명령어";
    public static final String UPTIME = "!업타임";
    public static final String SVCID_KEY = "svcid";
    public static final String CMD_KEY = "cmd";
    public static final String CID_KEY = "cid";
    public static final String BDY_KEY = "bdy";
    public static final String SID_KEY = "sid";
    public static final String DOT = "\\.";
    public static final String STATUS = "status";
    public static final String OPEN_DATE = "openDate";
    public static final String CLOSE_DATE = "closeDate";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final WebSocketClient websocketclient;
    private final ChzzkServices chzzkServices;
    private final Gson gson = new Gson();
    private final CommandMessageRepository messageRepository;
    private final DonationMessageRepository donationMessageRepository;
    private final NormalMessageRepository normalMessageRepository;
    @Getter
    public LinkedTreeMap channelInfoDetail;
    HashMap<String, Object> openWebSocketJson = new HashMap();
    HashMap<String, Object> bdy = new HashMap();
    private Disposable webSocketSessionDisposable;
    private LinkedTreeMap channelInfo;
    private LinkedTreeMap chatChannelInfo;
    private LinkedTreeMap tokenInfo;
    private LinkedTreeMap myInfo;
    private String svcid;
    private String cid;
    private String sid;
    @Value("${chzzk.ChannelName}")
    private String channelName;
    private boolean isWebSocketOpen = false;

    public boolean isWebSocketOpen() {
        return isWebSocketOpen;
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void startWebSocket() {
        channelInfo = fetchChannelInfo();
        myInfo = fetchUserStatus();
        chatChannelInfo = fetchChatChannelInfo();
        channelInfoDetail = fetchChannelDetail();
        tokenInfo = fetchToken();
        establishWebSocketConnection();
    }

    public LinkedTreeMap fetchChannelDetail() {
        String searchChannelDteail = chzzkServices.reqChzzk("service/v2/channels/" + channelInfo.get("channelId") + "/live-detail").block();
        HashMap channelDteail = gson.fromJson(searchChannelDteail, HashMap.class);
        if (channelDteail.get("code").toString().equals(SUCCESS_CODE)) {
            //log.debug("searchChannelInfoDetail : {}", channelDteail.get("content"));
            return channelInfoDetail = ((LinkedTreeMap) channelDteail.get("content"));
        }
        return null;
    }

    public LinkedTreeMap fetchChannelInfo() {
        String searchChannelInfo = chzzkServices.reqChzzk("service/v1/search/channels?keyword=" + channelName + "&offset=0&size=13&withFirstChannelContent=false")
                .block();
        //log.debug("channelSearch : {}", searchChannelInfo);
        HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData = gson.fromJson(searchChannelInfo, HashMap.class);
        return channelInfo = processChannelSearch(searchChannelData);
    }

    private LinkedTreeMap fetchUserStatus() {
        String searchMyInfo = chzzkServices.reqGame("nng_main/v1/user/getUserStatus").block();
        HashMap myInfoContent = gson.fromJson(searchMyInfo, HashMap.class);
        if (myInfoContent.get("code").toString().equals(SUCCESS_CODE)) {
            log.debug("search myInfo : {}", myInfoContent.get("content"));
            return ((LinkedTreeMap) myInfoContent.get("content"));
        }
        return null;
    }

    private LinkedTreeMap fetchChatChannelInfo() {
        String searchChatChannelInfo = chzzkServices.getStatus("polling/v2/channels/" + channelInfo.get("channelId") + "/live-status").block();
        HashMap searchChatChannel = gson.fromJson(searchChatChannelInfo, HashMap.class);
        if (searchChatChannel.get("code").toString().equals(SUCCESS_CODE)) {
            log.debug("search chatChannelInfo : {}", searchChatChannel.get("content"));
            return ((LinkedTreeMap) searchChatChannel.get("content"));
        }
        return null;
    }

    private LinkedTreeMap fetchToken() {
        String searchTokenInfo = chzzkServices.reqGame("nng_main/v1/chats/access-token?channelId=" + chatChannelInfo.get("chatChannelId") + "&chatType=STREAMING").block();
        HashMap searchToken = gson.fromJson(searchTokenInfo, HashMap.class);
        if (searchToken.get("code").toString().equals(SUCCESS_CODE)) {
            log.debug("search tokenInfo : {}", searchToken.get("content"));
            return ((LinkedTreeMap) searchToken.get("content"));
        }
        return null;
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
        try {
            processSendMessage(gson.toJson(openWebSocketJson));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    // 웹소켓 연결 종료 코드
    public void stopWebSocketConnection() {
        if (webSocketSessionDisposable != null && !webSocketSessionDisposable.isDisposed()) {
            webSocketSessionDisposable.dispose();
            isWebSocketOpen = false;
        }
    }

    public void processSendMessage(String message) {

        int serverId = Math.abs(chatChannelInfo.get("chatChannelId").toString().chars()
                .reduce(0, Integer::sum)) % 9 + 1;
        HashMap pongCmd = new HashMap();
        pongCmd.put("cmd", ChatCommand.PONG.getValue());
        pongCmd.put("ver", "2");

        webSocketSessionDisposable = websocketclient.execute(
                URI.create("wss://kr-ss" + serverId + ".chat.naver.com/chat"),
                session -> {
                    isWebSocketOpen = true;
                    return Flux.merge(
                            // Periodic message sending every 20 seconds
                            session.send(Mono.just(session.textMessage(message))) // 연결 설정 후 초기 메시지 전송
                                    .thenMany(
                                            Flux.interval(Duration.ofSeconds(20)) // Interval setup
                                                    .flatMap(time -> session.send(
                                                            Mono.just(session.textMessage(gson.toJson(pongCmd))))
                                                    ) // Message sending every 20 seconds

                                    ),
                            // Handling received messages from server
                            session.receive() // 메시지 받기
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(s -> processReceivedMessage(session, s)) // 받은 메시지 처리하기
                    ).doOnCancel(() -> {
                        // this block will be executed when the subscription is cancelled
                        sendMessageToUser(session, "나님 자러갈게...", initializeMessageSendOptions());
                        isWebSocketOpen = false;
                    }).then();
                }
        ).subscribe();
    }

    public void processReceivedMessage(WebSocketSession session, String receivedMessage) {
        log.debug("Received WebSocket message: {}", receivedMessage);
        HashMap<String, Object> messageContent = convertMessageToMap(receivedMessage);

        int commandId = fetchCommandIdFrom(messageContent);
        if (messageContent.get(SVCID_KEY) != null) {
            svcid = messageContent.get(SVCID_KEY).toString();
        }
        if (messageContent.get(CID_KEY) != null) {
            cid = messageContent.get(CID_KEY).toString();
        }
        if (messageContent.get(BDY_KEY) != null
                && messageContent.get(BDY_KEY) instanceof LinkedTreeMap
                && ((LinkedTreeMap) messageContent.get(BDY_KEY)).get(SID_KEY) != null) {
            sid = ((LinkedTreeMap) messageContent.get(BDY_KEY)).get(SID_KEY).toString();
        }

        performRequestBasedOn(commandId, messageContent, session);
    }

    private HashMap<String, Object> convertMessageToMap(String message) {
        return gson.fromJson(message, HashMap.class);
    }

    private int fetchCommandIdFrom(Map<String, Object> messageContent) {
        return Integer.parseInt(messageContent.get(CMD_KEY).toString().split(DOT)[0]);
    }

    private void performRequestBasedOn(Integer command, Map<String, Object> messageContent, WebSocketSession session) {

        switch (ChatCommand.getCommandValue(command)) {
            case ChatCommand.PING:
                logInfoFor(ChatCommand.PING);
                break;
            case ChatCommand.PONG:
                logInfoFor(ChatCommand.PONG);
                break;
            case ChatCommand.CONNECT:
                logInfoFor(ChatCommand.CONNECT);
                break;
            case ChatCommand.CONNECTED:
                logInfoFor(ChatCommand.CONNECTED);
                bdy.put("sid", ((LinkedTreeMap) messageContent.get("bdy")).get("sid"));
                openWebSocketJson.put("bdy", bdy);
                log.debug("openWebSocketJson : {}", openWebSocketJson);
                session.send(Mono.just(session.textMessage(gson.toJson(openWebSocketJson))));

                sendMessageToUser(session, "나님 등장!", initializeMessageSendOptions());
                break;
            case ChatCommand.REQUEST_RECENT_CHAT:
                logInfoFor(ChatCommand.REQUEST_RECENT_CHAT);
                break;
            case ChatCommand.RECENT_CHAT:
                logInfoFor(ChatCommand.RECENT_CHAT);
                break;
            case ChatCommand.EVENT:
                logInfoFor(ChatCommand.EVENT);
                break;
            case ChatCommand.CHAT:
                log.info("CHAT :{} : {} : {}",
                        ChatCommand.CHAT.getValue(),
                        (gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("profile"), HashMap.class)).get("nickname")
                        , ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg")
                );
                normalMessageRepository.save(NormalMessageEntity.builder()
                        .nickName((gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("profile"), HashMap.class)).get("nickname").toString())
                        .msg(((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg").toString())
                        .build());
                sendCommandMessage(session, gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("profile"), HashMap.class), ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg").toString());
                break;
            case ChatCommand.DONATION:
                log.info("DONATION : {}", ChatCommand.DONATION.getValue());
                try {
                    HashMap<String, Object> extras = (HashMap<String, Object>) gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class);
                    String donationType = null;
                    if (extras.get("donationType") != null) {
                        donationType = extras.get("donationType").toString();
                    }
                    
                    donationMessageRepository.save(DonationMessageEntity.builder()
                            .nickName((gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("profile"), HashMap.class)).get("nickname").toString())
                            .msg(((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("msg").toString())
                            .donationType(donationType)
                            .cost((gson.fromJson((String) ((LinkedTreeMap) ((ArrayList) messageContent.get("bdy")).get(0)).get("extras"), HashMap.class)).get("payAmount").toString())
                            .build());
                } catch (Exception e) {
                    log.info("{}", e.getMessage());
                }
                break;
            case ChatCommand.KICK:
                logInfoFor(ChatCommand.KICK);
                break;
            case ChatCommand.BLOCK:
                logInfoFor(ChatCommand.BLOCK);
                break;
            case ChatCommand.BLIND:
                logInfoFor(ChatCommand.BLIND);
                break;
            case ChatCommand.NOTICE:
                logInfoFor(ChatCommand.NOTICE);
                break;
            case ChatCommand.PENALTY:
                logInfoFor(ChatCommand.PENALTY);
                break;
            case ChatCommand.SEND_CHAT:
                logInfoFor(ChatCommand.SEND_CHAT);
                break;
            case ChatCommand.MEMBER_SYNC:
                logInfoFor(ChatCommand.MEMBER_SYNC);
                break;
            default:
                log.debug("messageContent : {}", messageContent);
                log.debug("Unknown command : {}", command);
                break;
        }
    }

    private void logInfoFor(ChatCommand command) {
        log.debug("{} : {}", command.name(), command.getValue());
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
        if (commandList.stream().anyMatch(commandMessageEntity -> commandInputMessage.equals(commandMessageEntity.getCmdStr()))) {
            if (isCommandUsesNickname(commandInputMessage, commandList)) {
                String responseMessage = userInfo.get(NICKNAME) + "님 " + getCommandMessage(commandInputMessage, commandList);
                sendMessageToUser(session, responseMessage, messageSendOptionsReference);
            } else {
                processCommand(commandInputMessage, commandList, session, messageSendOptionsReference);
            }
        }
    }

    private void processCommand(String commandInputMessage, List<CommandMessageEntity> commandList, WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        switch (commandInputMessage) {
            case COMMAND:
                executeCommands(commandList, session, messageSendOptionsReference);
                break;
            case UPTIME:
                executeUptime(session, messageSendOptionsReference);
                break;
            default:
                String responseMessage = getCommandMessage(commandInputMessage, commandList);
                sendMessageToUser(session, responseMessage, messageSendOptionsReference);
                break;
        }
    }

    private void executeCommands(List<CommandMessageEntity> commandList, WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        List<String> filteredCommands = commandList.stream()
                .map(CommandMessageEntity::getCmdStr)
                .filter(s -> !s.equals(COMMAND))
                .toList();
        sendMessageToUser(session, filteredCommands.isEmpty() ? "No commands are available." : String.join(", ", filteredCommands), messageSendOptionsReference);
    }

    private void executeUptime(WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        String upTimeMessage = "";
        boolean isOpen = channelInfoDetail.get(STATUS).toString().equalsIgnoreCase("OPEN");
        if (isOpen) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
            LocalDateTime openDate = LocalDateTime.parse(channelInfoDetail.get(OPEN_DATE).toString(), formatter);
            Duration duration = Duration.between(openDate, LocalDateTime.now());
            upTimeMessage = getFormattedUptimeMessage(duration);
        }
        sendMessageToUser(session, upTimeMessage, messageSendOptionsReference);
    }

    private String getFormattedUptimeMessage(Duration duration) {
        StringBuilder upTimeMessage = new StringBuilder();
        if (duration.toDays() > 0) {
            upTimeMessage.append(duration.toDays()).append("일 ");
        }
        if (duration.toHours() > 0) {
            upTimeMessage.append(duration.toHours() % 24).append("시간 ");
        }
        if (duration.toMinutes() > 0) {
            upTimeMessage.append(duration.toMinutes() % 60).append("분 ");
        }
        if (duration.getSeconds() > 0) {
            upTimeMessage.append(duration.getSeconds() % 60).append("초 방송중");
        }
        return upTimeMessage.toString();
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
        return userInfo.get(NICKNAME).toString().equals("뮤쪽이");
    }

    private boolean hasCommandPermission(HashMap userInfo) {
        return userInfo.get(USER_ROLE_CODE).toString().equals("streamer") || userInfo.get(NICKNAME).toString().equals("칠색딱따구리");
    }

    private AtomicReference<HashMap<String, Object>> initializeMessageSendOptions() {
        HashMap<String, Object> messageSendOptions = new HashMap<>();
        messageSendOptions.put("ver", "2");
        messageSendOptions.put("svcid", svcid);
        messageSendOptions.put("cid", cid);
        messageSendOptions.put("tid", 3);
        messageSendOptions.put("cmd", ChatCommand.SEND_CHAT.getValue());
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
        if (commandArguments.length == 3 || commandArguments.length == 4) {
            String command = commandArguments[1].replaceAll("_", " ");
            String response = commandArguments[2].replaceAll("_", " ");
            boolean nicknameUse = false;
            if (commandArguments.length == 4) {
                nicknameUse = commandArguments[3].equalsIgnoreCase(COMMAND_ENTITY_TRUE);
            }

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
                "[띄어쓰기는 _ 로 바꿔주세요]." +
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
                        log.debug("search channelInfo : {}", channelInfo);
                        return (LinkedTreeMap<String, Object>) channelInfo.get("channel");
                    }
                }
            }
        }
        return null;
    }

}
