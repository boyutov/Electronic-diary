document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#disciplines-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("discipline-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    function loadDisciplines() {
        apiFetch(`/api/${schoolName}/disciplines`)
            .then(r => {
                if (!r.ok) {
                    tableBody.innerHTML = `<tr><td colspan='2' class='td-error'>Ошибка загрузки (${r.status})</td></tr>`;
                    return null;
                }
                return r.json();
            })
            .then(disciplines => {
                if (!disciplines) return;
                tableBody.innerHTML = "";
                if (!disciplines.length) {
                    tableBody.innerHTML = "<tr><td colspan='2' class='td-empty'>Предметов нет</td></tr>";
                    return;
                }
                disciplines.forEach(d => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${d.name}</td>
                        <td>
                            <button class="button warning sm" data-id="${d.id}">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;" data-id="${d.id}">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(d.id, d.name));
                    tr.querySelector(".danger").addEventListener("click", () => deleteDiscipline(d.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(err => {
                if (err !== "Unauthorized")
                    tableBody.innerHTML = `<tr><td colspan='2' style='text-align:center;color:#ef4444;padding:24px;'>Ошибка: ${err}</td></tr>`;
            });
    }

    function openEditModal(id, name) {
        modalTitle.textContent = "Редактировать предмет";
        document.getElementById("discipline-id").value = id;
        document.getElementById("name").value = name;
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Добавить предмет";
        form.reset();
        document.getElementById("discipline-id").value = "";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("discipline-id").value;
        const method = id ? "PUT" : "POST";
        const url = id ? `/api/${schoolName}/disciplines/${id}` : `/api/${schoolName}/disciplines`;

        apiFetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: document.getElementById("name").value })
        }).then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadDisciplines();
            } else {
                r.json().then(err => {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                });
            }
        });
    });

    function deleteDiscipline(id) {
        if (!confirm("Удалить предмет?")) return;
        apiFetch(`/api/${schoolName}/disciplines/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadDisciplines(); else alert("Ошибка при удалении"); });
    }

    loadDisciplines();
});
