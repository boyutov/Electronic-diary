// AI Ассистент — виджет чата
(function () {
    const ALLOWED_ROLES = ["ADMIN", "DIRECTOR", "TEACHER"];
    const role = sessionStorage.getItem("userRole") || "";
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    if (!ALLOWED_ROLES.includes(role)) return;

    // Создаём FAB кнопку
    const fab = document.createElement("button");
    fab.className = "ai-fab";
    fab.title = "AI Ассистент";
    fab.innerHTML = "🤖";
    document.body.appendChild(fab);

    // Создаём окно чата
    const chatWindow = document.createElement("div");
    chatWindow.className = "ai-chat-window hidden";
    chatWindow.innerHTML = `
        <div class="ai-chat-header">
            <div>
                <h4>🤖 AI Ассистент</h4>
                <span>Помогу управлять школой</span>
            </div>
            <div class="ai-chat-header-right">
                <button class="ai-clear-btn" id="ai-clear">Очистить</button>
                <button class="ai-close-btn" id="ai-close">✕</button>
            </div>
        </div>
        <div class="ai-messages" id="ai-messages">
            <div class="ai-msg assistant">Привет! Я AI-ассистент. Могу помочь добавить учеников, создать группы, предметы и многое другое. Что нужно сделать?</div>
        </div>
        <div class="ai-input-area">
            <textarea id="ai-input" placeholder="Напишите задание... (Enter — отправить, Shift+Enter — новая строка)" rows="1"></textarea>
            <button class="ai-send-btn" id="ai-send">➤</button>
        </div>
    `;
    document.body.appendChild(chatWindow);

    const messagesEl = document.getElementById("ai-messages");
    const inputEl    = document.getElementById("ai-input");
    const sendBtn    = document.getElementById("ai-send");
    let isOpen = false;

    // Открыть/закрыть
    fab.addEventListener("click", () => {
        isOpen = !isOpen;
        chatWindow.classList.toggle("hidden", !isOpen);
        if (isOpen) { inputEl.focus(); scrollToBottom(); }
    });
    document.getElementById("ai-close").addEventListener("click", () => {
        isOpen = false; chatWindow.classList.add("hidden");
    });

    // Очистить историю
    document.getElementById("ai-clear").addEventListener("click", () => {
        apiFetch(`/api/${schoolName}/ai/clear`, { method: "POST" }).catch(() => {});
        messagesEl.innerHTML = `<div class="ai-msg assistant">История очищена. Чем могу помочь?</div>`;
    });

    // Отправка
    sendBtn.addEventListener("click", sendMessage);
    inputEl.addEventListener("keydown", e => {
        if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); }
    });

    // Авто-resize textarea
    inputEl.addEventListener("input", () => {
        inputEl.style.height = "auto";
        inputEl.style.height = Math.min(inputEl.scrollHeight, 100) + "px";
    });

    function sendMessage() {
        const text = inputEl.value.trim();
        if (!text) return;

        addMessage("user", text);
        inputEl.value = "";
        inputEl.style.height = "auto";
        sendBtn.disabled = true;

        // Индикатор загрузки
        const typingEl = addMessage("typing", "⏳ Думаю...");

        apiFetch(`/api/${schoolName}/ai/chat`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: text })
        })
        .then(r => r.ok ? r.json() : { reply: "Ошибка соединения с AI", success: false })
        .then(data => {
            typingEl.remove();
            addMessage("assistant", data.reply || "Нет ответа");
        })
        .catch(() => {
            typingEl.remove();
            addMessage("assistant", "❌ Ошибка соединения. Проверьте настройки API ключа.");
        })
        .finally(() => { sendBtn.disabled = false; inputEl.focus(); });
    }

    function addMessage(type, text) {
        const el = document.createElement("div");
        el.className = `ai-msg ${type}`;
        el.textContent = text;
        messagesEl.appendChild(el);
        scrollToBottom();
        return el;
    }

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }
})();
