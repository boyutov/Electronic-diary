document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#admins-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("admin-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const schoolName = window.location.pathname.split('/')[1];

    function loadAdmins() {
        fetch(`/api/${schoolName}/admins`)
            .then(response => response.json())
            .then(admins => {
                tableBody.innerHTML = "";
                admins.forEach(admin => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${admin.firstName} ${admin.secondName}</td>
                        <td>${admin.email}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${admin.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${admin.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteAdmin(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать администратора";
        document.getElementById("admin-id").value = id;
        
        fetch(`/api/${schoolName}/admins/${id}`)
            .then(response => response.json())
            .then(admin => {
                document.getElementById("firstName").value = admin.firstName;
                document.getElementById("secondName").value = admin.secondName;
                document.getElementById("thirdName").value = admin.thirdName || "";
                document.getElementById("email").value = admin.email;
                document.getElementById("password").value = ""; // Don't show password
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать администратора";
        form.reset();
        document.getElementById("admin-id").value = "";
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
        const id = document.getElementById("admin-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/admins/${id}` : `/api/${schoolName}/admins`;

        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value
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
                loadAdmins();
                alert(id ? "Администратор обновлен!" : "Администратор создан!");
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

    function deleteAdmin(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/admins/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadAdmins();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadAdmins();
});
