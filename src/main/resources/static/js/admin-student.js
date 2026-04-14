document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#students-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("student-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const groupSelect = document.getElementById("group");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    // Toggle office/group conditionals
    document.getElementById("hasOffice") && document.getElementById("hasOffice").addEventListener("change", function() {
        document.getElementById("teacher-office").style.display = this.value === "yes" ? "block" : "none";
    });

    apiFetch(`/api/${schoolName}/groups`)
        .then(r => r.json())
        .then(groups => {
            groupSelect.innerHTML = "<option disabled selected>Выберите группу</option>";
            groups.forEach(g => {
                const opt = document.createElement("option");
                opt.value = g.id;
                opt.textContent = g.name;
                groupSelect.appendChild(opt);
            });
        });

    function loadStudents() {
        apiFetch(`/api/${schoolName}/students`)
            .then(r => r.json())
            .then(students => {
                tableBody.innerHTML = "";
                if (students.length === 0) {
                    tableBody.innerHTML = "<tr><td colspan='5' style='text-align:center;color:#64748b;padding:24px;'>Учеников нет</td></tr>";
                    return;
                }
                students.forEach(s => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${s.firstName} ${s.secondName}${s.thirdName ? ' ' + s.thirdName : ''}</td>
                        <td>${s.email}</td>
                        <td>${s.age}</td>
                        <td>${s.groupName || "—"}</td>
                        <td>
                            <button class="button warning sm" data-id="${s.id}">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;" data-id="${s.id}">Удал.</button>
                        </td>
                    `;
                    tr.querySelectorAll(".warning")[0].addEventListener("click", () => openEditModal(s.id));
                    tr.querySelectorAll(".danger")[0].addEventListener("click", () => deleteStudent(s.id));
                    tableBody.appendChild(tr);
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать ученика";
        document.getElementById("student-id").value = id;
        apiFetch(`/api/${schoolName}/students/${id}`)
            .then(r => r.json())
            .then(s => {
                document.getElementById("firstName").value = s.firstName;
                document.getElementById("secondName").value = s.secondName;
                document.getElementById("thirdName").value = s.thirdName || "";
                document.getElementById("email").value = s.email;
                document.getElementById("password").value = "";
                document.getElementById("age").value = s.age;
                document.getElementById("group").value = s.groupId;
            });
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Добавить ученика";
        form.reset();
        document.getElementById("student-id").value = "";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("student-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/students/${id}` : `/api/${schoolName}/students`;

        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            age: parseInt(document.getElementById("age").value),
            groupId: parseInt(document.getElementById("group").value)
        };

        apiFetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadStudents();
            } else {
                r.json().then(err => {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                });
            }
        });
    });

    function deleteStudent(id) {
        if (!confirm("Удалить ученика?")) return;
        apiFetch(`/api/${schoolName}/students/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadStudents(); else alert("Ошибка при удалении"); });
    }

    loadStudents();
});
