document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    submitButton.addEventListener("click", () => {
        const name = form.querySelector("input[placeholder='Физика']").value;

        const data = {
            name
        };

        fetch(`/api/${schoolName}/disciplines`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Предмет успешно создан!");
                form.reset();
            } else {
                alert("Ошибка при создании предмета");
            }
        });
    });
});
