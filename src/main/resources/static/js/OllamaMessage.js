class OllamaMessage {
    constructor(role, content) {
        this.role = role;    // "user" or "assistant"
        this.content = content;
        this.element = this.createElement();
    }

    createElement() {
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${this.role === 'user' ? 'user-message' : 'ai-message'}`;

        // 아바타 생성
        const avatarDiv = document.createElement('div');
        if (this.role === 'user') {
            avatarDiv.className = 'user-avatar';
            const icon = document.createElement('i');
            icon.className = 'fas fa-user';
            avatarDiv.appendChild(icon);
        } else {
            avatarDiv.className = 'ai-avatar';
        }

        // 메시지 내용 컨테이너
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        
        if (this.role === 'assistant') {
            contentDiv.innerHTML = marked.parse(this.content);
            contentDiv.querySelectorAll('pre code').forEach((block) => {
                hljs.highlightElement(block);
            });
        } else {
            contentDiv.textContent = this.content;
        }

        messageDiv.appendChild(avatarDiv);
        messageDiv.appendChild(contentDiv);

        return messageDiv;
    }

    // JSON 데이터를 OllamaMessage 객체로 변환
    static fromJSON(jsonData) {
        return new OllamaMessage(jsonData.role, jsonData.content);
    }

    // JSON 형태로 변환 (API 요청 시 사용)
    toJSON() {
        return {
            role: this.role,
            content: this.content
        };
    }
}

class OllamaRequest {
    constructor(modelOrMessages, messages = [], stream = true) {
        if (Array.isArray(modelOrMessages)) {
            // ✅ 첫 번째 인자가 배열이면 messages로 간주하고 모델 기본값 사용
            this.model = "myuzzoki:latest";
            this.messages = modelOrMessages.map(msg => new OllamaMessage(msg.role, msg.content));
        } else if (typeof modelOrMessages === "string") {
            // ✅ 첫 번째 인자가 문자열이면 모델로 사용
            this.model = modelOrMessages.trim() !== "" ? modelOrMessages : "myuzzoki:latest";
            this.messages = messages.map(msg => new OllamaMessage(msg.role, msg.content));
        } else {
            // ✅ 첫 번째 인자가 없으면 기본 모델 사용
            this.model = "myuzzoki:latest";
            this.messages = [];
        }
        this.stream = stream;
        this.options = { temperature: 0.7, top_p: 0.9 };
    }

    static fromJSON(jsonData) {
        return new OllamaRequest(
            jsonData.model || "myuzzoki:latest",
            jsonData.messages ? jsonData.messages.map(OllamaMessage.fromJSON) : [],
            jsonData.stream
        );
    }

    toJSON() {
        return {
            model: this.model,
            messages: this.messages.map(msg => msg.toJSON()),
            stream: this.stream,
            options: this.options
        };
    }
}


class OllamaResponse {
    constructor(model, createdAt, message, doneReason, isDone, totalDuration, loadDuration, promptEvalCount, promptEvalDuration, evalCount, evalDuration) {
        this.model = model;
        this.createdAt = new Date(createdAt);
        this.message = message ? OllamaMessage.fromJSON(message) : null;
        this.doneReason = doneReason;
        this.isDone = isDone;
        this.totalDuration = totalDuration;
        this.loadDuration = loadDuration;
        this.promptEvalCount = promptEvalCount;
        this.promptEvalDuration = promptEvalDuration;
        this.evalCount = evalCount;
        this.evalDuration = evalDuration;
    }

    // JSON 데이터를 OllamaResponse 객체로 변환
    static fromJSON(jsonData) {
        return new OllamaResponse(
            jsonData.model,
            jsonData.created_at,
            jsonData.message,
            jsonData.done_reason,
            jsonData.done,
            jsonData.total_duration,
            jsonData.load_duration,
            jsonData.prompt_eval_count,
            jsonData.prompt_eval_duration,
            jsonData.eval_count,
            jsonData.eval_duration
        );
    }

    // JSON 형태로 변환
    toJSON() {
        return {
            model: this.model,
            created_at: this.createdAt.toISOString(),
            message: this.message ? this.message.toJSON() : null,
            done_reason: this.doneReason,
            done: this.isDone,
            total_duration: this.totalDuration,
            load_duration: this.loadDuration,
            prompt_eval_count: this.promptEvalCount,
            prompt_eval_duration: this.promptEvalDuration,
            eval_count: this.evalCount,
            eval_duration: this.evalDuration
        };
    }
}

// ✅ 스트리밍 응답을 처리하는 함수 (ollamaMessage.js에 포함)
function processStream(reader, botMessageContainer, chatBox) {
    const decoder = new TextDecoder();
    let partialBuffer = "";
    let markdownBuffer = ""; // ✅ 전체 메시지를 저장할 버퍼

    function readChunk() {
        reader.read().then(({ done, value }) => {

            partialBuffer += decoder.decode(value, { stream: true });
            const lines = partialBuffer.split("\n");
            partialBuffer = lines.pop(); // 마지막 줄은 불완전할 가능성이 있으므로 버퍼에 저장

            lines.forEach(line => {
                if (line.startsWith("data:")) {
                    let jsonString = line.replace("data:", "").trim();
                    try {
                        const jsonData = JSON.parse(jsonString);
                        const response = OllamaResponse.fromJSON(jsonData);

                        if (response.message && response.message.content) {
                            botMessageContainer.textContent += response.message.content;
                            markdownBuffer += response.message.content; // ✅ 스트리밍 데이터를 버퍼에 저장
                            chatBox.scrollTop = chatBox.scrollHeight;
                        }

                        if (response.isDone) {
                            console.log("✅ 응답 스트리밍 종료됨.");
                            // ✅ 모든 데이터를 받은 후 Markdown 변환
                            // ✅ Markdown 변환
                            botMessageContainer.innerHTML = marked.parse(markdownBuffer);
                            chatBox.scrollTop = chatBox.scrollHeight;

                            // ✅ LaTeX 수식 변환 (KaTeX 적용)
                            renderMathInElement(botMessageContainer, {
                                delimiters: [
                                    { left: "$$", right: "$$", display: true }, // 블록 수식
                                    { left: "\\(", right: "\\)", display: false } // 인라인 수식
                                ]
                            });
                            return;
                        }

                    } catch (error) {
                        console.error("❌ JSON 파싱 오류:", error);
                    }
                }
            });

            if (done) {
                console.log("✅ 스트리밍 완료");
                // ✅ Markdown 변환
                botMessageContainer.innerHTML = marked.parse(markdownBuffer);
                chatBox.scrollTop = chatBox.scrollHeight;

                // ✅ LaTeX 수식 변환 (KaTeX 적용)
                renderMathInElement(botMessageContainer, {
                    delimiters: [
                        { left: "$$", right: "$$", display: true }, // 블록 수식
                        { left: "\\(", right: "\\)", display: false } // 인라인 수식
                    ]
                });
                return;
            }

            readChunk();
        }).catch(error => console.error("❌ 스트리밍 읽기 오류:", error));
    }

    readChunk();
}