document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.querySelector("#polls-table tbody");
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("poll-form");
    const addBtn = document.getElementById("add-btn");
    const closeBtn = document.getElementById("modal-close");
    const formError = document.getElementById("form-error");
    const statsModal = document.getElementById("stats-modal");
    const statsModalBody = document.getElementById("stats-modal-body");
    const statsModalTitle = document.getElementById("stats-modal-title");
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    const ROLES = [
        { value: "STUDENT",  label: "Ученики" },
        { value: "TEACHER",  label: "Учителя" },
        { value: "PARENT",   label: "Родители" },
        { value: "DIRECTOR", label: "Директора" },
    ];

    document.getElementById("stats-modal-close").onclick = () => statsModal.classList.remove("open");
    statsModal.addEventListener("click", e => { if (e.target === statsModal) statsModal.classList.remove("open"); });

    function getRolesValue() {
        return ROLES.filter(r => document.getElementById("role-" + r.value)?.checked)
            .map(r => r.value).join(",");
    }

    function setRolesValue(allowedRoles) {
        const active = allowedRoles ? allowedRoles.split(",").map(s => s.trim()) : ROLES.map(r => r.value);
        ROLES.forEach(r => {
            const cb = document.getElementById("role-" + r.value);
            if (cb) cb.checked = active.includes(r.value);
        });
    }

    function openStatsModal(poll) {
        statsModalTitle.textContent = `Результаты: ${poll.title}`;
        const total = poll.options.reduce((s, o) => s + o.voteCount, 0);
        statsModalBody.innerHTML = poll.options.length === 0
            ? "<p style='color:#64748b;'>Нет вариантов.</p>"
            : poll.options.map(opt => {
                const pct = total > 0 ? Math.round(opt.voteCount / total * 100) : 0;
                return `<div style="margin-bottom:14px;">
                    <div style="display:flex;justify-content:space-between;font-size:0.92em;margin-bottom:5px;">
                        <span style="font-weight:500;">${opt.optionText}</span>
                        <span style="color:#64748b;">${opt.voteCount} голос(ов) · ${pct}%</span>
                    </div>
                    <div style="background:#f1f5f9;border-radius:999px;height:10px;">
                        <div style="background:var(--primary);border-radius:999px;height:10px;width:${pct}%;"></div>
                    </div>
                </div>`;
            }).join("") + `<p style="color:#64748b;font-size:0.88em;margin-top:16px;border-top:1px solid var(--border);padding-top:12px;">Всего голосов: <strong>${total}</strong></p>`;
        statsModal.classList.add("open");
    }

    function loadPolls() {
        apiFetch(`/api/${schoolName}/polls`)
            .then(r => r.ok ? r.json() : [])
            .then(polls => {
                tableBody.innerHTML = "";
                if (!polls.length) {
                    tableBody.innerHTML = "<tr><td colspan='6' class='td-empty'>Голосований нет</td></tr>";
                    return;
                }
                polls.forEach(p => {
                    const date = p.createdAt ? new Date(p.createdAt).toLocaleDateString("ru-RU") : "—";
                    const total = (p.options || []).reduce((s, o) => s + o.voteCount, 0);
                    const roles = p.allowedRoles
                        ? p.allowedRoles.split(",").map(r => {
                            const f = ROLES.find(x => x.value === r.trim());
                            return f ? f.label : r.trim();
                          }).join(", ")
                        : "Все";
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${p.title}</td>
                        <td class="col-center"><span class="badge ${p.active ? "green" : "gray"}">${p.active ? "Активно" : "Закрыто"}</span></td>
                        <td class="col-center"><strong>${total}</strong></td>
                        <td style="color:#64748b;font-size:0.88em;">${roles}</td>
                        <td>${date}</td>
                        <td>
                            <button class="button ghost sm stats-btn">📊 Итоги</button>
                            <button class="button warning sm" style="margin-left:4px;">Ред.</button>
                            <button class="button danger sm" style="margin-left:4px;">Удал.</button>
                        </td>
                    `;
                    tr.querySelector(".stats-btn").addEventListener("click", () => openStatsModal(p));
                    tr.querySelector(".warning").addEventListener("click", () => openEditModal(p.id));
                    tr.querySelector(".danger").addEventListener("click", () => deletePoll(p.id));
                    tableBody.appendChild(tr);
                });
            })
            .catch(() => {
                tableBody.innerHTML = "<tr><td colspan='6' class='td-error'>Ошибка загрузки</td></tr>";
            });
    }

    function openEditModal(id) {
        modalTitle.textContent = "Редактировать голосование";
        document.getElementById("poll-id").value = id;
        apiFetch(`/api/${schoolName}/polls/${id}`)
            .then(r => r.json())
            .then(p => {
                document.getElementById("title").value = p.title;
                document.getElementById("description").value = p.description || "";
                document.getElementById("options").value = (p.options || []).map(o => o.optionText).join(", ");
                document.getElementById("active").value = p.active ? "yes" : "no";
                setRolesValue(p.allowedRoles);
            });
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        modalTitle.textContent = "Создать голосование";
        form.reset();
        document.getElementById("poll-id").value = "";
        setRolesValue(null);
        formError.style.display = "none";
        modal.classList.add("open");
    };

    closeBtn.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("poll-id").value;
        const data = {
            title: document.getElementById("title").value,
            description: document.getElementById("description").value,
            options: document.getElementById("options").value.split(",").map(s => s.trim()).filter(Boolean),
            active: document.getElementById("active").value === "yes",
            allowedRoles: getRolesValue()
        };
        apiFetch(id ? `/api/${schoolName}/polls/${id}` : `/api/${schoolName}/polls`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadPolls(); }
            else r.json().then(err => {
                formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                formError.style.display = "block";
            });
        });
    });

    function deletePoll(id) {
        if (!confirm("Удалить голосование?")) return;
        apiFetch(`/api/${schoolName}/polls/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadPolls(); else alert("Ошибка при удалении"); });
    }

    loadPolls();
});
