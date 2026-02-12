document.addEventListener("DOMContentLoaded", () => {
    const teacherSelect = document.querySelector("select[name='teacher']");
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    fetch(`/api/${schoolName}/teachers`)
        .then(response => response.json())
        .then(teachers => {
            teacherSelect.innerHTML = "<option selected disabled>Выберите учителя</option>";
            teachers.forEach(teacher => {
                const option = document.createElement("option");
                option.value = teacher.id;
                option.textContent = `${teacher.firstName} ${teacher.secondName}`;
                teacherSelect.appendChild(option);
            });
        });

    submitButton.addEventListener("click", () => {
        const name = form.querySelector("input[placeholder='Подготовка к ЕНТ']").value;
        const teacherId = teacherSelect.value;
        const description = form.querySelector("input[placeholder='Краткое описание']").value;

        const data = {
            name,
            teacherId: parseInt(teacherId),
            description
        };

        fetch(`/api/${schoolName}/courses`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Курс успешно создан!");
                form.reset();
            } else {
                alert("Ошибка при создании курса");
            }
        });
    });
});
