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
        return response;
    });
};

// Безопасный парсинг JSON — возвращает null если тело не JSON
window.safeJson = function(response) {
    return response.json().catch(() => null);
};
