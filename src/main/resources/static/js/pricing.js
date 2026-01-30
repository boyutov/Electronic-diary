const form = document.getElementById("purchase-form");
const result = document.getElementById("purchase-result");

const setResult = (message, isError = false) => {
    result.hidden = false;
    result.style.background = isError ? "#fee2e2" : "#dcfce7";
    result.style.color = isError ? "#991b1b" : "#166534";
    result.textContent = message;
};

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    result.hidden = true;

    const payload = {
        schoolName: form.schoolName.value.trim(),
        contactEmail: form.contactEmail.value.trim(),
        contactPhone: form.contactPhone.value.trim() || null
    };

    if (!payload.schoolName || !payload.contactEmail) {
        setResult("Пожалуйста, заполните название школы и email.", true);
        return;
    }

    form.querySelector("button").disabled = true;

    try {
        const response = await fetch("/api/purchase", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error("Ошибка при создании аккаунта школы.");
        }

        const data = await response.json();
        setResult(`Готово! ID школы: ${data.schoolId}. Пароль для входа: ${data.generatedPassword}`);
    } catch (error) {
        setResult(error.message, true);
    } finally {
        form.querySelector("button").disabled = false;
    }
});
