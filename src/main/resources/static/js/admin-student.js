document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#students-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("student-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const groupSelect = document.getElementById("group");
    const schoolName = window.location.pathname.split('/')[1];

    // Load groups for select
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

    function loadStudents() {
        fetch(`/api/${schoolName}/students`)
            .then(response => response.json())
            .then(students => {
                tableBody.innerHTML = "";
                students.forEach(student => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>${student.firstName} ${student.secondName}</td>
                        <td>${student.email}</td>
                        <td>${student.age}</td>
                        <td>${student.groupName}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${student.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${student.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteStudent(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать ученика";
        document.getElementById("student-id").value = id;
        
        fetch(`/api/${schoolName}/students/${id}`)
            .then(response => response.json())
            .then(student => {
                document.getElementById("firstName").value = student.firstName;
                document.getElementById("secondName").value = student.secondName;
                document.getElementById("thirdName").value = student.thirdName || "";
                document.getElementById("email").value = student.email;
                document.getElementById("password").value = ""; // Don't show password
                document.getElementById("age").value = student.age;
                document.getElementById("group").value = student.groupId;
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать ученика";
        form.reset();
        document.getElementById("student-id").value = "";
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
                loadStudents();
                alert(id ? "Ученик обновлен!" : "Ученик создан!");
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

    function deleteStudent(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/students/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadStudents();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadStudents();
});
