document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    submitButton.addEventListener("click", () => {
        const firstName = form.querySelector("input[placeholder='Имя']").value;
        const secondName = form.querySelector("input[placeholder='Фамилия']").value;
        const thirdName = form.querySelector("input[placeholder='Отчество']").value;
        const email = form.querySelector("input[type='email']").value;
        const password = form.querySelector("input[type='password']").value;

        const data = {
            firstName,
            secondName,
            thirdName,
            email,
            password
        };

        fetch(`/api/${schoolName}/directors`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Директор успешно создан!");
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
                    alert("Ошибка при создании директора:\n" + errorMessages);
                });
            }
        });
    });
});
