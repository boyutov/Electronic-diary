document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#students-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("student-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const groupSelect = document.getElementById("group");
    const formError = document.getElementById("form-error");
    const searchInput = document.getElementById("search-input");
    const filterGroup = document.getElementById("filter-group");
    const resetFilter = document.getElementById("reset-filter");
    const studentsCount = document.getElementById("students-count");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    let allStudents = [];

    // Загрузка групп в оба селекта
    apiFetch(`/api/${schoolName}/groups`)
        .then(r => r.json())
        .then(groups => {
            groupSelect.innerHTML = "<option disabled selected>Выберите группу</option>";
            groups.forEach(g => {
                const opt = document.createElement("option");
                opt.value = g.id;
                opt.textContent = g.name;
                groupSelect.appendChild(opt);

                const fOpt = document.createElement("option");
                fOpt.value = g.id;
                fOpt.textContent = g.name;
                filterGroup.appendChild(fOpt);
            });
        });

    function renderStudents() {
        const search = searchInput.value.toLowerCase().trim();
        const groupId = filterGroup.value;

        const filtered = allStudents.filter(s => {
            const fullName = `${s.firstName} ${s.secondName} ${s.thirdName || ""}`.toLowerCase();
            const matchSearch = !search || fullName.includes(search) || (s.email || "").toLowerCase().includes(search);
            const matchGroup = !groupId || String(s.groupId) === groupId;
            return matchSearch && matchGroup;
        });

        tableBody.innerHTML = "";
        studentsCount.textContent = `Найдено: ${filtered.length} из ${allStudents.length}`;

        if (filtered.length === 0) {
            tableBody.innerHTML = "<tr><td colspan='5' style='text-align:center;color:#64748b;padding:24px;'>Ничего не найдено</td></tr>";
            return;
        }

        filtered.forEach(s => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${esc(s.firstName)} ${esc(s.secondName)}${s.thirdName ? ' ' + esc(s.thirdName) : ''}</td>
                <td>${esc(s.email)}</td>
                <td>${s.age}</td>
                <td>${esc(s.groupName || "—")}</td>
                <td><div class="action-btns">
                    <button class="btn-edit" data-id="${s.id}">Редактировать</button>
                    <button class="btn-action reset-password" data-id="${s.id}" title="Отправить новый временный пароль на почту ученика">Пароль</button>
                    <button class="btn-delete" data-id="${s.id}">Удалить</button>
                </div></td>
            `;
            tr.querySelector(".btn-edit").addEventListener("click", () => openEditModal(s.id));
            tr.querySelector(".reset-password").addEventListener("click", () => resetPassword(s));
            tr.querySelector(".btn-delete").addEventListener("click", () => deleteStudent(s.id));
            tableBody.appendChild(tr);
        });
    }

    function loadStudents() {
        apiFetch(`/api/${schoolName}/students`)
            .then(r => r.json())
            .then(students => {
                allStudents = students;
                renderStudents();
            });
    }

    searchInput.addEventListener("input", renderStudents);
    filterGroup.addEventListener("change", renderStudents);
    resetFilter.addEventListener("click", () => {
        searchInput.value = "";
        filterGroup.value = "";
        renderStudents();
    });

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
            password: document.getElementById("password").value || null, // пусто → временный пароль на почту
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

    // Новый временный пароль уходит только на почту ученика — админ его не видит
    function resetPassword(s) {
        if (!confirm(`Отправить новый временный пароль ученику ${s.secondName} ${s.firstName}? Письмо уйдёт на его почту, а если её нет — родителю. Старый пароль перестанет работать.`)) return;
        apiFetch(`/api/${schoolName}/students/${s.id}/reset-password`, { method: "POST" })
            .then(async r => {
                if (r.ok) { alert("Пароль отправлен"); return; }
                const body = await r.json().catch(() => null);
                alert(body?.message || "Не удалось отправить пароль");
            });
    }

    function deleteStudent(id) {
        if (!confirm("Удалить ученика?")) return;
        apiFetch(`/api/${schoolName}/students/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadStudents(); else alert("Ошибка при удалении"); });
    }

    loadStudents();
});
