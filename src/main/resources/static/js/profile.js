document.addEventListener("DOMContentLoaded", () => {
    const firstNameInput = document.getElementById("firstName");
    const secondNameInput = document.getElementById("secondName");
    const thirdNameInput = document.getElementById("thirdName");
    const emailInput = document.getElementById("email");
    const newPasswordInput = document.getElementById("newPassword");
    const submitButton = document.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    // Загрузка данных пользователя
    fetch(`/api/${schoolName}/profile/me`)
        .then(response => response.json())
        .then(user => {
            firstNameInput.value = user.firstName;
            secondNameInput.value = user.secondName;
            thirdNameInput.value = user.thirdName || "";
            emailInput.value = user.email;
        });

    submitButton.addEventListener("click", () => {
        const data = {
            firstName: firstNameInput.value,
            secondName: secondNameInput.value,
            thirdName: thirdNameInput.value,
            email: emailInput.value,
            newPassword: newPasswordInput.value
        };

        fetch(`/api/${schoolName}/profile/me`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Профиль успешно обновлен!");
                newPasswordInput.value = ""; // Очистить поле пароля
            } else {
                alert("Ошибка при обновлении профиля");
            }
        });
    });
});
