document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    submitButton.addEventListener("click", () => {
        const title = form.querySelector("input[placeholder='Выберите дату собрания']").value;
        const description = form.querySelector("input[placeholder='Краткое описание']").value;
        const options = form.querySelector("input[placeholder='5 мая, 7 мая']").value.split(',').map(s => s.trim());
        const active = form.querySelector("select").value === "yes";

        const data = {
            title,
            description,
            options,
            active
        };

        fetch(`/api/${schoolName}/polls`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Голосование успешно создано!");
                form.reset();
            } else {
                alert("Ошибка при создании голосования");
            }
        });
    });
});
