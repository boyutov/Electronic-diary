document.addEventListener("DOMContentLoaded", () => {
    const disciplineSelect = document.querySelector("select[name='discipline']");
    const teacherSelect = document.querySelector("select[name='teacher']");
    const groupSelect = document.querySelector("select[name='group']");
    const form = document.querySelector("form");
    const submitButton = form.querySelector("button");
    const schoolName = window.location.pathname.split('/')[1];

    fetch(`/api/${schoolName}/disciplines`)
        .then(response => response.json())
        .then(disciplines => {
            disciplineSelect.innerHTML = "<option selected disabled>Выберите предмет</option>";
            disciplines.forEach(discipline => {
                const option = document.createElement("option");
                option.value = discipline.id;
                option.textContent = discipline.name;
                disciplineSelect.appendChild(option);
            });
        });

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
        const disciplineId = disciplineSelect.value;
        const teacherId = teacherSelect.value;
        const groupId = groupSelect.value;
        const dayOfWeek = form.querySelector("input[placeholder='1']").value;
        const lessonNumber = form.querySelector("input[placeholder='1']").value;
        const classroom = form.querySelector("input[placeholder='204']").value;

        const data = {
            disciplineId: parseInt(disciplineId),
            teacherId: parseInt(teacherId),
            groupId: parseInt(groupId),
            dayOfWeek: parseInt(dayOfWeek),
            lessonNumber: parseInt(lessonNumber),
            classroom
        };

        fetch(`/api/${schoolName}/schedules`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                alert("Урок успешно добавлен в расписание!");
                form.reset();
            } else {
                alert("Ошибка при добавлении урока");
            }
        });
    });
});
