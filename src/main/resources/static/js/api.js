// Обёртка над fetch для автоматического добавления JWT токена
window.apiFetch = function(url, options = {}) {
    const token = sessionStorage.getItem("jwtToken");

    if (token) {
        if (!options.headers) options.headers = {};
        options.headers["Authorization"] = `Bearer ${token}`;
    }

    return fetch(url, options).then(response => {
        if (response.status === 401) {
            sessionStorage.removeItem("jwtToken");
            window.location.href = "/login";
            return Promise.reject("Unauthorized");
        }
        // Сервер не пускает дальше, пока не сменён временный пароль
        if (response.status === 403) {
            return response.clone().json()
                .then(body => {
                    if (body && body.error === "PASSWORD_CHANGE_REQUIRED") {
                        const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
                        window.location.href = `/${schoolName}/change-password`;
                        return Promise.reject("Password change required");
                    }
                    return response;
                }, () => response);
        }
        return response;
    });
};

// Безопасный парсинг JSON — возвращает null если тело не JSON
window.safeJson = function(response) {
    return response.json().catch(() => null);
};

// Шкала оценок школы: FIVE (2–5) или HUNDRED (1–100). Выбирает директор/администратор в «Оценивании».
// Загружается один раз; страницы ждут markScaleReady, прежде чем раскрашивать оценки.
// Обычный fetch, а не apiFetch: на странице смены пароля apiFetch увёл бы в бесконечный редирект
window.MARK_SCALE = "HUNDRED";
window.markScaleReady = (function () {
    const token = sessionStorage.getItem("jwtToken");
    const sn = sessionStorage.getItem("schoolName");
    if (!token || !sn || window.location.pathname.endsWith("/change-password")) return Promise.resolve(window.MARK_SCALE);
    return fetch(`/api/${sn}/grading/settings`, { headers: { "Authorization": `Bearer ${token}` } })
        // тело читаем всегда: непрочитанный ответ с ошибкой браузер держит открытым
        .then(r => r.json().then(body => r.ok ? body : null, () => null))
        .then(s => { if (s && s.markScale) window.MARK_SCALE = s.markScale; return window.MARK_SCALE; })
        .catch(() => window.MARK_SCALE);
})();
window.markMax = () => window.MARK_SCALE === "FIVE" ? 5 : 100;
// high / mid / low — для оценки и для среднего балла
window.markLevel = function (value) {
    const v = Number(value);
    if (window.MARK_SCALE === "FIVE") return v >= 4 ? "high" : v >= 3 ? "mid" : "low";
    return v >= 80 ? "high" : v >= 60 ? "mid" : "low";
};
window.markTone = (value) => ({ high: "green", mid: "yellow", low: "red" })[window.markLevel(value)];
window.markColors = (value) => ({
    high: { fg: "#15803d", bg: "#dcfce7" }, mid: { fg: "#854d0e", bg: "#fef9c3" }, low: { fg: "#b91c1c", bg: "#fee2e2" }
})[window.markLevel(value)];

// Учебные периоды школы (четверти / полугодия / год) с датами — их задаёт директор в «Оценивании»
window.loadPeriods = function () {
    const sn = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    return apiFetch(`/api/${sn}/grading/periods`)
        .then(r => r.ok ? r.json() : { periods: [] })
        .catch(() => ({ periods: [] }));
};
// Оценка попадает в период по дате выставления; period = null — все оценки
window.markInPeriod = function (mark, period) {
    if (!period) return true;
    const date = String(mark.createdAt || mark.date || "").slice(0, 10);
    return date >= period.from && date <= period.to;
};
// Выпадающий список периодов. По умолчанию — текущий период (или последний прошедший)
window.renderPeriodPicker = function (host, periods, onChange) {
    const today = new Date().toISOString().slice(0, 10);
    const current = periods.findIndex(p => today >= p.from && today <= p.to);
    const selected = current >= 0 ? current : (periods.length ? 0 : -1);
    const label = p => `${p.name} (${new Date(p.from).toLocaleDateString("ru-RU", { day: "numeric", month: "short" })} — ${new Date(p.to).toLocaleDateString("ru-RU", { day: "numeric", month: "short" })})`;
    host.innerHTML = `<label class="period-picker"><span>Период</span><select>
        ${periods.map((p, i) => `<option value="${i}"${i === selected ? " selected" : ""}>${esc(label(p))}</option>`).join("")}
        <option value="all"${selected < 0 ? " selected" : ""}>Все оценки</option></select></label>`;
    const select = host.querySelector("select");
    const fire = () => onChange(select.value === "all" ? null : periods[Number(select.value)]);
    select.addEventListener("change", fire);
    fire();
};
