document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#news-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("news-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const teacherSelect = document.getElementById("teacher");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    apiFetch(`/api/${schoolName}/teachers`)
        .then(r => r.json())
        .then(teachers => {
            teacherSelect.innerHTML = "<option value=''>Без учителя</option>";
            teachers.forEach(t => {
                const opt = document.createElement("option");
                opt.value = t.id;
                opt.textContent = `${t.firstName} ${t.secondName}`;
                teacherSelect.appendChild(opt);
            });
        });

    function loadNews() {
        apiFetch(`/api/${schoolName}/news`)
            .then(r => r.json())
            .then(newsList => {
                tableBody.innerHTML = "";
                if (!newsList.length) {
                    tableBody.innerHTML = "<tr><td colspan='3' class='td-empty'>Новостей нет</td></tr>";
                    return;
                }
                newsList.forEach(n => {
                    const date = n.createdAt ? new Date(n.createdAt).toLocaleDateString('ru-RU') : "—";
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${n.title}</td>
                        <td>${date}</td>
                        <td>
                            <button class="button warning sm" data-id="${n.id}">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;" data-id="${n.id}">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(n.id));
                    tr.querySelector(".danger").addEventListener("click", () => deleteNews(n.id));
                    tableBody.appendChild(tr);
                });
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать новость";
        document.getElementById("news-id").value = id;
        apiFetch(`/api/${schoolName}/news/${id}`)
            .then(r => r.json())
            .then(n => {
                document.getElementById("title").value = n.title;
                document.getElementById("text").value = n.text;
                teacherSelect.value = n.teacherId || "";
            });
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Опубликовать новость";
        form.reset();
        document.getElementById("news-id").value = "";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("news-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/news/${id}` : `/api/${schoolName}/news`;
        const teacherId = teacherSelect.value;

        const data = {
            title: document.getElementById("title").value,
            text: document.getElementById("text").value,
            teacherId: teacherId ? parseInt(teacherId) : null
        };

        apiFetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadNews();
            } else {
                r.json().then(err => {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                });
            }
        });
    });

    function deleteNews(id) {
        if (!confirm("Удалить новость?")) return;
        apiFetch(`/api/${schoolName}/news/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadNews(); else alert("Ошибка при удалении"); });
    }

    loadNews();
});
