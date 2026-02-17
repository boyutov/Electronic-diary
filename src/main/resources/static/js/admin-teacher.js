document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#teachers-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("teacher-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const groupSelect = document.getElementById("group");
    const hasOfficeSelect = document.getElementById("hasOffice");
    const hasGroupSelect = document.getElementById("hasGroup");
    const disciplinesContainer = document.getElementById("disciplines-container");
    const schoolName = window.location.pathname.split('/')[1];

    // Load groups
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

    // Load disciplines
    fetch(`/api/${schoolName}/disciplines`)
        .then(response => response.json())
        .then(disciplines => {
            disciplinesContainer.innerHTML = "";
            if (disciplines.length === 0) {
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
        });

    function loadTeachers() {
        fetch(`/api/${schoolName}/teachers`)
            .then(response => response.json())
            .then(teachers => {
                tableBody.innerHTML = "";
                teachers.forEach(teacher => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${teacher.firstName} ${teacher.secondName}</td>
                        <td>${teacher.email}</td>
                        <td>${teacher.phone || "-"}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${teacher.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${teacher.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteTeacher(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать учителя";
        document.getElementById("teacher-id").value = id;
        
        fetch(`/api/${schoolName}/teachers/${id}`)
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
                document.getElementById("office").value = teacher.office || "";
                toggleConditional(hasOfficeSelect);

                hasGroupSelect.value = teacher.hasGroup ? "yes" : "no";
                if (teacher.hasGroup && teacher.groupId) {
                    groupSelect.value = teacher.groupId;
                }
                toggleConditional(hasGroupSelect);

                // Set disciplines
                document.querySelectorAll("input[name='disciplines']").forEach(cb => {
                    cb.checked = teacher.disciplineIds && teacher.disciplineIds.includes(parseInt(cb.value));
                });
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать учителя";
        form.reset();
        document.getElementById("teacher-id").value = "";
        modal.style.display = "block";
        // Reset conditionals
        toggleConditional(hasOfficeSelect);
        toggleConditional(hasGroupSelect);
        // Reset checkboxes
        document.querySelectorAll("input[name='disciplines']").forEach(cb => cb.checked = false);
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
                loadTeachers();
                alert(id ? "Учитель обновлен!" : "Учитель создан!");
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

    function deleteTeacher(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/teachers/${id}`, {
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
    const toggleConditional = (select) => {
        const targetId = select.dataset.toggleTarget;
        if (!targetId) return;
        const target = document.getElementById(targetId);
        if (!target) return;
        if (select.value === "yes") {
            target.classList.add("is-visible");
        } else {
            target.classList.remove("is-visible");
        }
    };

    hasOfficeSelect.addEventListener("change", () => toggleConditional(hasOfficeSelect));
    hasGroupSelect.addEventListener("change", () => toggleConditional(hasGroupSelect));

    loadTeachers();
});
