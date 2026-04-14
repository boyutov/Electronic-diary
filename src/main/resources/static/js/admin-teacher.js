document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#teachers-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("teacher-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const groupSelect = document.getElementById("group");
    const hasOfficeSelect = document.getElementById("hasOffice");
    const hasGroupSelect = document.getElementById("hasGroup");
    const disciplinesContainer = document.getElementById("disciplines-container");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    // Load groups
    apiFetch(`/api/${schoolName}/groups`)
        .then(response => response.ok ? response.json() : [])
        .then(groups => {
            groupSelect.innerHTML = "<option selected disabled>Выберите группу</option>";
            groups.forEach(group => {
                const option = document.createElement("option");
                option.value = group.id;
                option.textContent = group.name;
                groupSelect.appendChild(option);
            });
        })
        .catch(() => {});

    // Load disciplines
    apiFetch(`/api/${schoolName}/disciplines`)
        .then(response => response.ok ? response.json() : [])
        .then(disciplines => {
            disciplinesContainer.innerHTML = "";
            if (!disciplines.length) {
                disciplinesContainer.innerHTML = "<p>Нет предметов. Создайте их сначала.</p>";
                return;
            }
            disciplines.forEach(discipline => {
                const div = document.createElement("div");
                div.className = "checkbox-item";
                div.innerHTML = `
                    <input type="checkbox" id="disc-${discipline.id}" value="${discipline.id}" name="disciplines">
                    <label for="disc-${discipline.id}">${discipline.name}</label>
                `;
                disciplinesContainer.appendChild(div);
            });
        })
        .catch(() => {});

    function loadTeachers() {
        apiFetch(`/api/${schoolName}/teachers`)
            .then(response => {
                if (!response.ok) {
                    tableBody.innerHTML = `<tr><td colspan='5' class='td-error'>Ошибка загрузки (${response.status})</td></tr>`;
                    return null;
                }
                return response.json();
            })
            .then(teachers => {
                if (!teachers) return;
                tableBody.innerHTML = "";
                if (!teachers.length) {
                    tableBody.innerHTML = "<tr><td colspan='5' class='td-empty'>Учителей нет</td></tr>";
                    return;
                }
                teachers.forEach(teacher => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${teacher.firstName} ${teacher.secondName}</td>
                        <td>${teacher.email}</td>
                        <td>${teacher.phone || "—"}</td>
                        <td>${teacher.groupName || "—"}</td>
                        <td>
                            <button class="button warning sm">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;">Удал.</button>
                        </td>
                    `;
                    row.querySelector(".warning").addEventListener("click", () => openEditModal(teacher.id));
                    row.querySelector(".danger").addEventListener("click", () => deleteTeacher(teacher.id));
                    tableBody.appendChild(row);
                });
            })
            .catch(err => {
                if (err !== "Unauthorized")
                    tableBody.innerHTML = `<tr><td colspan='5' class='td-error'>Ошибка: ${err}</td></tr>`;
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать учителя";
        document.getElementById("teacher-id").value = id;
        
        apiFetch(`/api/${schoolName}/teachers/${id}`)
            .then(response => response.json())
            .then(teacher => {
                document.getElementById("firstName").value = teacher.firstName;
                document.getElementById("secondName").value = teacher.secondName;
                document.getElementById("thirdName").value = teacher.thirdName || "";
                document.getElementById("email").value = teacher.email;
                document.getElementById("password").value = ""; // Don't show password
                document.getElementById("phone").value = teacher.phone || "";
                document.getElementById("bio").value = teacher.bio || "";
                
                hasOfficeSelect.value = teacher.hasOffice ? "yes" : "no";
                document.getElementById("teacher-office").style.display = teacher.hasOffice ? "block" : "none";
                document.getElementById("office").value = teacher.office || "";

                hasGroupSelect.value = teacher.hasGroup ? "yes" : "no";
                document.getElementById("teacher-group").style.display = teacher.hasGroup ? "block" : "none";
                if (teacher.hasGroup && teacher.groupId) {
                    groupSelect.value = teacher.groupId;
                }

                // Set disciplines
                document.querySelectorAll("input[name='disciplines']").forEach(cb => {
                    cb.checked = teacher.disciplineIds && teacher.disciplineIds.includes(parseInt(cb.value));
                });
            });
        
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать учителя";
        form.reset();
        document.getElementById("teacher-id").value = "";
        if (formError) formError.style.display = "none";
        document.getElementById("teacher-office").style.display = "none";
        document.getElementById("teacher-group").style.display = "none";
        document.querySelectorAll("input[name='disciplines']").forEach(cb => cb.checked = false);
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("teacher-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/teachers/${id}` : `/api/${schoolName}/teachers`;

        const hasGroup = hasGroupSelect.value === "yes";
        const groupId = groupSelect.value;
        const hasOffice = hasOfficeSelect.value === "yes";
        const office = document.getElementById("office").value;

        // Collect selected disciplines
        const selectedDisciplines = Array.from(document.querySelectorAll("input[name='disciplines']:checked"))
            .map(cb => parseInt(cb.value));

        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            phone: document.getElementById("phone").value,
            bio: document.getElementById("bio").value,
            hasOffice: hasOffice,
            office: hasOffice ? office : null,
            hasGroup: hasGroup,
            groupId: hasGroup ? parseInt(groupId) : null,
            disciplineIds: selectedDisciplines
        };

        apiFetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadTeachers();
            } else {
                r.json().then(err => {
                    if (formError) {
                        formError.textContent = typeof err === 'object' ? Object.values(err).join(", ") : String(err);
                        formError.style.display = "block";
                    }
                });
            }
        });
    });

    function deleteTeacher(id) {
        if (confirm("Вы уверены?")) {
            apiFetch(`/api/${schoolName}/teachers/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadTeachers();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    // Helper for conditional fields
    hasOfficeSelect.addEventListener("change", function() {
        document.getElementById("teacher-office").style.display = this.value === "yes" ? "block" : "none";
    });
    hasGroupSelect.addEventListener("change", function() {
        document.getElementById("teacher-group").style.display = this.value === "yes" ? "block" : "none";
    });

    loadTeachers();
});