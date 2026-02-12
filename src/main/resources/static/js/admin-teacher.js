document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const hasOfficeSelect = form.querySelector("[data-toggle-target='teacher-office']");
    const hasGroupSelect = form.querySelector("[data-toggle-target='teacher-group']");
    const groupSelect = form.querySelector("select[name='group']");
    const schoolName = window.location.pathname.split('/')[1];

    // Загрузка групп
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
        const phone = form.querySelector("input[placeholder='+7 700 000 00 00']").value;
        const bio = form.querySelector("input[placeholder='Краткая информация']").value;
        const hasOffice = hasOfficeSelect.value === "yes";
        const office = form.querySelector("input[placeholder='101']").value;
        const hasGroup = hasGroupSelect.value === "yes";
        const groupId = groupSelect.value;

        const data = {
            firstName,
            secondName,
            thirdName,
            email,
            password,
            phone,
            bio,
            hasOffice,
            office: hasOffice ? office : null,
            hasGroup,
            groupId: hasGroup ? parseInt(groupId) : null
        };

        fetch(`/api/${schoolName}/teachers`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Учитель успешно создан!");
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
                    alert("Ошибка при создании учителя:\n" + errorMessages);
                });
            }
        });
    });
});
