document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#directors-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("director-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    function loadDirectors() {
        apiFetch(`/api/${schoolName}/directors`)
            .then(r => {
                if (!r.ok) {
                    tableBody.innerHTML = `<tr><td colspan='3' class='td-error'>Ошибка загрузки (${r.status})</td></tr>`;
                    return null;
                }
                return r.json();
            })
            .then(directors => {
                if (!directors) return;
                tableBody.innerHTML = "";
                if (!directors.length) {
                    tableBody.innerHTML = "<tr><td colspan='3' class='td-empty'>Директоров нет</td></tr>";
                    return;
                }
                directors.forEach(d => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${d.firstName} ${d.secondName}${d.thirdName ? ' ' + d.thirdName : ''}</td>
                        <td>${d.email}</td>
                        <td>
                            <button class="button warning sm" data-id="${d.id}">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;" data-id="${d.id}">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(d.id));
                    tr.querySelector(".danger").addEventListener("click", () => deleteDirector(d.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(err => {
                if (err !== "Unauthorized")
                    tableBody.innerHTML = `<tr><td colspan='3' class='td-error'>Ошибка: ${err}</td></tr>`;
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать директора";
        document.getElementById("director-id").value = id;
        apiFetch(`/api/${schoolName}/directors/${id}`)
            .then(r => r.json())
            .then(d => {
                document.getElementById("firstName").value = d.firstName;
                document.getElementById("secondName").value = d.secondName;
                document.getElementById("thirdName").value = d.thirdName || "";
                document.getElementById("email").value = d.email;
                document.getElementById("password").value = "";
            });
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Добавить директора";
        form.reset();
        document.getElementById("director-id").value = "";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
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

        apiFetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadDirectors();
            } else {
                r.json().then(err => {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                });
            }
        });
    });

    function deleteDirector(id) {
        if (!confirm("Удалить директора?")) return;
        apiFetch(`/api/${schoolName}/directors/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadDirectors(); else alert("Ошибка при удалении"); });
    }

    loadDirectors();
});
