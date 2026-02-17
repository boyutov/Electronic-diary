document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#news-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("news-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementsByClassName("close")[0];
    const teacherSelect = document.getElementById("teacher");
    const schoolName = window.location.pathname.split('/')[1];

    // Load teachers
    fetch(`/api/${schoolName}/teachers`)
        .then(response => response.json())
        .then(teachers => {
            teacherSelect.innerHTML = "<option value=''>Без учителя</option>";
            teachers.forEach(teacher => {
                const option = document.createElement("option");
                option.value = teacher.id;
                option.textContent = `${teacher.firstName} ${teacher.secondName}`;
                teacherSelect.appendChild(option);
            });
        });

    function loadNews() {
        fetch(`/api/${schoolName}/news`)
            .then(response => response.json())
            .then(newsList => {
                tableBody.innerHTML = "";
                newsList.forEach(news => {
                    const row = document.createElement("tr");
                    const date = new Date(news.createdAt).toLocaleDateString();
                    row.innerHTML = `
                        <td>${news.title}</td>
                        <td>${date}</td>
                        <td>${news.teacher ? (news.teacher.user.firstName + ' ' + news.teacher.user.secondName) : '-'}</td>
                        <td>
                            <button class="action-btn edit-btn" data-id="${news.id}">Ред.</button>
                            <button class="action-btn delete-btn" data-id="${news.id}">Удал.</button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => openEditModal(e.target.dataset.id));
                });

                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => deleteNews(e.target.dataset.id));
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать новость";
        document.getElementById("news-id").value = id;
        
        fetch(`/api/${schoolName}/news/${id}`)
            .then(response => response.json())
            .then(news => {
                document.getElementById("title").value = news.title;
                document.getElementById("text").value = news.text;
                document.getElementById("teacher").value = news.teacher ? news.teacher.id : "";
            });
        
        modal.style.display = "block";
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Опубликовать новость";
        form.reset();
        document.getElementById("news-id").value = "";
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
        const id = document.getElementById("news-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/news/${id}` : `/api/${schoolName}/news`;

        const teacherId = document.getElementById("teacher").value;

        const data = {
            title: document.getElementById("title").value,
            text: document.getElementById("text").value,
            teacherId: teacherId ? parseInt(teacherId) : null
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
                loadNews();
                alert(id ? "Новость обновлена!" : "Новость опубликована!");
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

    function deleteNews(id) {
        if (confirm("Вы уверены?")) {
            fetch(`/api/${schoolName}/news/${id}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    loadNews();
                } else {
                    alert("Ошибка при удалении");
                }
            });
        }
    }

    loadNews();
});
