document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#parents-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("parent-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const formError = document.getElementById("form-error");
    const studentsContainer = document.getElementById("students-container");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    let allStudents = [];

    // Загружаем всех учеников для выбора
    apiFetch(`/api/${schoolName}/students`)
        .then(r => r.ok ? r.json() : [])
        .then(students => {
            allStudents = students;
            renderStudentCheckboxes([]);
        }).catch(() => {});

    function renderStudentCheckboxes(selectedIds) {
        studentsContainer.innerHTML = "";
        if (!allStudents.length) {
            studentsContainer.innerHTML = "<p style='color:#64748b;font-size:0.9em;'>Учеников нет. Сначала создайте учеников.</p>";
            return;
        }
        allStudents.forEach(s => {
            const div = document.createElement("div");
            div.className = "checkbox-item";
            div.innerHTML = `
                <input type="checkbox" id="st-${s.id}" value="${s.id}" name="students"
                    ${selectedIds.includes(s.id) ? "checked" : ""}>
                <label for="st-${s.id}">${s.secondName} ${s.firstName}${s.thirdName ? " " + s.thirdName : ""} — ${s.groupName || "без класса"}</label>
            `;
            studentsContainer.appendChild(div);
        });
    }

    function getSelectedStudentIds() {
        return Array.from(document.querySelectorAll("input[name='students']:checked"))
            .map(cb => parseInt(cb.value));
    }

    function loadParents() {
        apiFetch(`/api/${schoolName}/parents`)
            .then(r => r.ok ? r.json() : [])
            .then(parents => {
                tableBody.innerHTML = "";
                if (!parents.length) {
                    tableBody.innerHTML = "<tr><td colspan='5' class='td-empty'>Родителей нет</td></tr>";
                    return;
                }
                parents.forEach(p => {
                    const childrenText = p.children && p.children.length
                        ? p.children.map(c => `${c.firstName} ${c.secondName}`).join(", ")
                        : "—";
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${p.firstName} ${p.secondName}${p.thirdName ? " " + p.thirdName : ""}</td>
                        <td>${p.email}</td>
                        <td>${p.phone || "—"}</td>
                        <td style="color:#64748b;font-size:0.88em;">${childrenText}</td>
                        <td>
                            <button class="button warning sm">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(p.id, p));
                    tr.querySelector(".danger").addEventListener("click", () => deleteParent(p.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(() => {
                tableBody.innerHTML = "<tr><td colspan='5' class='td-error'>Ошибка загрузки</td></tr>";
            });
    }

    function openEditModal(id, parent) {
        modalTitle.textContent = "Редактировать родителя";
        document.getElementById("parent-id").value = id;
        document.getElementById("firstName").value = parent.firstName || "";
        document.getElementById("secondName").value = parent.secondName || "";
        document.getElementById("thirdName").value = parent.thirdName || "";
        document.getElementById("email").value = parent.email || "";
        document.getElementById("password").value = "";
        document.getElementById("phone").value = parent.phone || "";
        const selectedIds = (parent.children || []).map(c => Number(c.id));
        renderStudentCheckboxes(selectedIds);
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Добавить родителя";
        form.reset();
        document.getElementById("parent-id").value = "";
        renderStudentCheckboxes([]);
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("parent-id").value;
        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            phone: document.getElementById("phone").value,
            studentIds: getSelectedStudentIds()
        };
        apiFetch(id ? `/api/${schoolName}/parents/${id}` : `/api/${schoolName}/parents`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadParents(); }
            else r.json().then(err => {
                formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                formError.style.display = "block";
            });
        });
    });

    function deleteParent(id) {
        if (!confirm("Удалить родителя?")) return;
        apiFetch(`/api/${schoolName}/parents/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadParents(); else alert("Ошибка при удалении"); });
    }

    loadParents();
});
