document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    // --- Группы ---
    const tableBody = document.querySelector("#groups-table tbody");
    const modal = document.getElementById("modal");
    const form = document.getElementById("group-form");
    const addBtn = document.getElementById("add-btn");
    const hasOfficeSelect = document.getElementById("hasOffice");
    const formError = document.getElementById("form-error");

    hasOfficeSelect.addEventListener("change", function () {
        document.getElementById("group-office").style.display = this.value === "yes" ? "block" : "none";
    });

    function loadGroups() {
        apiFetch(`/api/${schoolName}/groups`)
            .then(r => r.ok ? r.json() : [])
            .then(groups => {
                tableBody.innerHTML = "";
                if (!groups.length) {
                    tableBody.innerHTML = "<tr><td colspan='6' class='td-empty'>Классов нет</td></tr>";
                    return;
                }
                groups.forEach(g => {
                    const tr = document.createElement("tr");
                    const funding = g.fundingType === "CONTRACT"
                        ? `<span class="badge yellow">Контракт</span>`
                        : `<span class="badge green">Бюджет</span>`;
                    tr.innerHTML = `
                        <td>${g.name}</td>
                        <td class="col-center">${g.course || "—"}</td>
                        <td>${g.office || "—"}</td>
                        <td class="col-center">${funding}</td>
                        <td>${g.curatorName || "—"}</td>
                        <td>
                            <button class="button sm" style="background:#0ea5e9;">🎓 Ученики</button>
                            <button class="button warning sm" style="margin-left:4px;">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".button.sm").addEventListener("click", () => openStudentsModal(g.id, g.name));
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(g.id));
                    tr.querySelector(".danger").addEventListener("click", () => deleteGroup(g.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(() => {
                tableBody.innerHTML = "<tr><td colspan='6' class='td-error'>Ошибка загрузки</td></tr>";
            });
    }

    function openEditModal(id) {
        document.getElementById("modal-title").textContent = "Редактировать класс";
        document.getElementById("group-id").value = id;
        apiFetch(`/api/${schoolName}/groups/${id}`)
            .then(r => r.json())
            .then(g => {
                document.getElementById("name").value = g.name;
                document.getElementById("course").value = g.course || "";
                hasOfficeSelect.value = g.hasOffice ? "yes" : "no";
                document.getElementById("group-office").style.display = g.hasOffice ? "block" : "none";
                document.getElementById("office").value = g.office || "";
                document.getElementById("fundingType").value = g.fundingType || "BUDGET";
            });
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        document.getElementById("modal-title").textContent = "Добавить класс";
        form.reset();
        document.getElementById("group-id").value = "";
        document.getElementById("group-office").style.display = "none";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    document.getElementById("modal-close").onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("group-id").value;
        const hasOffice = hasOfficeSelect.value === "yes";
        apiFetch(id ? `/api/${schoolName}/groups/${id}` : `/api/${schoolName}/groups`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: document.getElementById("name").value,
                hasOffice,
                office: hasOffice ? document.getElementById("office").value : null,
                course: parseInt(document.getElementById("course").value) || null,
                fundingType: document.getElementById("fundingType").value
            })
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadGroups(); }
            else r.json().then(err => {
                formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                formError.style.display = "block";
            });
        });
    });

    function deleteGroup(id) {
        if (!confirm("Удалить класс?")) return;
        apiFetch(`/api/${schoolName}/groups/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadGroups(); else alert("Ошибка при удалении"); });
    }

    // --- Ученики группы ---
    const studentsModal = document.getElementById("students-modal");
    const studentsTableBody = document.querySelector("#students-table tbody");
    const studentFormModal = document.getElementById("student-form-modal");
    const studentForm = document.getElementById("student-form");
    const studentFormError = document.getElementById("student-form-error");
    let currentGroupId = null;

    function openStudentsModal(groupId, groupName) {
        currentGroupId = groupId;
        document.getElementById("students-modal-title").textContent = `Ученики — ${groupName}`;
        loadStudents(groupId);
        studentsModal.classList.add("open");
    }

    document.getElementById("students-modal-close").onclick = () => studentsModal.classList.remove("open");
    studentsModal.addEventListener("click", e => { if (e.target === studentsModal) studentsModal.classList.remove("open"); });

    document.getElementById("add-student-btn").onclick = () => {
        document.getElementById("student-form-title").textContent = "Добавить ученика";
        studentForm.reset();
        document.getElementById("student-id").value = "";
        studentFormError.style.display = "none";
        studentFormModal.classList.add("open");
    };

    document.getElementById("student-form-close").onclick = () => studentFormModal.classList.remove("open");
    studentFormModal.addEventListener("click", e => { if (e.target === studentFormModal) studentFormModal.classList.remove("open"); });

    function loadStudents(groupId) {
        studentsTableBody.innerHTML = "<tr><td colspan='4' class='td-empty'>Загрузка...</td></tr>";
        apiFetch(`/api/${schoolName}/groups/${groupId}/students`)
            .then(r => r.ok ? r.json() : [])
            .then(students => {
                studentsTableBody.innerHTML = "";
                if (!students.length) {
                    studentsTableBody.innerHTML = "<tr><td colspan='4' class='td-empty'>Учеников нет</td></tr>";
                    return;
                }
                students.forEach(s => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${s.firstName} ${s.secondName}${s.thirdName ? ' ' + s.thirdName : ''}</td>
                        <td>${s.email}</td>
                        <td class="col-center">${s.age}</td>
                        <td>
                            <button class="button warning sm">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".warning").addEventListener("click", () => openStudentEditModal(s.id));
                    tr.querySelector(".danger").addEventListener("click", () => deleteStudent(s.id));
                    studentsTableBody.appendChild(tr);
                });
            })
            .catch(() => {
                studentsTableBody.innerHTML = "<tr><td colspan='4' class='td-error'>Ошибка загрузки</td></tr>";
            });
    }

    function openStudentEditModal(id) {
        document.getElementById("student-form-title").textContent = "Редактировать ученика";
        document.getElementById("student-id").value = id;
        apiFetch(`/api/${schoolName}/students/${id}`)
            .then(r => r.json())
            .then(s => {
                document.getElementById("s-firstName").value = s.firstName;
                document.getElementById("s-secondName").value = s.secondName;
                document.getElementById("s-thirdName").value = s.thirdName || "";
                document.getElementById("s-email").value = s.email;
                document.getElementById("s-password").value = "";
                document.getElementById("s-age").value = s.age;
            });
        studentFormError.style.display = "none";
        studentFormModal.classList.add("open");
    }

    studentForm.addEventListener("submit", e => {
        e.preventDefault();
        studentFormError.style.display = "none";
        const id = document.getElementById("student-id").value;
        const data = {
            firstName: document.getElementById("s-firstName").value,
            secondName: document.getElementById("s-secondName").value,
            thirdName: document.getElementById("s-thirdName").value,
            email: document.getElementById("s-email").value,
            password: document.getElementById("s-password").value,
            age: parseInt(document.getElementById("s-age").value),
            groupId: currentGroupId
        };
        apiFetch(id ? `/api/${schoolName}/students/${id}` : `/api/${schoolName}/students`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                studentFormModal.classList.remove("open");
                loadStudents(currentGroupId);
            } else {
                r.json().then(err => {
                    studentFormError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    studentFormError.style.display = "block";
                });
            }
        });
    });

    function deleteStudent(id) {
        if (!confirm("Удалить ученика?")) return;
        apiFetch(`/api/${schoolName}/students/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadStudents(currentGroupId); else alert("Ошибка при удалении"); });
    }

    loadGroups();
});
