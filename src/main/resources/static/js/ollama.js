document.addEventListener("DOMContentLoaded", function () {
    const userInput = document.getElementById("user-input");
    const sendBtn = document.getElementById("send-btn");
    const chatBox = document.getElementById("chat-box");

    sendBtn.addEventListener("click", sendMessage);
    userInput.addEventListener("keypress", function (event) {
        if (event.key === "Enter") sendMessage();
    });

    function sendMessage() {
        const message = userInput.value.trim();
        if (!message) return;

        appendMessage("user", message);
        userInput.value = "";

        const botMessageContainer = appendMessage("bot", ""); // AI 메시지 컨테이너 생성
        startLoadingAnimation(botMessageContainer); // 로딩 애니메이션 시작

        const requestBody = {
            model: "myuzzoki:latest",
            messages: [{ role: "user", content: message }],
            stream: true
        };

        fetch("/ollama/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody)
        })
            .then(response => {
                if (!response.body) throw new Error("❌ 응답 스트림이 없습니다.");

                stopLoadingAnimation(botMessageContainer);

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let partialBuffer = "";

                function processStream() {
                    setTimeout(() => { // ✅ `processStream` 자체를 300ms마다 실행
                        reader.read().then(({ done, value }) => {
                            if (done) {
                                console.log("✅ 스트리밍 완료");
                                return;
                            }

                            partialBuffer += decoder.decode(value, { stream: true });

                            const lines = partialBuffer.split("\n");
                            partialBuffer = "";

                            lines.forEach(line => {
                                if (line.startsWith("data:")) {
                                    let jsonString = line.replace("data:", "").trim();

                                    try {
                                        const jsonData = JSON.parse(jsonString);
                                        if (jsonData.message && jsonData.message.content) {
                                            botMessageContainer.textContent += jsonData.message.content;
                                            chatBox.scrollTop = chatBox.scrollHeight;
                                        }

                                        // ✅ `done: true` 감지하면 스트리밍 종료
                                        if (jsonData.done === true) {
                                            console.log("✅ 응답 스트리밍 종료됨.");
                                            return;
                                        }
                                    } catch (error) {
                                        console.error("❌ JSON 파싱 오류:", error);
                                    }
                                }
                            });

                            processStream();
                        });
                    }, 300);
                }

                processStream(); // ✅ 처음 실행
            })
            .catch(error => console.error("❌ 오류:", error));
    }

    function appendMessage(role, message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", role);
        messageElement.textContent = message;
        chatBox.appendChild(messageElement);
        chatBox.scrollTop = chatBox.scrollHeight;
        return messageElement;
    }

    function startLoadingAnimation(element) {
        let dots = "";
        element.textContent = dots;
        element.loadingInterval = setInterval(() => {
            dots = dots.length < 3 ? dots + "•" : "";
            element.textContent = dots;
        }, 500);
    }

    function stopLoadingAnimation(element) {
        clearInterval(element.loadingInterval);
        element.textContent = "";
    }
});
