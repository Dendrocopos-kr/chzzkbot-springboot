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

        const request = new OllamaRequest([new OllamaMessage("user", message)]);
        //const request = new OllamaRequest('hf.co/bartowski/Aya-Expanse-8B-GGUF',[new OllamaMessage("user", message)]);

        fetch("/ollama/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request.toJSON())
        }).then(response => {
            if (!response.body) throw new Error("❌ 응답 스트림이 없습니다.");

            stopLoadingAnimation(botMessageContainer);
            const reader = response.body.getReader();
            processStream(reader, botMessageContainer, chatBox); // ✅ 모듈화된 `processStream` 사용
        }).catch(error => console.error("❌ 오류 발생:", error));
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
