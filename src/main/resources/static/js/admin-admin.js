document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#admins-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("admin-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const formError = document.getElementById("form-error");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    function loadAdmins() {
        apiFetch(`/api/${schoolName}/admins`)
            .then(r => {
                if (!r.ok) {
                    tableBody.innerHTML = `<tr><td colspan='4' class='td-error'>Ошибка загрузки (${esc(r.status)})</td></tr>`;
                    return null;
                }
                return r.json();
            })
            .then(admins => {
                if (!admins) return;
                tableBody.innerHTML = "";
                if (!admins.length) {
                    tableBody.innerHTML = "<tr><td colspan='4' class='td-empty'>Администраторов нет</td></tr>";
                    return;
                }
                admins.forEach((a, idx) => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td class="col-num">${idx + 1}</td>
                        <td>${esc(a.firstName)} ${esc(a.secondName)}${a.thirdName ? ' ' + esc(a.thirdName) : ''}</td>
                        <td>${esc(a.email)}</td>
                        <td>
                            <div class="action-btns">
                                <button class="btn-edit">Редактировать</button>
                                <button class="btn-delete">Удалить</button>
                            </div>
                        </td>
                    `;
                    tr.querySelector(".btn-edit").addEventListener("click", () => openEditModal(a.id));
                    tr.querySelector(".btn-delete").addEventListener("click", () => deleteAdmin(a.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(err => {
                if (err !== "Unauthorized")
                    tableBody.innerHTML = `<tr><td colspan='4' class='td-error'>Ошибка: ${err}</td></tr>`;
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать администратора";
        document.getElementById("admin-id").value = id;
        apiFetch(`/api/${schoolName}/admins/${id}`)
            .then(r => r.json())
            .then(a => {
                document.getElementById("firstName").value = a.firstName;
                document.getElementById("secondName").value = a.secondName;
                document.getElementById("thirdName").value = a.thirdName || "";
                document.getElementById("email").value = a.email;
                document.getElementById("password").value = "";
            });
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Добавить администратора";
        form.reset();
        document.getElementById("admin-id").value = "";
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
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

        apiFetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                modal.classList.remove("open");
                loadAdmins();
            } else {
                r.json().then(err => {
                    formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                    formError.style.display = "block";
                });
            }
        });
    });

    function deleteAdmin(id) {
        if (!confirm("Удалить администратора?")) return;
        apiFetch(`/api/${schoolName}/admins/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadAdmins(); else alert("Ошибка при удалении"); });
    }

    loadAdmins();
});
