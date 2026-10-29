// Обязательная смена временного пароля после первого входа
document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const form = document.getElementById("password-form");
    const errorMsg = document.getElementById("error-msg");

    if (!sessionStorage.getItem("jwtToken")) {
        window.location.href = "/login";
        return;
    }

    const showError = (text) => {
        errorMsg.textContent = text;
        errorMsg.style.display = "block";
    };

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        errorMsg.style.display = "none";

        const currentPassword = document.getElementById("currentPassword").value;
        const newPassword = document.getElementById("newPassword").value;
        if (newPassword !== document.getElementById("repeatPassword").value) {
            showError("Пароли не совпадают");
            return;
        }

        apiFetch(`/api/${schoolName}/profile/password`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ currentPassword, newPassword })
        })
        .then(async response => {
            if (!response.ok) {
                const text = await response.text();
                let message = text;
                try {
                    const body = JSON.parse(text);
                    message = body.newPassword || body.currentPassword || body.message || text;
                } catch (ignored) { }
                throw new Error(message || "Не удалось сменить пароль");
            }

            // Пароль сменён — ведём на страницу роли, как после обычного входа
            const role = sessionStorage.getItem("userRole");
            let redirectUrl = `/${schoolName}`;
            if (role === "ADMIN") redirectUrl += "/admin";
            else if (role === "DIRECTOR") redirectUrl += "/director";
            else if (role === "TEACHER") redirectUrl += "/teacher";
            else if (role === "PARENT") redirectUrl += "/parent";
            else if (role === "STUDENT") redirectUrl += "/student";
            window.location.href = redirectUrl;
        })
        .catch(error => showError(error.message));
    });
});
