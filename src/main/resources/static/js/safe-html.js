// Защита от XSS: данные с сервера (имена, тексты новостей, комментарии…) вставляются в HTML только через esc().
// Иначе "<img onerror=...>" в имени ученика выполнился бы у администратора, открывшего список
window.esc = function (value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
};

// Строка внутри onclick="f(...)": сначала как JS-строка, потом как HTML-атрибут
window.escJs = function (value) {
    return window.esc(JSON.stringify(String(value ?? "")));
};
