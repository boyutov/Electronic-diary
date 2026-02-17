document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#directors-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("director-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const schoolName = window.location.pathname.split('/')[1];

    function loadDirectors() {
        fetch(`/api/${schoolName}/directors`)
            .then(response => response.json())
            .then(directors => {
                tableBody.innerHTML = "";
                directors.forEach(director => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${director.firstName} ${director.secondName}</td>
                        <td>${director.email}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${director.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${director.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteDirector(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать директора";
        document.getElementById("director-id").value = id;
        
        fetch(`/api/${schoolName}/directors/${id}`)
            .then(response => response.json())
            .then(director => {
                document.getElementById("firstName").value = director.firstName;
                document.getElementById("secondName").value = director.secondName;
                document.getElementById("thirdName").value = director.thirdName || "";
                document.getElementById("email").value = director.email;
                document.getElementById("password").value = ""; // Don't show password
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать директора";
        form.reset();
        document.getElementById("director-id").value = "";
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
        const id = document.getElementById("director-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/directors/${id}` : `/api/${schoolName}/directors`;

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
                loadDirectors();
                alert(id ? "Директор обновлен!" : "Директор создан!");
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

    function deleteDirector(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/directors/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadDirectors();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadDirectors();
});
