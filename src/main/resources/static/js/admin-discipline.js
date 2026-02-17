document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#disciplines-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("discipline-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const schoolName = window.location.pathname.split('/')[1];

    function loadDisciplines() {
        fetch(`/api/${schoolName}/disciplines`)
            .then(response => response.json())
            .then(disciplines => {
                tableBody.innerHTML = "";
                disciplines.forEach(discipline => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${discipline.name}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${discipline.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${discipline.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteDiscipline(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать предмет";
        document.getElementById("discipline-id").value = id;
        
        fetch(`/api/${schoolName}/disciplines/${id}`)
            .then(response => response.json())
            .then(discipline => {
                document.getElementById("name").value = discipline.name;
            });

        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать предмет";
        form.reset();
        document.getElementById("discipline-id").value = "";
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
        const id = document.getElementById("discipline-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/disciplines/${id}` : `/api/${schoolName}/disciplines`;

        const data = {
            name: document.getElementById("name").value
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
                loadDisciplines();
                alert(id ? "Предмет обновлен!" : "Предмет создан!");
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

    function deleteDiscipline(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/disciplines/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadDisciplines();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadDisciplines();
});
