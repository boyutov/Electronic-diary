document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#courses-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("course-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const teacherSelect = document.getElementById("teacher");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    apiFetch(`/api/${schoolName}/teachers`)
        .then(r => r.ok ? r.json() : [])
        .then(teachers => {
            teacherSelect.innerHTML = "<option selected disabled>Выберите учителя</option>";
            teachers.forEach(t => {
                const opt = document.createElement("option");
                opt.value = t.id;
                opt.textContent = `${t.firstName} ${t.secondName}`;
                teacherSelect.appendChild(opt);
            });
        });

    function loadCourses() {
        apiFetch(`/api/${schoolName}/courses`)
            .then(r => r.ok ? r.json() : [])
            .then(courses => {
                tableBody.innerHTML = "";
                if (!courses.length) {
                    tableBody.innerHTML = "<tr><td colspan='4' class='td-empty'>Курсов нет</td></tr>";
                    return;
                }
                courses.forEach(course => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${course.name}</td>
                        <td>${course.teacherName || "—"}</td>
                        <td>${course.description || "—"}</td>
                        <td>
                            <button class="button warning sm" data-id="${course.id}">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;" data-id="${course.id}">Удал.</button>
                        </td>
                    `;
                    row.querySelector(".warning").addEventListener("click", () => openEditModal(course.id));
                    row.querySelector(".danger").addEventListener("click", () => deleteCourse(course.id));
                    tableBody.appendChild(row);
                });
            })
            .catch(() => {
                tableBody.innerHTML = "<tr><td colspan='4' class='td-error'>Ошибка загрузки</td></tr>";
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать курс";
        document.getElementById("course-id").value = id;
        apiFetch(`/api/${schoolName}/courses/${id}`)
            .then(r => r.json())
            .then(course => {
                document.getElementById("name").value = course.name;
                document.getElementById("teacher").value = course.teacherId || "";
                document.getElementById("description").value = course.description || "";
            });
        if (formError) formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать курс";
        form.reset();
        document.getElementById("course-id").value = "";
        if (formError) formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        const id = document.getElementById("course-id").value;
        const data = {
            name: document.getElementById("name").value,
            teacherId: parseInt(document.getElementById("teacher").value),
            description: document.getElementById("description").value
        };
        apiFetch(id ? `/api/${schoolName}/courses/${id}` : `/api/${schoolName}/courses`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadCourses(); }
            else r.json().then(err => {
                if (formError) {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                }
            });
        });
    });

    function deleteCourse(id) {
        if (!confirm("Удалить курс?")) return;
        apiFetch(`/api/${schoolName}/courses/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadCourses(); else alert("Ошибка при удалении"); });
    }

    loadCourses();
});
