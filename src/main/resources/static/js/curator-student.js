document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];
    let groupId = null;

    // Получаем профиль куратора, чтобы узнать ID его группы
    fetch(`/api/${schoolName}/profile/curator`)
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            return Promise.reject("Could not fetch curator profile");
        })
        .then(profile => {
            groupId = profile.groupId;
        })
        .catch(error => {
            console.error("Error fetching curator group:", error);
            alert("Не удалось определить вашу группу. Вы не можете создавать учеников.");
            submitButton.disabled = true;
        });

    submitButton.addEventListener("click", () => {
        if (!groupId) {
            alert("Группа не определена. Создание невозможно.");
            return;
        }

        const firstName = form.querySelector("input[placeholder='Имя']").value;
        const secondName = form.querySelector("input[placeholder='Фамилия']").value;
        const thirdName = form.querySelector("input[placeholder='Отчество']").value;
        const email = form.querySelector("input[type='email']").value;
        const password = form.querySelector("input[type='password']").value;
        const age = form.querySelector("input[placeholder='15']").value;

        const data = {
            firstName,
            secondName,
            thirdName,
            email,
            password,
            age: parseInt(age),
            groupId: groupId // Автоматически подставляем ID группы
        };

        fetch(`/api/${schoolName}/students`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Ученик успешно создан!");
                form.reset();
            } else {
                response.json().then(errors => {
                    let errorMessages = "";
                    if (typeof errors === 'object') {
                        for (const [field, message] of Object.entries(errors)) {
                            errorMessages += `${field}: ${message}\n`;
                        }
                    } else {
                        errorMessages = errors;
                    }
                    alert("Ошибка при создании ученика:\n" + errorMessages);
                });
            }
        });
    });
});
