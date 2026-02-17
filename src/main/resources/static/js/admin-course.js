document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#courses-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("course-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const teacherSelect = document.getElementById("teacher");
    const schoolName = window.location.pathname.split('/')[1];

    // Load teachers
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

    function loadCourses() {
        fetch(`/api/${schoolName}/courses`)
            .then(response => response.json())
            .then(courses => {
                tableBody.innerHTML = "";
                courses.forEach(course => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${course.name}</td>
                        <td>${course.teacher ? (course.teacher.user.firstName + ' ' + course.teacher.user.secondName) : '-'}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${course.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${course.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteCourse(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать курс";
        document.getElementById("course-id").value = id;
        
        fetch(`/api/${schoolName}/courses/${id}`)
            .then(response => response.json())
            .then(course => {
                document.getElementById("name").value = course.name;
                document.getElementById("description").value = course.description || "";
                document.getElementById("teacher").value = course.teacher.id;
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать курс";
        form.reset();
        document.getElementById("course-id").value = "";
        modal.style.display = "block";
    };

    closeBtn.onclick = () => {
        modal.style.display = "none";
    };

    window.onclick = (event) => {
        if (event.target == modal) {
            modal.style.display = "none";
        }
    };

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("course-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/courses/${id}` : `/api/${schoolName}/courses`;

        const data = {
            name: document.getElementById("name").value,
            teacherId: parseInt(document.getElementById("teacher").value),
            description: document.getElementById("description").value
        };

        fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                modal.style.display = "none";
                loadCourses();
                alert(id ? "Курс обновлен!" : "Курс создан!");
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
                    alert("Ошибка:\n" + errorMessages);
                });
            }
        });
    });

    function deleteCourse(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/courses/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadCourses();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadCourses();
});
