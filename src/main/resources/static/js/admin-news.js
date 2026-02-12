document.addEventListener("DOMContentLoaded", () => {
    const teacherSelect = document.querySelector("select[name='teacher']");
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    fetch(`/api/${schoolName}/teachers`)
        .then(response => response.json())
        .then(teachers => {
            teacherSelect.innerHTML = "<option value=''>Без учителя</option>";
            teachers.forEach(teacher => {
                const option = document.createElement("option");
                option.value = teacher.id;
                option.textContent = `${teacher.firstName} ${teacher.secondName}`;
                teacherSelect.appendChild(option);
            });
        });

    submitButton.addEventListener("click", () => {
        const title = form.querySelector("input[placeholder='Новая неделя']").value;
        const text = form.querySelector("input[placeholder='Описание новости']").value;
        const teacherId = teacherSelect.value;

        const data = {
            title,
            text,
            teacherId: teacherId ? parseInt(teacherId) : null
        };

        fetch(`/api/${schoolName}/news`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Новость успешно опубликована!");
                form.reset();
            } else {
                alert("Ошибка при публикации новости");
            }
        });
    });
});
