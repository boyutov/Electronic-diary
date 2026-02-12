document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const hasOfficeSelect = form.querySelector("[data-toggle-target='group-office']");
    const schoolName = window.location.pathname.split('/')[1];

    submitButton.addEventListener("click", () => {
        const name = form.querySelector("input[placeholder='9Б']").value;
        const hasOffice = hasOfficeSelect.value === "yes";
        const office = form.querySelector("input[placeholder='203']").value;
        const course = form.querySelector("input[placeholder='1']").value;

        const data = {
            name,
            hasOffice,
            office: hasOffice ? office : null,
            course: parseInt(course)
        };

        fetch(`/api/${schoolName}/groups`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Группа успешно создана!");
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
                    alert("Ошибка при создании группы:\n" + errorMessages);
                });
            }
        });
    });
});
