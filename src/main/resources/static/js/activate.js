const activateForm = document.getElementById("activate-form");
const activateResult = document.getElementById("activate-result");

const showActivateResult = (message, isError = false) => {
    activateResult.hidden = false;
    activateResult.style.background = isError ? "#fee2e2" : "#dcfce7";
    activateResult.style.color = isError ? "#991b1b" : "#166534";
    activateResult.textContent = message;
};

activateForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    activateResult.hidden = true;

    const payload = {
        schoolPassword: activateForm.schoolPassword.value.trim(),
        firstName: activateForm.firstName.value.trim(),
        secondName: activateForm.secondName.value.trim(),
        thirdName: activateForm.thirdName.value.trim() || null,
        email: activateForm.email.value.trim()
    };

    if (!payload.schoolPassword || !payload.firstName || !payload.secondName || !payload.email) {
        showActivateResult("Пожалуйста, заполните обязательные поля.", true);
        return;
    }

    activateForm.querySelector("button").disabled = true;

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

        showActivateResult("Готово! Администратор создан. Теперь можно войти.");
        setTimeout(() => {
            window.location.href = "/login";
        }, 1200);
    } catch (error) {
        showActivateResult(error.message, true);
    } finally {
        activateForm.querySelector("button").disabled = false;
    }
});
