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
