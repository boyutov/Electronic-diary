// Регистрация школы — бесплатная: только название, почта и телефон
document.addEventListener("DOMContentLoaded", () => {
    const purchaseForm = document.getElementById("purchase-form");
    const resultDiv = document.getElementById("purchase-result");

    purchaseForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const button = purchaseForm.querySelector("button[type=submit]");
        button.disabled = true;

        const data = {
            schoolName: document.getElementById("schoolName").value,
            contactEmail: document.getElementById("contactEmail").value,
            contactPhone: document.getElementById("contactPhone").value
        };

        fetch("/api/purchase", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        })
        .then(async response => {
            if (response.ok) return response.json();
            // 400 — понятная причина текстом (например, название занято); иначе общая ошибка
            const text = await response.text().catch(() => "");
            return Promise.reject(response.status === 400 && text && !text.startsWith("{")
                ? text : "Не удалось зарегистрировать школу. Проверьте данные.");
        })
        .then(data => {
            resultDiv.style.display = "block";
            resultDiv.style.background = "";
            resultDiv.style.color = "";
            resultDiv.innerHTML = `
                <strong>Школа зарегистрирована!</strong>
                Временный пароль: <strong></strong><br>
                Сохраните его — он нужен для <a href="/activate">активации</a>.
            `;
            resultDiv.querySelector("strong:nth-of-type(2)").textContent = data.generatedPassword;
            purchaseForm.reset();
        })
        .catch(error => {
            resultDiv.style.display = "block";
            resultDiv.style.background = "#fee2e2";
            resultDiv.style.color = "#991b1b";
            resultDiv.textContent = typeof error === "string" ? error : "Не удалось зарегистрировать школу";
        })
        .finally(() => { button.disabled = false; });
    });
});
