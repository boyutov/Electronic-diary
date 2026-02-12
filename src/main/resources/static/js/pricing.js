document.addEventListener("DOMContentLoaded", () => {
    const studentsInput = document.getElementById("calc-students");
    const durationSelect = document.getElementById("calc-duration");
    const promoInput = document.getElementById("calc-promo");
    const totalPriceElement = document.getElementById("total-price");
    const purchaseForm = document.getElementById("purchase-form");
    const resultDiv = document.getElementById("purchase-result");

    function calculatePrice() {
        const students = parseInt(studentsInput.value) || 0;
        const duration = parseInt(durationSelect.value) || 0;
        const promoCode = promoInput.value;

        fetch("/api/purchase/calculate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                schoolName: "Test", // Dummy data for validation
                contactEmail: "test@test.com", // Dummy data
                studentCount: students,
                durationMonths: duration,
                promoCode: promoCode
            })
        })
        .then(response => response.json())
        .then(price => {
            totalPriceElement.textContent = `$${price.toFixed(2)}`;
        });
    }

    studentsInput.addEventListener("input", calculatePrice);
    durationSelect.addEventListener("change", calculatePrice);
    promoInput.addEventListener("input", calculatePrice);

    // Initial calculation
    calculatePrice();

    purchaseForm.addEventListener("submit", (e) => {
        e.preventDefault();
        
        const data = {
            schoolName: document.getElementById("schoolName").value,
            contactEmail: document.getElementById("contactEmail").value,
            contactPhone: document.getElementById("contactPhone").value,
            studentCount: parseInt(studentsInput.value),
            durationMonths: parseInt(durationSelect.value),
            promoCode: promoInput.value
        };

        fetch("/api/purchase", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return Promise.reject("Ошибка при покупке");
            }
        })
        .then(data => {
            resultDiv.hidden = false;
            resultDiv.innerHTML = `
                <p>Школа успешно создана!</p>
                <p>ID школы: <strong>${data.schoolId}</strong></p>
                <p>Временный пароль администратора: <strong>${data.adminPassword}</strong></p>
                <p>Сохраните эти данные! Они нужны для активации.</p>
            `;
            purchaseForm.reset();
        })
        .catch(error => {
            alert(error);
        });
    });
});
