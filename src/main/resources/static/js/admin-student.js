document.addEventListener("DOMContentLoaded", () => {
    const groupSelect = document.querySelector("select[name='group']");
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    fetch(`/api/${schoolName}/groups`)
        .then(response => response.json())
        .then(groups => {
            groupSelect.innerHTML = "<option selected disabled>Выберите группу</option>";
            groups.forEach(group => {
                const option = document.createElement("option");
                option.value = group.id;
                option.textContent = group.name;
                groupSelect.appendChild(option);
            });
        });

    submitButton.addEventListener("click", () => {
        const firstName = form.querySelector("input[placeholder='Имя']").value;
        const secondName = form.querySelector("input[placeholder='Фамилия']").value;
        const thirdName = form.querySelector("input[placeholder='Отчество']").value;
        const email = form.querySelector("input[type='email']").value;
        const password = form.querySelector("input[type='password']").value;
        const age = form.querySelector("input[placeholder='15']").value;
        const groupId = groupSelect.value;

        const data = {
            firstName,
            secondName,
            thirdName,
            email,
            password,
            age: parseInt(age),
            groupId: parseInt(groupId)
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
