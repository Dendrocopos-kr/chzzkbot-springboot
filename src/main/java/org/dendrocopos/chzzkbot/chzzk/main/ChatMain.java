package org.dendrocopos.chzzkbot.chzzk.main;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatenum.ChatCommand;
import org.dendrocopos.chzzkbot.chzzk.chatservice.ChzzkServices;
import org.dendrocopos.chzzkbot.chzzk.chatservice.MessageService;
import org.dendrocopos.chzzkbot.chzzk.repository.DonationMessageRepository;
import org.dendrocopos.chzzkbot.chzzk.repository.NormalMessageRepository;
import org.dendrocopos.chzzkbot.core.utils.StringUtils;
import org.dendrocopos.chzzkbot.ollama.config.OllamaMessage;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.core.component.OllamaClient;
import org.dendrocopos.chzzkbot.chzzk.manager.AuthorizationManager;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.dendrocopos.chzzkbot.chzzk.utils.Constants.*;
import static org.dendrocopos.chzzkbot.chzzk.utils.EntityUtils.*;
import static org.dendrocopos.chzzkbot.core.utils.StringUtils.appendTimeUnit;
import static org.dendrocopos.chzzkbot.ollama.utils.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMain {
    private final WebSocketClient websocketclient;
    private final ChzzkServices chzzkServices;
    private final Gson gson;
    private final CommandMessageRepository messageRepository;
    private final AuthorizationManager authorizationManager;
    private final MessageService messageService;
    private final NormalMessageRepository normalMessageRepository;
    private final DonationMessageRepository donationMessageRepository;
    private final OllamaClient ollamaClient;

    @Getter
    public LinkedTreeMap channelInfoDetail;
    HashMap<String, Object> openWebSocketJson = new HashMap<>();
    HashMap<String, Object> bdy = new HashMap<>();
    private Disposable webSocketSessionDisposable;
    private LinkedTreeMap channelInfo;
    private LinkedTreeMap chatChannelInfo;
    private LinkedTreeMap tokenInfo;
    private LinkedTreeMap myInfo;
    private String svcid;
    private String cid;
    private String sid;

    @Value("${chzzk.ChannelName}")
    public String channelName;
    @Value("${chzzk.bot.name}")
    public String botName;
    @Value("${chzzk.bot.openingMessage}")
    public String announcementMessage;
    @Value("${chzzk.bot.closingMessage}")
    public String closingMessage;
    @Value("${ollama.callCommand}")
    public String COMMAND_AI;

    private boolean isWebSocketOpen = false;
    private int serverId;

    public boolean isWebSocketOpen() {
        return isWebSocketOpen;
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void startWebSocket() {
        Map<String, Object> body = createWebSocketBody();
        String jsonResponse = gson.toJson(body);
        try {
            processSendMessage(jsonResponse);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private Map<String, Object> createWebSocketBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("accTkn", tokenInfo.get("accessToken"));
        body.put("auth", "SEND");
        body.put("devType", 2001);
        body.put("uid", myInfo.get("userIdHash"));

        HashMap<String, Object> openWebSocketJson = new HashMap<>();
        openWebSocketJson.put("bdy", body);
        openWebSocketJson.put("cid", chatChannelInfo.get("chatChannelId"));
        openWebSocketJson.put("svcid", "game");
        openWebSocketJson.put("ver", "2");
        openWebSocketJson.put("tid", 1);
        openWebSocketJson.put("cmd", 100);
        return openWebSocketJson;
    }

    public boolean fetchChannelDetail() {
        try {
            log.info("🔍 채널 상세 정보 조회 중...");

            // ✅ API 요청 및 응답 받기
            String searchChannelDetail = chzzkServices.reqChzzk(
                    "service/v2/channels/" + channelInfo.get("channelId") + "/live-detail"
            ).block();

            if (searchChannelDetail == null) {
                log.warn("⚠️ 채널 상세 정보 조회 실패: 응답이 null입니다.");
                channelInfoDetail = null;
                return false;
            }

            // ✅ JSON 파싱
            HashMap<String, Object> channelDetail = gson.fromJson(searchChannelDetail, HashMap.class);

            // ✅ 응답 코드 검증
            if (SUCCESS_CODE.equals(channelDetail.get("code").toString())) {
                channelInfoDetail = (LinkedTreeMap) channelDetail.get("content");
                log.info("✅ 채널 상세 정보 조회 성공");
                return true;
            } else {
                log.warn("⚠️ 채널 상세 정보 조회 실패: 응답 코드 {}", channelDetail.get("code"));
                channelInfoDetail = null;
                return false;
            }
        } catch (Exception e) {
            log.error("❌ 채널 상세 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            channelInfoDetail = null;
            return false;
        }
    }


    public boolean fetchChannelInfo() {
        try {
            String searchChannelInfo = chzzkServices.reqChzzk("service/v1/search/channels?keyword=" + channelName + "&offset=0&size=13&withFirstChannelContent=false")
                    .block();

            log.debug("channelSearch : {}", searchChannelInfo);

            if (searchChannelInfo == null || searchChannelInfo.isBlank()) {
                log.warn("⚠ 채널 검색 결과가 비어 있습니다. (channelName: {})", channelName);
                return false;
            }

            HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData = gson.fromJson(searchChannelInfo, HashMap.class);
            channelInfo = processChannelSearch(searchChannelData);

            return true;
        } catch (Exception e) {
            log.error("❌ 채널 정보를 가져오는 중 오류 발생 (channelName: {})", channelName, e);
            return false;
        }
    }


    public boolean fetchUserStatus() {
        try {
            String searchMyInfo = chzzkServices.reqGame("nng_main/v1/user/getUserStatus").block();

            if (searchMyInfo == null || searchMyInfo.isBlank()) {
                log.warn("⚠ 사용자 상태 정보를 가져올 수 없습니다.");
                return false;
            }

            HashMap<String, Object> myInfoContent = gson.fromJson(searchMyInfo, HashMap.class);

            if (SUCCESS_CODE.equals(myInfoContent.get("code").toString())) {
                //log.info("✅ 검색된 사용자 정보: {}", myInfoContent.get("content"));
                myInfo = (LinkedTreeMap<String, Object>) myInfoContent.get("content");
                return true;
            } else {
                log.warn("⚠ 사용자 상태 요청 실패: {}", myInfoContent);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ 사용자 상태 정보를 가져오는 중 오류 발생", e);
            return false;
        }
    }


    public boolean fetchChatChannelInfo() {
        try {
            String searchChatChannelInfo = chzzkServices.getStatus("polling/v2/channels/" + channelInfo.get("channelId") + "/live-status").block();

            if (searchChatChannelInfo == null || searchChatChannelInfo.isBlank()) {
                log.warn("⚠ 채팅 채널 정보 응답이 비어 있습니다.");
                return false;
            }

            HashMap<String, Object> searchChatChannel = gson.fromJson(searchChatChannelInfo, HashMap.class);

            if (searchChatChannel != null && SUCCESS_CODE.equals(searchChatChannel.get("code").toString())) {
                //log.info("✅ 채팅 채널 정보 검색 결과: {}", searchChatChannel.get("content"));
                log.info("✅ 채팅 채널 정보 검색 성공");
                chatChannelInfo = (LinkedTreeMap<String, Object>) searchChatChannel.get("content");
                return true;
            } else {
                log.warn("⚠ 채팅 채널 정보 검색 실패: {}", searchChatChannel);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ 채팅 채널 정보를 가져오는 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }


    public boolean fetchToken() {
        try {
            String searchTokenInfo = chzzkServices.reqGame("nng_main/v1/chats/access-token?channelId="
                    + chatChannelInfo.get("chatChannelId") + "&chatType=STREAMING").block();

            if (searchTokenInfo == null || searchTokenInfo.isBlank()) {
                log.warn("⚠️ API 응답이 비어있음.");
                return false;
            }

            HashMap searchToken = gson.fromJson(searchTokenInfo, HashMap.class);

            if (searchToken.containsKey("code") && SUCCESS_CODE.equals(searchToken.get("code").toString())) {
                //log.info("✅ 토큰 검색 성공: {}", searchToken.get("content"));
                tokenInfo = (LinkedTreeMap) searchToken.get("content");
                return true;
            } else {
                log.warn("⚠️ 토큰 검색 실패: 응답 코드 = {}", searchToken.get("code"));
                return false;
            }

        } catch (JsonSyntaxException e) {
            log.error("❌ JSON 파싱 오류: {}", e.getMessage(), e);
            return false;
        } catch (WebClientResponseException e) {
            log.error("❌ 웹 요청 실패 (HTTP 상태 코드: {}): {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ 토큰 조회 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }


    // 웹소켓 연결 종료 코드
    public void stopWebSocketConnection() {
        if (webSocketSessionDisposable != null && !webSocketSessionDisposable.isDisposed()) {
            webSocketSessionDisposable.dispose();
        }
        isWebSocketOpen = false;
    }

    public boolean isServerIdChange() {
        try {
            if (this.serverId == 0) {
                log.warn("⚠️ 서버 ID가 0입니다. 새로운 서버 ID를 계산합니다.");
                this.serverId = calculateServerId();
                return true; // ID가 설정되지 않은 경우 변경 필요
            }

            int calculatedServerId = calculateServerId();
            boolean isChanged = calculatedServerId == this.serverId;

            log.info("🔄 서버 ID 확인: 기존={}, 새 계산={}", this.serverId, calculatedServerId);
            return isChanged;
        } catch (Exception e) {
            log.error("❌ 서버 ID 검증 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    private int calculateServerId() {
        try {
            return Math.abs(chatChannelInfo.get("chatChannelId").toString().chars()
                    .reduce(0, Integer::sum)) % 9 + 1;
        } catch (Exception e) {
            log.error("❌ 서버 ID 계산 오류: {}", e.getMessage(), e);
            return -1; // 서버 ID 계산에 실패하면 -1 반환
        }
    }

    private HashMap<String, Object> constructPongCmd() {
        HashMap<String, Object> pongCmd = new HashMap<>();
        pongCmd.put("cmd", ChatCommand.PONG.getValue());
        pongCmd.put("ver", "2");
        return pongCmd;
    }

    public void processSendMessage(String message) {
        serverId = calculateServerId();
        String uriString = "wss://kr-ss" + serverId + ".chat.naver.com/chat";
        webSocketSessionDisposable = websocketclient.execute(
                URI.create(uriString),
                session -> {
                    isWebSocketOpen = true;
                    return Flux.merge(
                            getPeriodicMessageFlux(session, message),
                            getReceivedMessageFlux(session)
                    ).doOnCancel(() -> {
                        // this block will be executed when the subscription is cancelled
                        sendMessageToUser(session, closingMessage, initializeMessageSendOptions());
                        isWebSocketOpen = false;
                    }).then();
                }
        ).subscribe();
    }

    private Flux<Void> getPeriodicMessageFlux(WebSocketSession session, String message) {
        return session.send(Mono.just(session.textMessage(message))) // 연결 설정 후 초기 메시지 전송
                .thenMany(
                        Flux.interval(Duration.ofSeconds(20)) // Interval setup
                                .flatMap(time -> session.send(
                                        Mono.just(session.textMessage(gson.toJson(constructPongCmd())))
                                ))
                );
    }

    private Flux<String> getReceivedMessageFlux(WebSocketSession session) {
        return session.receive() // 메시지 받기
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(s -> processReceivedMessage(session, s)); // 받은 메시지 처리하기
    }

    public void processReceivedMessage(WebSocketSession session, String receivedMessage) {
        log.info("Received WebSocket message: {}", receivedMessage);
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
        Optional<ChatCommand> optionalCommand = ChatCommand.getCommand(command);

        if (optionalCommand.isPresent()) {
            ChatCommand chatCommand = optionalCommand.get();

            switch (chatCommand) {
                case PING, PONG, CONNECT, REQUEST_RECENT_CHAT, RECENT_CHAT, EVENT,
                     KICK, BLOCK, BLIND, NOTICE, PENALTY, SEND_CHAT, MEMBER_SYNC,SUCCESS:
                    log.info("[COMMAND] {} ({}) 실행", chatCommand.name(), chatCommand.getValue());
                    break;

                case CONNECTED:
                    log.info("[COMMAND] {} ({}): 연결됨", chatCommand.name(), chatCommand.getValue());

                    // WebSocket 세션 ID 저장
                    bdy.put("sid", ((LinkedTreeMap<?, ?>) messageContent.get("bdy")).get("sid"));
                    openWebSocketJson.put("bdy", bdy);

                    log.debug("[WEBSOCKET] 초기 연결 데이터: {}", openWebSocketJson);

                    // WebSocket 메시지 전송
                    session.send(Mono.just(session.textMessage(gson.toJson(openWebSocketJson))));

                    // 사용자에게 안내 메시지 전송
                    sendMessageToUser(session, announcementMessage, initializeMessageSendOptions());
                    break;

                case CHAT:
                    handleChatCommand(messageContent, session);
                    break;

                case DONATION:
                    handleDonationCommand(messageContent);
                    break;

                default:
                    log.warn("[WARNING] 알 수 없는 명령어 수신: {} (데이터: {})", command, messageContent);
                    break;
            }
        } else {
            // ✅ 알 수 없는 명령어일 경우 그대로 출력
            log.warn("[UNKNOWN COMMAND] 알 수 없는 명령어: {}", command);
        }
    }

    private void handleChatCommand(Map<String, Object> messageContent, WebSocketSession session) {
        logChatOrDonationInfo(ChatCommand.CHAT, messageContent);

        try {
            messageService.saveNormalMessage(messageContent);
        } catch (Exception e) {
            log.error("error : {}", e.getMessage());
        }

        sendCommandMessage(session, getProfile(messageContent), getMsg(messageContent));
    }

    private void handleDonationCommand(Map<String, Object> messageContent) {
        logChatOrDonationInfo(ChatCommand.DONATION, messageContent);

        try {
            messageService.saveDonationMessage(messageContent);
        } catch (Exception e) {
            log.error("error : {}", e.getMessage());
        }
    }

    private void logChatOrDonationInfo(ChatCommand command, Map<String, Object> messageContent) {
        log.info("{} :{} : {} : {}",
                command.name(),
                command.getValue(),
                getNickname(messageContent),
                getMsg(messageContent)
        );
    }

    private void sendCommandMessage(WebSocketSession session, HashMap userInfo, String commandInputMessage) {
        if (authorizationManager.isSpecialUser(userInfo)) {
            return;
        }
        List<CommandMessageEntity> commandList = messageRepository.findAll();

        final AtomicReference<HashMap<String, Object>> messageSendOptions = initializeMessageSendOptions(); // Final

        String[] commandArguments = commandInputMessage.split(" ");
        String commandType = commandArguments[0];
        if (authorizationManager.hasCommandPermission(userInfo)) {

            switch (commandType) {
                case COMMAND_ADD:
                case COMMAND_MODIFY:
                    handleAddOrModifyCommand(session, commandArguments, messageSendOptions);
                    break;
                case COMMAND_DELETE:
                    handleDeleteCommand(session, commandArguments, messageSendOptions);
                    break;
                case COMMAND_COOLDOWN:
                    handleUpdateCooldown(session, commandArguments, messageSendOptions);
                    break;
            }
        }
        if( COMMAND_AI.equals(commandType) && commandArguments.length > 1){
            /**
             * AI 모델 응답대기
             */
            log.info("request : {}",String.join(" ", Arrays.stream(commandInputMessage.split(" ")).skip(1).toArray(String[]::new)));

            ollamaClient.isConnected()
                    .flatMapMany(connected -> {
                        if (Boolean.TRUE.equals(connected)) {
                            Flux<OllamaResponse> responseFlux = ollamaClient.generateResponse(session.getId(),
                                            OllamaRequest.builder().messages(
                                                    List.of(
                                                            OllamaMessage.builder()
                                                                    .role("system")
                                                                    .content("text로만 대답해줘.")
                                                                    .build(),
                                                            OllamaMessage.builder()
                                                                    .role("user")
                                                                    .content(StringUtils.getSubstringAfterFirstSpace(commandInputMessage))
                                                                    .build()
                                                    )
                                            ).build());

                            return responseFlux
                                    .takeUntil(OllamaResponse::isDone) // ✅ "done": true가 나오면 중단
                                    .map(OllamaResponse::getMessage)   // ✅ Message 객체 추출
                                    .map(OllamaMessage::getContent) // ✅ content 값만 추출
                                    .filter(content -> content != null && !content.isBlank()) // ✅ 빈 응답 제거
                                    .collectList() // ✅ 모든 데이터를 리스트로 수집
                                    .map(responses -> String.join("", responses)) // ✅ 모든 글자를 붙여서 연결
                                    .flux(); // ✅ 다시 Flux로 변환


                        } else {
                            sendMessageToUser(session, "AI 연결이 되지 않았습니다.", messageSendOptions);
                            return Flux.empty();
                        }
                    })
                    .doOnNext(finalResponse -> sendMessageToUser(session, finalResponse, messageSendOptions)) // ✅ 최종 데이터 전송
                    .subscribe();

        }else{
            checkForCommand(commandInputMessage, commandList, session, messageSendOptions, userInfo);
        }
    }

    private void checkForCommand(String commandInputMessage, List<CommandMessageEntity> commandList, WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference, HashMap userInfo) {
        if (commandList.stream().anyMatch(commandMessageEntity -> commandInputMessage.equals(commandMessageEntity.getCmdStr()))) {
            if(isCooldownElapsed(commandInputMessage,commandList)){
                if (isCommandUsesNickname(commandInputMessage, commandList)) {
                    String responseMessage = userInfo.get(NICKNAME) + "님 " + getCommandMessage(commandInputMessage, commandList);
                    sendMessageToUser(session, responseMessage, messageSendOptionsReference);
                } else {
                    processCommand(commandInputMessage, commandList, session, messageSendOptionsReference);
                }
            }
        }
    }

    private void processCommand(String commandInputMessage, List<CommandMessageEntity> commandList, WebSocketSession session, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        String responseMessage = "";
        switch (commandInputMessage) {
            case "!업타임":
                executeUptime(session, messageSendOptionsReference);
                break;
            case "?":
                responseMessage = getCommandMessage(commandInputMessage, commandList);
                Long counting = getCommandCounting(commandInputMessage, commandList);
                countCommand(session,responseMessage,counting,messageSendOptionsReference);
                CommandMessageEntity commandMessage = getCommandMessageEntity(commandInputMessage, commandList);
                commandMessage.setCounting(counting+1);
                messageRepository.save(commandMessage);
                break;
            case "!팔로우":
            default:
                responseMessage = getCommandMessage(commandInputMessage, commandList);
                sendMessageToUser(session, responseMessage, messageSendOptionsReference);
                break;
        }
    }



    private void countCommand(WebSocketSession session, String responseMessage, Long counting, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        sendMessageToUser(session, String.format("%s %d개 수집중",responseMessage,counting), messageSendOptionsReference);
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

        appendTimeUnit(upTimeMessage, duration.toDays(), "일 ");
        appendTimeUnit(upTimeMessage, duration.toHours() % 24, "시간 ");
        appendTimeUnit(upTimeMessage, duration.toMinutes() % 60, "분 ");
        appendTimeUnit(upTimeMessage, duration.getSeconds() % 60, "초 방송 중");

        return upTimeMessage.toString();
    }

    private CommandMessageEntity getCommandMessageEntity(String command, List<CommandMessageEntity> commandList){
        return commandList.stream().filter(commandMessageEntity -> commandMessageEntity.getCmdStr().equals(command)).findFirst().orElse(null);
    }

    private Long getCommandCounting(String command, List<CommandMessageEntity> commandList){
        /*return commandList.stream().filter(commandMessageEntity -> command.equals(commandMessageEntity.getCmdStr()))
                .findFirst()
                .map(CommandMessageEntity::getCounting)
                .orElse(0L);*/
        return normalMessageRepository.countByMsg(command) + donationMessageRepository.countByMsg(command);
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

    private boolean isCooldownElapsed(String command, List<CommandMessageEntity> commandList) {
        return commandList.stream()
                .filter(commandMessageEntity -> command.equals(commandMessageEntity.getCmdStr()))
                .findFirst()
                .map(this::updateCommandTimeIfNecessary)
                .orElse(false);
    }

    private boolean updateCommandTimeIfNecessary(CommandMessageEntity cmd) {
        LocalDateTime lastCommandTime = cmd.getLastCommandTime();
        if (lastCommandTime == null || isCooldownElapsed(lastCommandTime, cmd.getCooldown())) {
            cmd.setLastCommandTime(LocalDateTime.now());
            messageRepository.save(cmd);
            return true;
        }
        return false;
    }

    private boolean isCooldownElapsed(LocalDateTime lastCommandTime, long cooldown) {
        Duration durationSinceLastCommand = Duration.between(lastCommandTime, LocalDateTime.now());
        return durationSinceLastCommand.toMillis() > cooldown;
    }

    /**
     * 초기 메세지 전송 옵션
     */
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
        if (commandArguments.length == 2) {
            String command = commandArguments[1];
            messageRepository.deleteById(command);

            String feedbackMessage = command + " 명령어가 삭제 되었습니다.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        } else {
            String feedbackMessage = "!삭제 [명령어] 형식으로 입력해주세요.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        }
    }

    private void handleUpdateCooldown(WebSocketSession session, String[] commandArguments, AtomicReference<HashMap<String,Object>> messageSendOptionsReference){
        if(commandArguments.length == 3){
            String command = commandArguments[1];
            long cooldown = Long.parseLong(commandArguments[2]);
            if( cooldown < 0 ){
                String feedbackMessage = "쿨타임은 [0]ms 이상으로 입력해주세요";
                sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
                return;
            }
            messageRepository.save(
                    CommandMessageEntity.builder()
                            .cmdStr(command)
                            .cooldown(cooldown)
                            .build()
            );
        }else{
            String feedbackMessage = "!쿨타임 [명령어] [5000] 형식으로 입력해주세요";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        }
    }

    private void handleAddOrModifyCommand(WebSocketSession session, String[] commandArguments, AtomicReference<HashMap<String, Object>> messageSendOptionsReference) {
        if (commandArguments.length == 3) {
            String command = commandArguments[1];
            String response = commandArguments[2].replaceAll("_", " ");

            messageRepository.save(CommandMessageEntity.builder()
                    .cmdStr(command)
                    .cmdMsg(response)
                    .build());

            String feedbackMessage = command + " 명령어가 수정되었습니다.";
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        } else {
            String feedbackMessage = getUsageMessageForModifyOrAddCommand();
            sendMessageToUser(session, feedbackMessage, messageSendOptionsReference);
        }
    }

    private String getUsageMessageForModifyOrAddCommand() {
        return "!추가 [명령어] [응답] 형식으로 입력해주세요." +
                "[띄어쓰기는 _ 로 바꿔주세요].";
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

    /*private LinkedTreeMap<String, Object> processChannelSearch(HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData) {
        for (String key : searchChannelData.keySet()) {
            if (CONTENT.equals(key)) {
                LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>> content = searchChannelData.get(CONTENT);

                for (String contentKey : content.keySet()) {
                    if (DATA.equals(contentKey)) {
                        List<LinkedTreeMap<String, Object>> dataList = content.get(DATA);
                        LinkedTreeMap<String, Object> channelInfo = dataList.getFirst();
                        log.info("search channelInfo : {}", channelInfo);
                        return (LinkedTreeMap<String, Object>) channelInfo.get("channel");
                    }
                }
            }
        }
        return null;
    }*/
    private LinkedTreeMap<String, Object> processChannelSearch(HashMap<String, LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>>> searchChannelData) {
        try {
            if (searchChannelData == null || searchChannelData.isEmpty()) {
                log.warn("⚠️ 채널 검색 데이터가 비어 있습니다.");
                return null;
            }

            if (!searchChannelData.containsKey(CONTENT)) {
                log.warn("⚠️ '{}' 키를 찾을 수 없습니다.", CONTENT);
                return null;
            }

            LinkedTreeMap<String, List<LinkedTreeMap<String, Object>>> content = searchChannelData.get(CONTENT);
            if (content == null || content.isEmpty()) {
                log.warn("⚠️ '{}' 데이터가 비어 있습니다.", CONTENT);
                return null;
            }

            if (!content.containsKey(DATA)) {
                log.warn("⚠️ '{}' 키를 찾을 수 없습니다.", DATA);
                return null;
            }

            List<LinkedTreeMap<String, Object>> dataList = content.get(DATA);
            if (dataList == null || dataList.isEmpty()) {
                log.warn("⚠️ '{}' 리스트가 비어 있습니다.", DATA);
                return null;
            }

            LinkedTreeMap<String, Object> channelInfo = dataList.getFirst();
            if (channelInfo == null) {
                log.warn("⚠️ 채널 정보가 null입니다.");
                return null;
            }

            log.info("🔍 검색된 채널 정보: {}", channelInfo);

            if (!channelInfo.containsKey("channel")) {
                log.warn("⚠️ 'channel' 키를 찾을 수 없습니다.");
                return null;
            }

            return (LinkedTreeMap<String, Object>) channelInfo.get("channel");
        } catch (Exception e) {
            log.error("❌ 채널 검색 처리 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }


}
