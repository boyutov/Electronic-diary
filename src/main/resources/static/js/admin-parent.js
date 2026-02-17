document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#parents-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("parent-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const schoolName = window.location.pathname.split('/')[1];

    function loadParents() {
        fetch(`/api/${schoolName}/parents`)
            .then(response => response.json())
            .then(parents => {
                tableBody.innerHTML = "";
                parents.forEach(parent => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${parent.user.firstName} ${parent.user.secondName}</td>
                        <td>${parent.user.email}</td>
                        <td>${parent.phone || "-"}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${parent.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${parent.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteParent(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать родителя";
        document.getElementById("parent-id").value = id;

        fetch(`/api/${schoolName}/parents/${id}`)
            .then(response => response.json())
            .then(parent => {
                document.getElementById("firstName").value = parent.user.firstName;
                document.getElementById("secondName").value = parent.user.secondName;
                document.getElementById("thirdName").value = parent.user.thirdName || "";
                document.getElementById("email").value = parent.user.email;
                document.getElementById("password").value = ""; // Don't show password
                document.getElementById("phone").value = parent.phone || "";
            });

        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать родителя";
        form.reset();
        document.getElementById("parent-id").value = "";
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
        const id = document.getElementById("parent-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/parents/${id}` : `/api/${schoolName}/parents`;

        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            phone: document.getElementById("phone").value
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
                loadParents();
                alert(id ? "Родитель обновлен!" : "Родитель создан!");
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

    function deleteParent(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/parents/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadParents();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadParents();
});
