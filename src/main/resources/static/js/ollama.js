document.addEventListener("DOMContentLoaded", function () {
    const userInput = document.getElementById("user-input");
    const sendBtn = document.getElementById("send-btn");
    const clearBtn = document.getElementById("clear-btn");
    const chatBox = document.getElementById("chat-box");

    let isProcessing = false;
    let messageHistory = [];

    // 텍스트 영역 자동 크기 조절
    function adjustTextareaHeight() {
        userInput.style.height = 'auto';
        userInput.style.height = Math.min(userInput.scrollHeight, 150) + 'px';
    }

    function sendMessage() {
        const message = userInput.value.trim();
        if (!message || isProcessing) return;

        isProcessing = true;
        sendBtn.disabled = true;

        // 사용자 메시지 추가
        appendMessage("user", message);
        messageHistory.push(new OllamaMessage("user", message));

        // 입력창 초기화
        userInput.value = "";
        userInput.style.height = 'auto';

        const botMessageContainer = appendMessage("bot", "");
        startLoadingAnimation(botMessageContainer);

        const request = new OllamaRequest([new OllamaMessage("user", message)]);

        fetch("/ollama/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request.toJSON())
        }).then(response => {
            if (!response.body) throw new Error("응답 스트림이 없습니다.");

            stopLoadingAnimation(botMessageContainer);
            const reader = response.body.getReader();
            return processStream(reader, botMessageContainer, chatBox);
        }).catch(error => {
            console.error("오류 발생:", error);
            botMessageContainer.textContent = "죄송합니다. 오류가 발생했습니다. 다시 시도해 주세요.";
            botMessageContainer.classList.add("error");
        }).finally(() => {
            isProcessing = false;
            sendBtn.disabled = false;
            chatBox.scrollTop = chatBox.scrollHeight;
        });
    }

    function appendMessage(role, message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("chat-message", role === "user" ? "user-message" : "ai-message");
        
        // 아바타 컨테이너
        const avatar = document.createElement("div");
        avatar.className = role === "user" ? "user-avatar" : "ai-avatar";
        if (role === "user") {
            const icon = document.createElement("i");
            icon.className = "fas fa-user";
            avatar.appendChild(icon);
        }
        
        // 메시지 컨텐츠 래퍼
        const contentWrapper = document.createElement("div");
        contentWrapper.className = "message-content-wrapper";
        
        // 메시지 컨텐츠
        const messageContent = document.createElement("div");
        messageContent.className = "message-content";
        messageContent.textContent = message;
        
        // 시간 표시
        const timeDiv = document.createElement("div");
        timeDiv.className = "message-time";
        timeDiv.textContent = new Date().toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit"
        });
        
        // DOM 구조 조립
        contentWrapper.appendChild(messageContent);
        contentWrapper.appendChild(timeDiv);
        messageElement.appendChild(avatar);
        messageElement.appendChild(contentWrapper);
        
        chatBox.appendChild(messageElement);
        chatBox.scrollTop = chatBox.scrollHeight;
        
        return messageContent;
    }

    function startLoadingAnimation(element) {
        const loadingSpan = document.createElement("span");
        loadingSpan.classList.add("loading-dots");
        element.appendChild(loadingSpan);
    }

    function stopLoadingAnimation(element) {
        element.innerHTML = "";
    }

    // 이벤트 리스너 설정
    userInput.addEventListener("keydown", function(event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    userInput.addEventListener("input", adjustTextareaHeight);
    
    sendBtn.addEventListener("click", sendMessage);

    // 대화 내용 초기화
    clearBtn.addEventListener("click", function() {
        if (confirm("대화 내용을 모두 지우시겠습니까?")) {
            chatBox.innerHTML = "";
            messageHistory = [];
        }
    });

    // 초기 환영 메시지
    const welcomeMessage = "안녕하세요! 저는 뮤쪽이 AI입니다. 무엇을 도와드릴까요?";
    const botMessageContainer = appendMessage("bot", "");
    botMessageContainer.innerHTML = marked.parse(welcomeMessage);
});

// 스트림 처리 함수
function processStream(reader, messageElement, chatBox) {
    const decoder = new TextDecoder();
    let markdownBuffer = "";
    let partialBuffer = "";

    function processChunk() {
        return reader.read().then(({value, done}) => {
            if (done) {
                // 스트림 완료 시 최종 마크다운 렌더링
                messageElement.innerHTML = marked.parse(markdownBuffer);
                renderMathInElement(messageElement, {
                    delimiters: [
                        {left: "$$", right: "$$", display: true},
                        {left: "\\(", right: "\\)", display: false}
                    ]
                });
                return;
            }

            partialBuffer += decoder.decode(value, {stream: true});
            const lines = partialBuffer.split("\n");
            partialBuffer = lines.pop() || "";

            lines.forEach(line => {
                if (line.startsWith("data:")) {
                    try {
                        const jsonData = JSON.parse(line.replace("data:", "").trim());
                        const response = OllamaResponse.fromJSON(jsonData);

                        if (response.message?.content) {
                            markdownBuffer += response.message.content;
                            messageElement.innerHTML = marked.parse(markdownBuffer);
                            
                            renderMathInElement(messageElement, {
                                delimiters: [
                                    {left: "$$", right: "$$", display: true},
                                    {left: "\\(", right: "\\)", display: false}
                                ]
                            });

                            chatBox.scrollTop = chatBox.scrollHeight;
                        }
                    } catch (error) {
                        console.error("JSON 파싱 오류:", error);
                    }
                }
            });

            return processChunk();
        });
    }

    return processChunk().catch(error => {
        console.error("스트림 처리 오류:", error);
        throw error;
    });
}