const loginForm = document.getElementById("login-form");
const loginResult = document.getElementById("login-result");

const showLoginResult = (message, isError = false) => {
    loginResult.hidden = false;
    loginResult.style.background = isError ? "#fee2e2" : "#dcfce7";
    loginResult.style.color = isError ? "#991b1b" : "#166534";
    loginResult.textContent = message;
};

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    loginResult.hidden = true;

    const payload = {
        schoolPassword: loginForm.schoolPassword.value.trim(),
        firstName: loginForm.firstName.value.trim(),
        secondName: loginForm.secondName.value.trim(),
        thirdName: loginForm.thirdName.value.trim() || null,
        lastName: loginForm.lastName.value.trim() || null,
        email: loginForm.email.value.trim()
    };

    if (!payload.schoolPassword || !payload.firstName || !payload.secondName || !payload.email) {
        showLoginResult("Пожалуйста, заполните обязательные поля.", true);
        return;
    }

    loginForm.querySelector("button").disabled = true;

    try {
        const response = await fetch("/api/purchase/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "Не удалось активировать аккаунт школы.");
        }

        showLoginResult("Готово! Аккаунт школы активирован, администратор создан. Переходим в панель...");
        setTimeout(() => {
            window.location.href = "/admin";
        }, 1200);
    } catch (error) {
        showLoginResult(error.message, true);
    } finally {
        loginForm.querySelector("button").disabled = false;
    }
});
