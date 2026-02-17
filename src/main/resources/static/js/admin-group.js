document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#groups-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("group-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const hasOfficeSelect = document.getElementById("hasOffice");
    const schoolName = window.location.pathname.split('/')[1];

    // We need a way to get a single group by ID for editing.
    // I'll assume I can find it in the list for now to save time, 
    // or I should add GET /groups/{id} to GroupController.
    // Let's add GET /groups/{id} to GroupController first.
    
    function loadGroups() {
        fetch(`/api/${schoolName}/groups`)
            .then(response => response.json())
            .then(groups => {
                tableBody.innerHTML = "";
                groups.forEach(group => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${group.name}</td>
                        <td>${group.course || "-"}</td>
                        <td>${group.office || "-"}</td> <!-- GroupResponse doesn't have office! Need to update DTO -->
                        <td>
                            <button class="action-btn edit-btn" data-id="${group.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${group.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteGroup(e.target.dataset.id));
                });
            });
    }

    // Need to update GroupResponse to include office and course.
    // And add GET /groups/{id}

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать группу";
        document.getElementById("group-id").value = id;
        
        // Fetch group details
        fetch(`/api/${schoolName}/groups/${id}`)
            .then(response => response.json())
            .then(group => {
                document.getElementById("name").value = group.name;
                document.getElementById("course").value = group.course;
                
                hasOfficeSelect.value = group.hasOffice ? "yes" : "no";
                document.getElementById("office").value = group.office || "";
                toggleConditional(hasOfficeSelect);
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать группу";
        form.reset();
        document.getElementById("group-id").value = "";
        modal.style.display = "block";
        toggleConditional(hasOfficeSelect);
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
        const id = document.getElementById("group-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/groups/${id}` : `/api/${schoolName}/groups`;

        const hasOffice = hasOfficeSelect.value === "yes";
        const office = document.getElementById("office").value;

        const data = {
            name: document.getElementById("name").value,
            hasOffice: hasOffice,
            office: hasOffice ? office : null,
            course: parseInt(document.getElementById("course").value)
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
                loadGroups();
                alert(id ? "Группа обновлена!" : "Группа создана!");
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

    function deleteGroup(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/groups/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadGroups();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

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

    loadGroups();
});
