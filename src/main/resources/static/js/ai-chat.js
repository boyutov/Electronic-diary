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
    fab.innerHTML = window.icon ? window.icon("bot") : "🤖";
    document.body.appendChild(fab);

    // Создаём окно чата
    const chatWindow = document.createElement("div");
    chatWindow.className = "ai-chat-window hidden";
    chatWindow.innerHTML = `
        <div class="ai-chat-header">
            <div>
                <h4>AI Ассистент</h4>
                <span>Чат с защитой контактов · локальный импорт</span>
            </div>
            <div class="ai-chat-header-right">
                <button class="ai-clear-btn" id="ai-clear">Очистить</button>
                <button class="ai-close-btn" id="ai-close">✕</button>
            </div>
        </div>
        <div class="ai-messages" id="ai-messages">
            <div class="ai-msg assistant">Помогу с учениками, классами и расписанием. Excel обрабатывается локально. Команды чата отправляются во внешний ИИ с заменой известных имён и контактов.</div>
        </div>
        <div id="ai-file-panel" style="padding:12px" hidden>
            <label>Excel, до 5 МБ <input id="ai-file" type="file" accept=".xlsx,.xls"></label>
            <label>Класс, если отсутствует в файле <input id="ai-file-group" placeholder="Например, 9А"></label>
            <button id="ai-file-preview" type="button">Предпросмотр файла</button>
            <button id="ai-file-import" type="button" disabled>Импортировать учеников</button>
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
    const fileInput = document.getElementById("ai-file");
    const previewButton = document.getElementById("ai-file-preview");
    const importButton = document.getElementById("ai-file-import");
    let previewedFile = null;
    let suggestedMapping = null;
    if (["ADMIN", "DIRECTOR"].includes(role)) document.getElementById("ai-file-panel").hidden = false;
    fileInput.addEventListener("change", () => {
        previewedFile = null;
        suggestedMapping = null;
        importButton.disabled = true;
    });
    previewButton.addEventListener("click", async () => {
        importButton.disabled = true;
        previewedFile = null;
        const file = fileInput.files[0];
        if (!file || file.size > 5000000) return addMessage("assistant", "Выберите Excel до 5 МБ.");
        previewButton.disabled = true;
        try {
            const form = new FormData();
            form.append("file", file);
            const response = await apiFetch(`/api/${schoolName}/ai/files/preview`, {method: "POST", body: form});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "Не удалось прочитать файл");
            if (fileInput.files[0] !== file) return;
            suggestedMapping = result.suggestedMapping;
            const fields = Object.values(suggestedMapping);
            addMessage("assistant", result.headers.join(" | ") + "\n" + result.preview.map(row => row.join(" | ")).join("\n") +
                `\nВсего строк: ${result.totalRows}. Проверьте ФИО, почту и класс. Будут созданы аккаунты и поставлены в очередь письма с временными паролями.`);
            if (!(fields.includes("fullName") || (fields.includes("firstName") && fields.includes("secondName"))) ||
                !(fields.includes("email") || fields.includes("parentEmail"))) {
                throw new Error("Нужны колонки «ФИО» и «Почта» (либо почта родителя). Для ручного сопоставления используйте страницу импорта.");
            }
            previewedFile = file;
            importButton.disabled = false;
        } catch (error) { addMessage("assistant", error.message); }
        finally { previewButton.disabled = false; }
    });
    importButton.addEventListener("click", async () => {
        if (!previewedFile || previewedFile !== fileInput.files[0]) return;
        importButton.disabled = true;
        previewButton.disabled = true;
        fileInput.disabled = true;
        try {
            const dataUrl = await new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result);
                reader.onerror = () => reject(new Error("Не удалось прочитать файл"));
                reader.readAsDataURL(previewedFile);
            });
            const response = await apiFetch(`/api/${schoolName}/ai/files/import`, {
                method: "POST", headers: {"Content-Type": "application/json"},
                body: JSON.stringify({type: "students", fileData: dataUrl.split(",")[1],
                    mapping: suggestedMapping, defaultGroup: document.getElementById("ai-file-group").value.trim()})
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "Ошибка импорта");
            addMessage("assistant", `Создано: ${result.added}. Ошибок: ${result.errors}.\n` + result.errorDetails.join("\n"));
        } catch (error) { addMessage("assistant", error.message); }
        finally {
            previewedFile = null;
            previewButton.disabled = false;
            fileInput.disabled = false;
        }
    });

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
            addMessage("assistant", "❌ Ошибка соединения. Попробуйте ещё раз.");
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
